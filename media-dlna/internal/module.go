package internal

import (
	"context"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"time"

	"google.golang.org/grpc"

	"github.com/Muxcore-Media/core/pkg/contracts"
	modulesdk "github.com/Muxcore-Media/core/sdk/go/module"
	dlnav1 "github.com/Muxcore-Media/media-dlna/proto/dlnav1"
)

const moduleVersion = "0.1.0"

type Module struct {
	dlnav1.UnimplementedDlnaServiceServer

	mu sync.RWMutex

	id             string
	grpcAddr       string
	healthHTTPAddr string
	mediaPaths     []string
	browseRoot     string
	friendlyName   string
	dlnaHTTPAddr   string
	ifName         string
	noTranscode    bool
	forceTranscode string
	noProbe        bool
	ignoreHidden   bool
	ignoreUnreadable bool
	ignorePaths    []string
	notifyInterval time.Duration
	probeCachePath string

	grpcSrv *grpc.Server
	grpcLis net.Listener
	httpLis net.Listener
	runner  dmsRunner

	stopCh chan struct{}
}

type Config struct {
	ID               string
	GRPCAddr         string
	HealthHTTPAddr   string
	MediaPaths       []string
	BrowseRoot       string
	FriendlyName     string
	DlnaHTTPAddr     string
	IfName           string
	NoTranscode      bool
	ForceTranscode   string
	NoProbe          bool
	IgnoreHidden     bool
	IgnoreUnreadable bool
	IgnorePaths      []string
	NotifyInterval   time.Duration
	ProbeCachePath   string
}

func NewModule(cfg Config) *Module {
	if cfg.ID == "" {
		cfg.ID = "media-dlna"
	}
	if cfg.GRPCAddr == "" {
		cfg.GRPCAddr = ":9751"
	}
	if cfg.HealthHTTPAddr == "" {
		cfg.HealthHTTPAddr = ":8751"
	}
	if cfg.DlnaHTTPAddr == "" {
		cfg.DlnaHTTPAddr = ":9750"
	}
	if cfg.FriendlyName == "" {
		cfg.FriendlyName = "MuxCore DLNA"
	}
	if cfg.NotifyInterval <= 0 {
		cfg.NotifyInterval = 30 * time.Second
	}
	applyEnv(&cfg)

	m := &Module{
		id:               cfg.ID,
		grpcAddr:         cfg.GRPCAddr,
		healthHTTPAddr:   cfg.HealthHTTPAddr,
		mediaPaths:       append([]string(nil), cfg.MediaPaths...),
		browseRoot:         cfg.BrowseRoot,
		friendlyName:     cfg.FriendlyName,
		dlnaHTTPAddr:     cfg.DlnaHTTPAddr,
		ifName:           cfg.IfName,
		noTranscode:      cfg.NoTranscode,
		forceTranscode:   cfg.ForceTranscode,
		noProbe:          cfg.NoProbe,
		ignoreHidden:     cfg.IgnoreHidden,
		ignoreUnreadable: cfg.IgnoreUnreadable,
		ignorePaths:      append([]string(nil), cfg.IgnorePaths...),
		notifyInterval:   cfg.NotifyInterval,
		probeCachePath:   cfg.ProbeCachePath,
		stopCh:           make(chan struct{}),
	}
	m.runner.probeCachePath = m.probeCachePath
	return m
}

func applyEnv(cfg *Config) {
	if v := strings.TrimSpace(os.Getenv("DLNA_GRPC_ADDR")); v != "" {
		cfg.GRPCAddr = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_HEALTH_HTTP_ADDR")); v != "" {
		cfg.HealthHTTPAddr = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_HTTP_ADDR")); v != "" {
		cfg.DlnaHTTPAddr = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_FRIENDLY_NAME")); v != "" {
		cfg.FriendlyName = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_IFNAME")); v != "" {
		cfg.IfName = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_PROBE_CACHE_PATH")); v != "" {
		cfg.ProbeCachePath = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_FORCE_TRANSCODE")); v != "" {
		cfg.ForceTranscode = v
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_IGNORE_PATHS")); v != "" {
		cfg.IgnorePaths = splitPaths(v)
	}
	if v := strings.TrimSpace(os.Getenv("DLNA_NOTIFY_INTERVAL")); v != "" {
		if d, err := time.ParseDuration(v); err == nil && d > 0 {
			cfg.NotifyInterval = d
		}
	}
	cfg.NoTranscode = cfg.NoTranscode || envBool("DLNA_NO_TRANSCODE", false)
	cfg.NoProbe = cfg.NoProbe || envBool("DLNA_NO_PROBE", false)
	cfg.IgnoreHidden = cfg.IgnoreHidden || envBool("DLNA_IGNORE_HIDDEN", false)
	cfg.IgnoreUnreadable = cfg.IgnoreUnreadable || envBool("DLNA_IGNORE_UNREADABLE", false)
	if len(cfg.MediaPaths) == 0 {
		if paths, err := resolveMediaPaths(nil); err == nil {
			cfg.MediaPaths = paths
		}
	}
	if cfg.BrowseRoot == "" && len(cfg.MediaPaths) > 0 {
		if root, err := browseRootForPaths(cfg.MediaPaths); err == nil {
			cfg.BrowseRoot = root
		}
	}
	if cfg.ProbeCachePath == "" {
		cfg.ProbeCachePath = defaultProbeCachePath()
	}
}

func defaultProbeCachePath() string {
	if v := strings.TrimSpace(os.Getenv("XDG_CACHE_HOME")); v != "" {
		return filepath.Join(v, "media-dlna", "ffprobe-cache.json")
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return "/var/lib/media-dlna/ffprobe-cache.json"
	}
	return filepath.Join(home, ".cache", "media-dlna", "ffprobe-cache.json")
}

func (m *Module) Info() contracts.ModuleInfo {
	return contracts.ModuleInfo{
		ID:          m.id,
		Name:        "Media DLNA",
		Version:     moduleVersion,
		Roles:       []string{"media"},
		Description: "DLNA/UPnP media server for MuxCore library paths",
		Author:      "MuxCore",
		Capabilities: []string{
			"media.dlna",
			"settings",
		},
		MinCoreVersion: "0.5.0",
		HTTPAddr:       m.healthHTTPAddr,
	}
}

func (m *Module) Init(ctx context.Context) error {
	if err := m.refreshPaths(ctx); err != nil {
		return err
	}
	lis, err := net.Listen("tcp", m.grpcAddr)
	if err != nil {
		return fmt.Errorf("listen gRPC %s: %w", m.grpcAddr, err)
	}
	m.grpcLis = lis
	httpLis, err := net.Listen("tcp", m.healthHTTPAddr)
	if err != nil {
		lis.Close()
		return fmt.Errorf("listen health HTTP %s: %w", m.healthHTTPAddr, err)
	}
	m.httpLis = httpLis
	slog.Info("media-dlna initialized",
		"grpc", m.grpcAddr,
		"health", m.healthHTTPAddr,
		"dlna_http", m.dlnaHTTPAddr,
		"browse_root", m.browseRoot,
	)
	return nil
}

func (m *Module) Start(ctx context.Context) error {
	m.grpcSrv = grpc.NewServer()
	dlnav1.RegisterDlnaServiceServer(m.grpcSrv, m)
	modulesdk.RegisterSettings(m.grpcSrv, m.id, m)
	go func() {
		if err := m.grpcSrv.Serve(m.grpcLis); err != nil {
			slog.Error("media-dlna gRPC error", "error", err)
		}
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		status := "ok"
		if !m.configured() {
			status = "degraded"
		}
		_, _ = fmt.Fprintf(w, `{"status":%q,"running":%t,"browse_root":%q}`,
			status, m.runner.running(), m.browseRoot)
	})
	go func() {
		if err := http.Serve(m.httpLis, mux); err != nil && err != http.ErrServerClosed {
			slog.Error("media-dlna health HTTP error", "error", err)
		}
	}()

	if err := m.startDLNA(ctx); err != nil {
		return err
	}
	return nil
}

func (m *Module) Stop(ctx context.Context) error {
	select {
	case <-m.stopCh:
	default:
		close(m.stopCh)
	}
	if err := m.runner.stop(ctx); err != nil {
		slog.Warn("media-dlna runner stop", "error", err)
	}
	if m.grpcSrv != nil {
		m.grpcSrv.GracefulStop()
	}
	if m.httpLis != nil {
		_ = m.httpLis.Close()
	}
	slog.Info("media-dlna stopped")
	return nil
}

func (m *Module) Health(ctx context.Context) error {
	if !m.configured() {
		return fmt.Errorf("dlna not configured (set DLNA_MEDIA_PATH or DLNA_MEDIA_PATHS)")
	}
	if !m.runner.running() {
		return fmt.Errorf("dlna server not running")
	}
	return nil
}

func (m *Module) configured() bool {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.browseRoot != "" && len(m.mediaPaths) > 0
}

func (m *Module) refreshPaths(ctx context.Context) error {
	paths, err := resolveMediaPaths(m.mediaPaths)
	if err != nil {
		return err
	}
	root := m.browseRoot
	if root == "" {
		root, err = browseRootForPaths(paths)
		if err != nil {
			return err
		}
	}
	m.mu.Lock()
	m.mediaPaths = paths
	m.browseRoot = root
	m.mu.Unlock()
	return nil
}

func (m *Module) startDLNA(ctx context.Context) error {
	m.mu.RLock()
	cfg := dmsConfig{
		BrowseRoot:       m.browseRoot,
		FriendlyName:     m.friendlyName,
		HTTPAddr:         m.dlnaHTTPAddr,
		IfName:           m.ifName,
		NoTranscode:      m.noTranscode,
		ForceTranscode:   m.forceTranscode,
		NoProbe:          m.noProbe,
		IgnoreHidden:     m.ignoreHidden,
		IgnoreUnreadable: m.ignoreUnreadable,
		IgnorePaths:      append([]string(nil), m.ignorePaths...),
		NotifyInterval:   m.notifyInterval,
	}
	m.mu.RUnlock()
	return m.runner.start(ctx, cfg)
}

func (m *Module) Status(ctx context.Context, _ *dlnav1.StatusRequest) (*dlnav1.StatusResponse, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	configured := m.browseRoot != "" && len(m.mediaPaths) > 0
	return &dlnav1.StatusResponse{
		Configured:   configured,
		Running:      m.runner.running(),
		FriendlyName: m.friendlyName,
		BrowseRoot:   m.browseRoot,
		HttpAddr:     m.runner.httpAddr(),
		MediaPaths:   append([]string(nil), m.mediaPaths...),
	}, nil
}

func envBool(key string, def bool) bool {
	v := strings.TrimSpace(os.Getenv(key))
	if v == "" {
		return def
	}
	b, err := strconv.ParseBool(v)
	if err != nil {
		return def
	}
	return b
}
