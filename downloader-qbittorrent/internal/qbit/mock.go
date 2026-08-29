package qbit

import (
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

// MockServer is an in-process qBittorrent WebUI stand-in for offline tests.
type MockServer struct {
	Username string
	Password string
	Server   *httptest.Server

	mu       sync.Mutex
	seq      atomic.Uint64
	torrents map[string]*Torrent
	authed   map[string]bool
}

// NewMockServer starts an httptest WebUI that accepts the given credentials.
func NewMockServer(user, pass string) *MockServer {
	if user == "" {
		user = "admin"
	}
	if pass == "" {
		pass = "adminadmin"
	}
	m := &MockServer{
		Username: user,
		Password: pass,
		torrents: map[string]*Torrent{},
		authed:   map[string]bool{},
	}
	mux := http.NewServeMux()
	mux.HandleFunc("/api/v2/auth/login", m.handleLogin)
	mux.HandleFunc("/api/v2/app/version", m.requireAuth(m.handleVersion))
	mux.HandleFunc("/api/v2/torrents/add", m.requireAuth(m.handleAdd))
	mux.HandleFunc("/api/v2/torrents/info", m.requireAuth(m.handleInfo))
	mux.HandleFunc("/api/v2/torrents/pause", m.requireAuth(m.handlePause))
	mux.HandleFunc("/api/v2/torrents/resume", m.requireAuth(m.handleResume))
	mux.HandleFunc("/api/v2/torrents/delete", m.requireAuth(m.handleDelete))
	m.Server = httptest.NewServer(mux)
	return m
}

func (m *MockServer) URL() string { return m.Server.URL }

func (m *MockServer) Close() { m.Server.Close() }

func (m *MockServer) Client() *Client {
	return &Client{
		BaseURL:    m.URL(),
		Username:   m.Username,
		Password:   m.Password,
		HTTPClient: m.Server.Client(),
	}
}

func (m *MockServer) requireAuth(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		cookie, err := r.Cookie("SID")
		m.mu.Lock()
		ok := err == nil && m.authed[cookie.Value]
		m.mu.Unlock()
		if !ok {
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}
		next(w, r)
	}
}

func (m *MockServer) handleLogin(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	if r.Form.Get("username") != m.Username || r.Form.Get("password") != m.Password {
		w.WriteHeader(http.StatusForbidden)
		_, _ = w.Write([]byte("Fails."))
		return
	}
	sid := fmt.Sprintf("sid-%d", m.seq.Add(1))
	m.mu.Lock()
	m.authed[sid] = true
	m.mu.Unlock()
	http.SetCookie(w, &http.Cookie{Name: "SID", Value: sid, Path: "/"})
	_, _ = w.Write([]byte("Ok."))
}

func (m *MockServer) handleVersion(w http.ResponseWriter, _ *http.Request) {
	_, _ = w.Write([]byte("v4.6.0"))
}

func (m *MockServer) handleAdd(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	urls := r.Form.Get("urls")
	if urls == "" {
		http.Error(w, "empty urls", http.StatusBadRequest)
		return
	}
	hash := hashFromURL(urls)
	name := nameFromURL(urls)
	save := r.Form.Get("savepath")
	if save == "" {
		save = "/downloads"
	}
	state := "downloading"
	if r.Form.Get("paused") == "true" {
		state = "pausedDL"
	}
	t := &Torrent{
		Hash: hash, Name: name, Size: 1024 * 1024, Downloaded: 0,
		Progress: 0.1, Dlspeed: 1024, State: state, SavePath: save,
		Category: r.Form.Get("category"), AddedOn: time.Now().Unix(),
	}
	m.mu.Lock()
	m.torrents[hash] = t
	m.mu.Unlock()
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("Ok."))
}

func (m *MockServer) handleInfo(w http.ResponseWriter, r *http.Request) {
	cat := r.URL.Query().Get("category")
	m.mu.Lock()
	out := make([]Torrent, 0, len(m.torrents))
	for _, t := range m.torrents {
		if cat != "" && t.Category != cat {
			continue
		}
		out = append(out, *t)
	}
	m.mu.Unlock()
	_ = json.NewEncoder(w).Encode(out)
}

func (m *MockServer) handlePause(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	m.setState(r.Form.Get("hashes"), "pausedDL")
	_, _ = w.Write([]byte("Ok."))
}

func (m *MockServer) handleResume(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	m.setState(r.Form.Get("hashes"), "downloading")
	_, _ = w.Write([]byte("Ok."))
}

func (m *MockServer) handleDelete(w http.ResponseWriter, r *http.Request) {
	_ = r.ParseForm()
	hashes := strings.Split(r.Form.Get("hashes"), "|")
	m.mu.Lock()
	for _, h := range hashes {
		delete(m.torrents, strings.ToLower(strings.TrimSpace(h)))
	}
	m.mu.Unlock()
	_, _ = w.Write([]byte("Ok."))
}

func (m *MockServer) setState(hashes, state string) {
	parts := strings.Split(hashes, "|")
	m.mu.Lock()
	defer m.mu.Unlock()
	for _, h := range parts {
		h = strings.ToLower(strings.TrimSpace(h))
		if t, ok := m.torrents[h]; ok {
			t.State = state
		}
	}
}

// Complete marks a torrent finished (uploading).
func (m *MockServer) Complete(hash string) bool {
	m.mu.Lock()
	defer m.mu.Unlock()
	t, ok := m.torrents[strings.ToLower(hash)]
	if !ok {
		return false
	}
	t.State = "uploading"
	t.Progress = 1
	t.Downloaded = t.Size
	t.CompletionOn = time.Now().Unix()
	return true
}

func hashFromURL(u string) string {
	sum := sha1.Sum([]byte(u))
	return hex.EncodeToString(sum[:])
}

func nameFromURL(u string) string {
	if i := strings.Index(u, "dn="); i >= 0 {
		rest := u[i+3:]
		if j := strings.IndexAny(rest, "&"); j >= 0 {
			rest = rest[:j]
		}
		if rest != "" {
			return rest
		}
	}
	return "fixture-torrent"
}
