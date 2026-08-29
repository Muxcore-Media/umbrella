package internal

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/Muxcore-Media/core/pkg/contracts"
)

// jobStatus values mirror SABnzbd-ish queue states for automation compatibility.
const (
	jobStatusQueued      = "Queued"
	jobStatusDownloading = "Downloading"
	jobStatusPaused      = "Paused"
	jobStatusVerifying   = "Verifying"
	jobStatusRepairing   = "Repairing"
	jobStatusExtracting  = "Extracting"
	jobStatusCompleted   = "Completed"
	jobStatusFailed      = "Failed"
)

type job struct {
	mu sync.Mutex

	id       string
	name     string
	category string
	nzbURL   string
	status   string
	progress float64
	total    int64
	done     int64
	storage  string
	errMsg   string
	paused   bool
	deleted  bool

	cancel context.CancelFunc
}

func (j *job) snapshot() (status, name, storage, errMsg string) {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.status, j.name, j.storage, j.errMsg
}

func (j *job) setStatus(status string) {
	j.mu.Lock()
	j.status = status
	j.mu.Unlock()
}

func (j *job) setProgress(total, done int64, progress float64) {
	j.mu.Lock()
	j.total = total
	j.done = done
	j.progress = progress
	j.mu.Unlock()
}

func (j *job) markCompleted(storage string) {
	j.mu.Lock()
	j.storage = storage
	j.done = j.total
	j.progress = 100
	j.status = jobStatusCompleted
	j.mu.Unlock()
}

func (j *job) markFailed(msg string) {
	j.mu.Lock()
	j.status = jobStatusFailed
	j.errMsg = msg
	j.mu.Unlock()
}

func (j *job) queueView() (id, name, category, status string, progress float64, total, remaining int64) {
	j.mu.Lock()
	defer j.mu.Unlock()
	rem := j.total - j.done
	if rem < 0 {
		rem = 0
	}
	return j.id, j.name, j.category, j.status, j.progress, j.total, rem
}

func (j *job) historyView() (id, name, category, status, storage string) {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.id, j.name, j.category, j.status, j.storage
}

func (j *job) isPaused() bool {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.paused
}

func (j *job) setPaused(paused bool) {
	j.mu.Lock()
	j.paused = paused
	j.mu.Unlock()
}

func (j *job) markDeleted() {
	j.mu.Lock()
	j.deleted = true
	j.mu.Unlock()
}

func (j *job) isDeleted() bool {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.deleted
}

func (j *job) storagePath() string {
	j.mu.Lock()
	defer j.mu.Unlock()
	return j.storage
}

func fetchNZB(ctx context.Context, client *http.Client, nzbURL string) ([]byte, error) {
	if strings.TrimSpace(nzbURL) == "" {
		return nil, fmt.Errorf("nzb url required")
	}
	if client == nil {
		client = &http.Client{Timeout: 2 * time.Minute}
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, nzbURL, nil)
	if err != nil {
		return nil, err
	}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		body, _ := io.ReadAll(io.LimitReader(resp.Body, 4096))
		return nil, fmt.Errorf("fetch nzb: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(body)))
	}
	return io.ReadAll(io.LimitReader(resp.Body, 64<<20))
}

func sanitizeName(name string) string {
	name = strings.TrimSpace(name)
	if name == "" {
		return "release"
	}
	name = filepath.Base(name)
	name = strings.TrimSuffix(name, ".nzb")
	replacer := strings.NewReplacer("/", "_", "\\", "_", ":", "_")
	return replacer.Replace(name)
}

func jobDir(base, category, name string) string {
	parts := []string{base}
	if category != "" {
		parts = append(parts, category)
	}
	parts = append(parts, sanitizeName(name))
	return filepath.Join(parts...)
}

func writeFixtureMedia(dir, name string, size int64) (string, error) {
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", err
	}
	base := sanitizeName(name)
	rel := base + ".mkv"
	abs := filepath.Join(dir, rel)
	payload := make([]byte, size)
	for i := range payload {
		payload[i] = byte(i % 251)
	}
	if err := os.WriteFile(abs, payload, 0o644); err != nil {
		return "", err
	}
	return abs, nil
}

func filesFromStorage(storage string) []contracts.DownloadEventFile {
	if storage == "" {
		return nil
	}
	return []contracts.DownloadEventFile{{Path: storage}}
}
