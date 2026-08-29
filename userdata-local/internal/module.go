package internal

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"os"
	"strings"
	"sync"

	"google.golang.org/grpc"

	"github.com/Muxcore-Media/core/pkg/contracts"
	"github.com/Muxcore-Media/core/pkg/tenant"
	modulesdk "github.com/Muxcore-Media/core/sdk/go/module"
	"github.com/Muxcore-Media/userdata-local/store"
)

type Module struct {
	mu       sync.RWMutex
	id       string
	httpAddr string
	dataDir  string
	store    *store.Store
	httpLis  net.Listener
	grpcSrv  *grpc.Server
	grpcLis  net.Listener
	grpcAddr string
}

type Config struct {
	ID       string
	HTTPAddr string
	GRPCAddr string
	DataDir  string
}

func NewModule(cfg Config) *Module {
	if cfg.ID == "" {
		cfg.ID = "userdata-local"
	}
	if cfg.HTTPAddr == "" {
		cfg.HTTPAddr = ":9672"
	}
	if cfg.GRPCAddr == "" {
		cfg.GRPCAddr = ":9673"
	}
	if cfg.DataDir == "" {
		cfg.DataDir = "/var/lib/muxcore-userdata"
	}
	if v := strings.TrimSpace(os.Getenv("USERDATA_LOCAL_HTTP_ADDR")); v != "" {
		cfg.HTTPAddr = v
	}
	if v := strings.TrimSpace(os.Getenv("USERDATA_LOCAL_GRPC_ADDR")); v != "" {
		cfg.GRPCAddr = v
	}
	if v := strings.TrimSpace(os.Getenv("USERDATA_LOCAL_DATA_DIR")); v != "" {
		cfg.DataDir = v
	}
	return &Module{
		id:       cfg.ID,
		httpAddr: cfg.HTTPAddr,
		grpcAddr: cfg.GRPCAddr,
		dataDir:  cfg.DataDir,
	}
}

func (m *Module) Info() contracts.ModuleInfo {
	return contracts.ModuleInfo{
		ID:             m.id,
		Name:           "Userdata Local",
		Version:        "0.1.0",
		Roles:          []string{"infrastructure"},
		Description:    "Durable per-user progress/favorites/prefs/queue (household loop)",
		Author:         "MuxCore",
		Capabilities:   []string{"userdata", "userdata.local", "settings"},
		MinCoreVersion: "0.5.0",
		HTTPAddr:       m.httpAddr,
	}
}

func (m *Module) Init(ctx context.Context) error {
	st, err := store.New(m.dataDir)
	if err != nil {
		return err
	}
	m.store = st
	m.httpLis, err = net.Listen("tcp", m.httpAddr)
	if err != nil {
		return fmt.Errorf("listen HTTP %s: %w", m.httpAddr, err)
	}
	m.grpcLis, err = net.Listen("tcp", m.grpcAddr)
	if err != nil {
		return fmt.Errorf("listen gRPC %s: %w", m.grpcAddr, err)
	}
	slog.Info("userdata-local initialized", "http", m.httpAddr, "data_dir", m.dataDir)
	return nil
}

func (m *Module) Start(ctx context.Context) error {
	m.grpcSrv = grpc.NewServer()
	modulesdk.RegisterSettings(m.grpcSrv, m.id, m)
	go func() {
		slog.Info("userdata-local gRPC started", "addr", m.grpcAddr)
		if err := m.grpcSrv.Serve(m.grpcLis); err != nil {
			slog.Error("userdata-local gRPC error", "error", err)
		}
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})
	mux.HandleFunc("GET /userdata", m.handleGet)
	mux.HandleFunc("PUT /userdata", m.handlePut)
	mux.HandleFunc("POST /userdata", m.handlePut)
	go func() {
		slog.Info("userdata-local HTTP started", "addr", m.httpAddr)
		if err := http.Serve(m.httpLis, tenant.Middleware(mux)); err != nil {
			slog.Error("userdata-local HTTP error", "error", err)
		}
	}()
	return nil
}

func (m *Module) Stop(ctx context.Context) error {
	if m.grpcSrv != nil {
		m.grpcSrv.GracefulStop()
	}
	if m.httpLis != nil {
		_ = m.httpLis.Close()
	}
	slog.Info("userdata-local stopped")
	return nil
}

func (m *Module) Health(ctx context.Context) error {
	if m.store == nil {
		return fmt.Errorf("store not initialized")
	}
	return nil
}

func (m *Module) scopeFrom(r *http.Request) store.Scope {
	userID := strings.TrimSpace(r.URL.Query().Get("user_id"))
	if userID == "" {
		userID = strings.TrimSpace(r.Header.Get("X-User-ID"))
	}
	if userID == "" {
		userID = "anonymous"
	}
	tenantID := ""
	if tenant.Enabled() {
		tenantID = tenant.ResolveFromRequest(r)
	}
	return store.Scope{TenantID: tenantID, UserID: userID}
}

func (m *Module) handleGet(w http.ResponseWriter, r *http.Request) {
	blob := m.store.Get(m.scopeFrom(r))
	writeJSON(w, blob)
}

func (m *Module) handlePut(w http.ResponseWriter, r *http.Request) {
	var incoming store.Blob
	if err := json.NewDecoder(r.Body).Decode(&incoming); err != nil {
		http.Error(w, `{"error":"invalid json"}`, http.StatusBadRequest)
		return
	}
	merged, err := m.store.Put(m.scopeFrom(r), incoming)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	writeJSON(w, merged)
}

func writeJSON(w http.ResponseWriter, v any) {
	w.Header().Set("Content-Type", "application/json")
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	_ = enc.Encode(v)
}
