package internal

import (
	"context"
	"crypto/tls"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/go-newsgroups/nntp"
	"github.com/go-newsgroups/nzb"
)

type nntpConfig struct {
	Host     string
	Port     int
	User     string
	Pass     string
	UseTLS   bool
	Insecure bool
}

func nntpConfigFromEnv() nntpConfig {
	cfg := nntpConfig{
		Host:   strings.TrimSpace(os.Getenv("NNTP_HOST")),
		User:   os.Getenv("NNTP_USER"),
		Pass:   os.Getenv("NNTP_PASS"),
		UseTLS: strings.EqualFold(os.Getenv("NNTP_SSL"), "true") || os.Getenv("NNTP_SSL") == "1",
	}
	if p := strings.TrimSpace(os.Getenv("NNTP_PORT")); p != "" {
		if n, err := strconv.Atoi(p); err == nil && n > 0 {
			cfg.Port = n
		}
	}
	if cfg.Port == 0 {
		if cfg.UseTLS {
			cfg.Port = 563
		} else {
			cfg.Port = 119
		}
	}
	cfg.Insecure = strings.EqualFold(os.Getenv("NNTP_TLS_INSECURE"), "true")
	return cfg
}

func (c nntpConfig) addr() string {
	return fmt.Sprintf("%s:%d", c.Host, c.Port)
}

func (c nntpConfig) configured() bool {
	return c.Host != ""
}

type nntpEngine struct {
	cfg nntpConfig
}

func newNNTPEngine(cfg nntpConfig) *nntpEngine {
	return &nntpEngine{cfg: cfg}
}

func (e *nntpEngine) dial(ctx context.Context) (*nntp.Conn, error) {
	if !e.cfg.configured() {
		return nil, fmt.Errorf("nntp unconfigured: set NNTP_HOST (+ NNTP_USER/NNTP_PASS for auth)")
	}
	var (
		conn *nntp.Conn
		err  error
	)
	if e.cfg.UseTLS {
		tlsCfg := &tls.Config{MinVersion: tls.VersionTLS12}
		if e.cfg.Insecure {
			tlsCfg.InsecureSkipVerify = true //nolint:gosec // operator opt-in for misconfigured providers
		}
		conn, err = nntp.DialTLS(ctx, e.cfg.addr(), tlsCfg)
	} else {
		conn, err = nntp.Dial(ctx, e.cfg.addr())
	}
	if err != nil {
		return nil, fmt.Errorf("nntp dial %s: %w", e.cfg.addr(), err)
	}
	if e.cfg.User != "" {
		if err := conn.Authenticate(e.cfg.User, e.cfg.Pass); err != nil {
			conn.Close()
			return nil, fmt.Errorf("nntp auth: %w", err)
		}
	}
	return conn, nil
}

func (e *nntpEngine) RunJob(ctx context.Context, j *job, nzbData []byte, destDir string) error {
	doc, err := nzb.Parse(nzbData)
	if err != nil {
		j.markFailed(err.Error())
		return fmt.Errorf("parse nzb: %w", err)
	}
	if len(doc.Files) == 0 {
		j.markFailed("nzb has no files")
		return fmt.Errorf("nzb has no files")
	}

	var total int64
	for _, f := range doc.Files {
		for _, seg := range f.Segments {
			total += int64(seg.Bytes)
		}
	}
	j.setProgress(total, 0, 0)
	j.setStatus(jobStatusDownloading)

	conn, err := e.dial(ctx)
	if err != nil {
		j.markFailed(err.Error())
		return err
	}
	defer conn.Close()

	_, name, category, _, _, _, _ := j.queueView()
	outRoot := jobDir(destDir, category, name)
	if err := os.MkdirAll(outRoot, 0o755); err != nil {
		j.markFailed(err.Error())
		return err
	}

	var done int64
	for i, f := range doc.Files {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}
		data, fileName, err := nzb.DownloadFile(ctx, conn, f)
		if err != nil {
			j.markFailed(err.Error())
			return fmt.Errorf("download file %d: %w", i, err)
		}
		safeName := sanitizeName(fileName)
		if safeName == "" {
			safeName = fmt.Sprintf("part-%04d.bin", i+1)
		}
		abs := filepath.Join(outRoot, safeName)
		if err := os.WriteFile(abs, data, 0o644); err != nil {
			j.markFailed(err.Error())
			return err
		}
		done += int64(len(data))
		progress := float64(0)
		if total > 0 {
			progress = float64(done) / float64(total) * 100
		}
		j.setProgress(total, done, progress)
	}

	if err := ctx.Err(); err != nil {
		return err
	}
	_, err = finalizeDownload(ctx, j, outRoot)
	return err
}
