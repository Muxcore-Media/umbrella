package internal

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/anacrolix/dms/dlna/dms"
	"github.com/anacrolix/dms/rrcache"
)

type dmsRunner struct {
	mu sync.Mutex

	srv            *dms.Server
	probeCache     *ffprobeCache
	probeCachePath string
	runDone        chan struct{}
}

type dmsConfig struct {
	BrowseRoot      string
	FriendlyName    string
	HTTPAddr        string
	IfName          string
	NoTranscode     bool
	ForceTranscode  string
	NoProbe         bool
	IgnoreHidden    bool
	IgnoreUnreadable bool
	IgnorePaths     []string
	NotifyInterval  time.Duration
}

func (r *dmsRunner) start(ctx context.Context, cfg dmsConfig) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.srv != nil {
		return fmt.Errorf("dlna server already running")
	}

	listener, network, err := listenTCP(cfg.HTTPAddr)
	if err != nil {
		return err
	}

	cache := &ffprobeCache{c: rrcache.New(64 << 20)}
	if r.probeCachePath != "" {
		if err := cache.load(r.probeCachePath); err != nil && !os.IsNotExist(err) {
			slog.Warn("dlna: ffprobe cache load failed", "path", r.probeCachePath, "error", err)
		}
	}
	r.probeCache = cache

	srv := &dms.Server{
		Logger:              slog.Default(),
		HTTPConn:            listener,
		FriendlyName:        cfg.FriendlyName,
		RootObjectPath:      filepath.Clean(cfg.BrowseRoot),
		FFProbeCache:        cache,
		NoTranscode:         cfg.NoTranscode,
		ForceTranscodeTo:    cfg.ForceTranscode,
		NoProbe:             cfg.NoProbe,
		IgnoreHidden:        cfg.IgnoreHidden,
		IgnoreUnreadable:    cfg.IgnoreUnreadable,
		IgnorePaths:         cfg.IgnorePaths,
		NotifyInterval:      cfg.NotifyInterval,
		Interfaces:          activeInterfaces(cfg.IfName),
	}
	if err := srv.Init(); err != nil {
		listener.Close()
		return fmt.Errorf("init dlna server: %w", err)
	}

	runDone := make(chan struct{})
	go func() {
		defer close(runDone)
		if err := srv.Run(); err != nil {
			slog.Error("dlna server stopped", "error", err)
		}
	}()

	r.srv = srv
	r.runDone = runDone
	slog.Info("dlna server started",
		"network", network,
		"addr", listener.Addr().String(),
		"browse_root", cfg.BrowseRoot,
		"friendly_name", cfg.FriendlyName,
	)
	return nil
}

func (r *dmsRunner) stop(ctx context.Context) error {
	r.mu.Lock()
	srv := r.srv
	runDone := r.runDone
	cache := r.probeCache
	cachePath := r.probeCachePath
	r.srv = nil
	r.runDone = nil
	r.probeCache = nil
	r.mu.Unlock()

	if srv == nil {
		return nil
	}
	err := srv.Close()
	if runDone != nil {
		select {
		case <-runDone:
		case <-ctx.Done():
		case <-time.After(10 * time.Second):
		}
	}
	if cache != nil && cachePath != "" {
		if saveErr := cache.save(cachePath); saveErr != nil {
			slog.Warn("dlna: ffprobe cache save failed", "path", cachePath, "error", saveErr)
		}
	}
	slog.Info("dlna server stopped")
	return err
}

func (r *dmsRunner) httpAddr() string {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.srv == nil || r.srv.HTTPConn == nil {
		return ""
	}
	return r.srv.HTTPConn.Addr().String()
}

func (r *dmsRunner) running() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	return r.srv != nil
}

func listenTCP(addr string) (net.Listener, string, error) {
	if strings.TrimSpace(addr) == "" {
		addr = ":9750"
	}
	host, _, err := net.SplitHostPort(addr)
	if err != nil {
		return nil, "", fmt.Errorf("parse dlna http addr %q: %w", addr, err)
	}
	network := "tcp"
	if ip := net.ParseIP(host); ip != nil {
		if ip.To4() != nil {
			network = "tcp4"
		} else {
			network = "tcp6"
		}
	}
	lis, err := net.Listen(network, addr)
	if err != nil {
		return nil, "", fmt.Errorf("listen dlna http %s: %w", addr, err)
	}
	return lis, network, nil
}

func activeInterfaces(ifName string) []net.Interface {
	var (
		ifs []net.Interface
		err error
	)
	if strings.TrimSpace(ifName) == "" {
		ifs, err = net.Interfaces()
	} else {
		var if_ *net.Interface
		if_, err = net.InterfaceByName(ifName)
		if if_ != nil {
			ifs = append(ifs, *if_)
		}
	}
	if err != nil {
		slog.Error("dlna: list network interfaces", "error", err)
		return nil
	}
	out := make([]net.Interface, 0, len(ifs))
	for _, if_ := range ifs {
		if if_.Flags&net.FlagUp == 0 || if_.MTU <= 0 {
			continue
		}
		out = append(out, if_)
	}
	return out
}

type ffprobeCache struct {
	c *rrcache.RRCache
	mu sync.Mutex
}

func (fc *ffprobeCache) Get(key interface{}) (value interface{}, ok bool) {
	fc.mu.Lock()
	defer fc.mu.Unlock()
	return fc.c.Get(key)
}

func (fc *ffprobeCache) Set(key interface{}, value interface{}) {
	fc.mu.Lock()
	defer fc.mu.Unlock()
	var size int64
	for _, v := range []interface{}{key, value} {
		b, err := json.Marshal(v)
		if err != nil {
			continue
		}
		size += int64(len(b))
	}
	fc.c.Set(key, value, size)
}

func (fc *ffprobeCache) load(path string) error {
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()
	var items []dms.FfprobeCacheItem
	if err := json.NewDecoder(f).Decode(&items); err != nil {
		return err
	}
	for _, item := range items {
		fc.Set(item.Key, item.Value)
	}
	return nil
}

func (fc *ffprobeCache) save(path string) error {
	if err := os.MkdirAll(filepath.Dir(path), 0700); err != nil {
		return err
	}
	fc.mu.Lock()
	items := fc.c.Items()
	fc.mu.Unlock()
	f, err := os.CreateTemp(filepath.Dir(path), filepath.Base(path)+".*")
	if err != nil {
		return err
	}
	if err := json.NewEncoder(f).Encode(items); err != nil {
		f.Close()
		os.Remove(f.Name())
		return err
	}
	if err := f.Close(); err != nil {
		os.Remove(f.Name())
		return err
	}
	return os.Rename(f.Name(), path)
}
