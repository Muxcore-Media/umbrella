package internal

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/google/uuid"
)

type usenetEngine interface {
	RunJob(ctx context.Context, j *job, nzbData []byte, destDir string) error
}

type fixtureEngine struct{}

func (fixtureEngine) RunJob(ctx context.Context, j *job, _ []byte, destDir string) error {
	j.setProgress(8*1024, 0, 0)
	j.setStatus(jobStatusDownloading)

	select {
	case <-ctx.Done():
		return ctx.Err()
	default:
	}

	dir := jobDir(destDir, j.category, j.name)
	if _, err := writeFixtureMedia(dir, j.name, 8*1024); err != nil {
		return failJob(j, err)
	}
	_, err := finalizeDownload(ctx, j, dir)
	return err
}

type jobManager struct {
	mu      sync.RWMutex
	jobs    map[string]*job
	history []*job
	engine  usenetEngine
	destDir string
	http    *httpClientWrap
}

type httpClientWrap struct {
	client *http.Client
}

func newJobManager(engine usenetEngine, destDir string) *jobManager {
	if destDir == "" {
		destDir = "/var/lib/downloader-native-usenet/downloads"
	}
	return &jobManager{
		jobs:    make(map[string]*job),
		engine:  engine,
		destDir: destDir,
	}
}

func (m *jobManager) add(ctx context.Context, nzbURL, name, category string, paused bool) (*job, error) {
	if name == "" {
		name = nzbURL
	}
	raw, err := fetchNZB(ctx, m.httpClient(), nzbURL)
	if err != nil {
		return nil, err
	}
	id := uuid.NewString()
	j := &job{
		id:       id,
		name:     name,
		category: category,
		nzbURL:   nzbURL,
		status:   jobStatusQueued,
	}
	if paused {
		j.setStatus(jobStatusPaused)
		j.setPaused(true)
	}
	m.mu.Lock()
	m.jobs[id] = j
	m.mu.Unlock()

	if !paused {
		m.startDownload(id, raw)
	}
	return j, nil
}

func (m *jobManager) httpClient() *http.Client {
	if m.http != nil && m.http.client != nil {
		return m.http.client
	}
	return nil
}

func (m *jobManager) startDownload(id string, nzbData []byte) {
	m.mu.RLock()
	j, ok := m.jobs[id]
	m.mu.RUnlock()
	if !ok || j.isDeleted() {
		return
	}
	ctx, cancel := context.WithCancel(context.Background())
	j.cancel = cancel
	go func() {
		err := m.engine.RunJob(ctx, j, nzbData, m.destDir)
		st, _, _, errMsg := j.snapshot()
		if err != nil && st != jobStatusCompleted {
			if ctx.Err() != nil {
				j.setStatus(jobStatusPaused)
				j.setPaused(true)
				return
			}
			if errMsg == "" {
				j.markFailed(err.Error())
			}
		}
		m.mu.Lock()
		defer m.mu.Unlock()
		if j.isDeleted() {
			return
		}
		delete(m.jobs, id)
		m.history = prependHistory(m.history, j, 100)
	}()
}

func prependHistory(hist []*job, j *job, limit int) []*job {
	out := []*job{j}
	for _, old := range hist {
		if old.id == j.id {
			continue
		}
		out = append(out, old)
		if len(out) >= limit {
			break
		}
	}
	return out
}

func (m *jobManager) queueSnapshot(category string) []*job {
	m.mu.RLock()
	defer m.mu.RUnlock()
	out := make([]*job, 0, len(m.jobs))
	for _, j := range m.jobs {
		if category != "" {
			_, _, cat, _, _, _, _ := j.queueView()
			if cat != category {
				continue
			}
		}
		out = append(out, j)
	}
	return out
}

func (m *jobManager) historySnapshot(limit int) []*job {
	m.mu.RLock()
	defer m.mu.RUnlock()
	if limit <= 0 || limit > len(m.history) {
		limit = len(m.history)
	}
	out := make([]*job, limit)
	copy(out, m.history[:limit])
	return out
}

func (m *jobManager) pause(id string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if id == "" {
		for _, j := range m.jobs {
			j.setPaused(true)
			j.setStatus(jobStatusPaused)
			if j.cancel != nil {
				j.cancel()
			}
		}
		return nil
	}
	j, ok := m.jobs[id]
	if !ok {
		return fmt.Errorf("job %q not found", id)
	}
	j.setPaused(true)
	j.setStatus(jobStatusPaused)
	if j.cancel != nil {
		j.cancel()
	}
	return nil
}

func (m *jobManager) resume(id string, nzbFetcher func(context.Context, string) ([]byte, error)) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	if id == "" {
		for jid, j := range m.jobs {
			if !j.isPaused() {
				continue
			}
			j.setPaused(false)
			j.setStatus(jobStatusQueued)
			url := j.nzbURL
			go func(jobID, nzbURL string) {
				ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
				defer cancel()
				raw, err := nzbFetcher(ctx, nzbURL)
				if err != nil {
					slog.Warn("resume fetch nzb failed", "job", jobID, "error", err)
					return
				}
				m.startDownload(jobID, raw)
			}(jid, url)
		}
		return nil
	}
	j, ok := m.jobs[id]
	if !ok {
		return fmt.Errorf("job %q not found", id)
	}
	if !j.isPaused() {
		return nil
	}
	j.setPaused(false)
	j.setStatus(jobStatusQueued)
	url := j.nzbURL
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
		defer cancel()
		raw, err := nzbFetcher(ctx, url)
		if err != nil {
			slog.Warn("resume fetch nzb failed", "job", id, "error", err)
			return
		}
		m.startDownload(id, raw)
	}()
	return nil
}

func (m *jobManager) delete(id string, deleteFiles bool) error {
	m.mu.Lock()
	j, ok := m.jobs[id]
	if ok {
		j.markDeleted()
		if j.cancel != nil {
			j.cancel()
		}
		delete(m.jobs, id)
	}
	m.mu.Unlock()
	if !ok {
		return fmt.Errorf("job %q not found", id)
	}
	if deleteFiles {
		if sp := j.storagePath(); sp != "" {
			_ = os.RemoveAll(filepath.Dir(sp))
		}
	}
	return nil
}

func (m *jobManager) findHistory(id string) *job {
	m.mu.RLock()
	defer m.mu.RUnlock()
	for _, j := range m.history {
		if j.id == id {
			return j
		}
	}
	return nil
}
