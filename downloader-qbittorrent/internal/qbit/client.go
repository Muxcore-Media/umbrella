// Package qbit implements a minimal qBittorrent WebUI API client.
package qbit

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"strings"
	"sync"
	"time"
)

// Client talks to qBittorrent's /api/v2 endpoints.
type Client struct {
	BaseURL    string
	Username   string
	Password   string
	HTTPClient *http.Client

	mu     sync.Mutex
	logged bool
}

func (c *Client) http() *http.Client {
	if c.HTTPClient != nil {
		if c.HTTPClient.Jar == nil {
			jar, _ := cookiejar.New(nil)
			// Clone transport/timeout but attach a jar so SID cookies stick.
			c.HTTPClient = &http.Client{
				Transport:     c.HTTPClient.Transport,
				CheckRedirect: c.HTTPClient.CheckRedirect,
				Timeout:       c.HTTPClient.Timeout,
				Jar:           jar,
			}
		}
		return c.HTTPClient
	}
	jar, _ := cookiejar.New(nil)
	c.HTTPClient = &http.Client{Timeout: 30 * time.Second, Jar: jar}
	return c.HTTPClient
}

func (c *Client) apiURL(path string) (string, error) {
	if c.BaseURL == "" {
		return "", fmt.Errorf("qbittorrent base URL required")
	}
	base := strings.TrimRight(c.BaseURL, "/")
	return base + "/api/v2" + path, nil
}

func (c *Client) ensureLogin(ctx context.Context) error {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.logged {
		return nil
	}
	u, err := c.apiURL("/auth/login")
	if err != nil {
		return err
	}
	form := url.Values{}
	form.Set("username", c.Username)
	form.Set("password", c.Password)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u, strings.NewReader(form.Encode()))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := c.http().Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if resp.StatusCode >= 300 {
		return fmt.Errorf("qbittorrent login: HTTP %d: %s", resp.StatusCode, strings.TrimSpace(string(body)))
	}
	if s := strings.TrimSpace(string(body)); s != "" && !strings.EqualFold(s, "Ok.") {
		return fmt.Errorf("qbittorrent login failed: %s", s)
	}
	c.logged = true
	return nil
}

func (c *Client) doForm(ctx context.Context, path string, form url.Values) ([]byte, error) {
	if err := c.ensureLogin(ctx); err != nil {
		return nil, err
	}
	u, err := c.apiURL(path)
	if err != nil {
		return nil, err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, u, strings.NewReader(form.Encode()))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	resp, err := c.http().Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(io.LimitReader(resp.Body, 4<<20))
	if err != nil {
		return nil, err
	}
	if resp.StatusCode == http.StatusForbidden {
		c.mu.Lock()
		c.logged = false
		c.mu.Unlock()
		return nil, fmt.Errorf("qbittorrent %s: forbidden (re-login required)", path)
	}
	if resp.StatusCode >= 300 {
		return nil, fmt.Errorf("qbittorrent %s: HTTP %d: %s", path, resp.StatusCode, strings.TrimSpace(string(body)))
	}
	return body, nil
}

func (c *Client) doGET(ctx context.Context, path string, query url.Values) ([]byte, error) {
	if err := c.ensureLogin(ctx); err != nil {
		return nil, err
	}
	u, err := c.apiURL(path)
	if err != nil {
		return nil, err
	}
	if len(query) > 0 {
		u += "?" + query.Encode()
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return nil, err
	}
	resp, err := c.http().Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(io.LimitReader(resp.Body, 8<<20))
	if err != nil {
		return nil, err
	}
	if resp.StatusCode == http.StatusForbidden {
		c.mu.Lock()
		c.logged = false
		c.mu.Unlock()
		return nil, fmt.Errorf("qbittorrent %s: forbidden (re-login required)", path)
	}
	if resp.StatusCode >= 300 {
		return nil, fmt.Errorf("qbittorrent %s: HTTP %d: %s", path, resp.StatusCode, strings.TrimSpace(string(body)))
	}
	return body, nil
}

// Torrent is a simplified torrents/info row.
type Torrent struct {
	Hash         string  `json:"hash"`
	Name         string  `json:"name"`
	Size         int64   `json:"size"`
	Downloaded   int64   `json:"downloaded"`
	Uploaded     int64   `json:"uploaded"`
	Progress     float64 `json:"progress"`
	Dlspeed      int64   `json:"dlspeed"`
	Upspeed      int64   `json:"upspeed"`
	NumSeeds     int     `json:"num_seeds"`
	NumLeechs    int     `json:"num_leechs"`
	State        string  `json:"state"`
	SavePath     string  `json:"save_path"`
	ContentPath  string  `json:"content_path,omitempty"` // fixture: completed media file
	Category     string  `json:"category"`
	AddedOn      int64   `json:"added_on"`
	CompletionOn int64   `json:"completion_on"`
}

// AddTorrent queues a magnet or .torrent URL.
func (c *Client) AddTorrent(ctx context.Context, torrentURL, savePath, category string, paused bool) error {
	form := url.Values{}
	form.Set("urls", torrentURL)
	if savePath != "" {
		form.Set("savepath", savePath)
	}
	if category != "" {
		form.Set("category", category)
	}
	if paused {
		form.Set("paused", "true")
	}
	_, err := c.doForm(ctx, "/torrents/add", form)
	return err
}

// ListTorrents returns torrents, optionally filtered by category.
func (c *Client) ListTorrents(ctx context.Context, category, filter string) ([]Torrent, error) {
	q := url.Values{}
	if category != "" {
		q.Set("category", category)
	}
	if filter != "" {
		q.Set("filter", filter)
	}
	body, err := c.doGET(ctx, "/torrents/info", q)
	if err != nil {
		return nil, err
	}
	var out []Torrent
	if err := json.Unmarshal(body, &out); err != nil {
		return nil, fmt.Errorf("decode torrents/info: %w", err)
	}
	return out, nil
}

// GetTorrent returns one torrent by hash, or nil if missing.
func (c *Client) GetTorrent(ctx context.Context, hash string) (*Torrent, error) {
	hash = strings.ToLower(strings.TrimSpace(hash))
	if hash == "" {
		return nil, fmt.Errorf("hash required")
	}
	list, err := c.ListTorrents(ctx, "", "")
	if err != nil {
		return nil, err
	}
	for i := range list {
		if strings.EqualFold(list[i].Hash, hash) {
			t := list[i]
			return &t, nil
		}
	}
	return nil, nil
}

// Pause pauses one or more torrents by hash (pipe-separated).
func (c *Client) Pause(ctx context.Context, hashes string) error {
	_, err := c.doForm(ctx, "/torrents/pause", url.Values{"hashes": {hashes}})
	return err
}

// Resume resumes one or more torrents by hash.
func (c *Client) Resume(ctx context.Context, hashes string) error {
	_, err := c.doForm(ctx, "/torrents/resume", url.Values{"hashes": {hashes}})
	return err
}

// Delete removes torrents; optionally delete files from disk.
func (c *Client) Delete(ctx context.Context, hashes string, deleteFiles bool) error {
	form := url.Values{"hashes": {hashes}}
	if deleteFiles {
		form.Set("deleteFiles", "true")
	} else {
		form.Set("deleteFiles", "false")
	}
	_, err := c.doForm(ctx, "/torrents/delete", form)
	return err
}

// Version hits /app/version as a cheap health probe.
func (c *Client) Version(ctx context.Context) (string, error) {
	body, err := c.doGET(ctx, "/app/version", nil)
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(string(body)), nil
}

// MapState converts qBit state strings to contracts-downloader status labels.
func MapState(state string) string {
	switch strings.ToLower(state) {
	case "uploading", "stalledup", "forcedup", "queuedup", "pausedup", "checkingup":
		return "completed"
	case "downloading", "forceddl", "metadl", "queueddl", "allocating", "checkingdl", "checkingresumedata", "moving":
		return "downloading"
	case "pauseddl":
		return "paused"
	case "error", "missingfiles":
		return "failed"
	case "stalleddl":
		return "stalled"
	default:
		if state == "" {
			return "unknown"
		}
		return state
	}
}
