//go:build integration

package integration

import (
	"context"
	"os"
	"path/filepath"
	"testing"
	"time"

	transcoderinteg "github.com/Muxcore-Media/media-transcoder/integsupport"
	transcodev1 "github.com/Muxcore-Media/media-transcoder/proto/transcodev1"
)

func TestMediaTranscoder_EnqueueListCancel(t *testing.T) {
	ctx := context.Background()
	m := transcoderinteg.NewTestModule(t, transcoderConfig(t))

	dir := t.TempDir()
	input := filepath.Join(dir, "input.mkv")
	output := filepath.Join(dir, "output.mkv")
	if err := os.WriteFile(input, []byte("video"), 0o600); err != nil {
		t.Fatal(err)
	}

	enq, err := m.Enqueue(ctx, &transcodev1.EnqueueRequest{
		InputPath:  input,
		OutputPath: output,
		ProfileId:  "h264_fast",
	})
	if err != nil {
		t.Fatalf("enqueue: %v", err)
	}
	if enq.GetJobId() == "" {
		t.Fatal("expected job id")
	}

	_, err = m.CancelJob(ctx, &transcodev1.CancelJobRequest{JobId: enq.GetJobId()})
	if err != nil {
		t.Fatalf("cancel job: %v", err)
	}

	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		job, getErr := m.GetJob(ctx, &transcodev1.GetJobRequest{JobId: enq.GetJobId()})
		if getErr == nil && job.GetJob().GetStatus() == "cancelled" {
			return
		}
		time.Sleep(50 * time.Millisecond)
	}
	t.Fatal("job was not cancelled before timeout")
}

func TestMediaTranscoder_JobListOrdering(t *testing.T) {
	ctx := context.Background()
	m := transcoderinteg.NewTestModule(t, transcoderConfig(t))

	dir := t.TempDir()
	var ids []string
	for i := 0; i < 3; i++ {
		in := filepath.Join(dir, "in-"+string(rune('a'+i))+".mkv")
		out := filepath.Join(dir, "out-"+string(rune('a'+i))+".mkv")
		if err := os.WriteFile(in, []byte("x"), 0o600); err != nil {
			t.Fatal(err)
		}
		resp, err := m.Enqueue(ctx, &transcodev1.EnqueueRequest{
			InputPath:  in,
			OutputPath: out,
			ProfileId:  "h264_fast",
		})
		if err != nil {
			t.Fatalf("enqueue %d: %v", i, err)
		}
		ids = append(ids, resp.GetJobId())
		time.Sleep(5 * time.Millisecond)
	}

	list, err := m.ListJobs(ctx, &transcodev1.ListJobsRequest{Page: 1, PageSize: 10})
	if err != nil {
		t.Fatalf("list jobs: %v", err)
	}
	if list.GetTotal() < 3 {
		t.Fatalf("expected at least 3 jobs, got %d", list.GetTotal())
	}
	// ListJobs orders by created_at DESC, stored with one-second (RFC3339) resolution,
	// so jobs enqueued within the same second have no defined relative order. Assert
	// the published contract: every enqueued job is listed and created_at never increases.
	seen := map[string]bool{}
	prev := ""
	for _, j := range list.GetJobs() {
		seen[j.GetId()] = true
		if prev != "" && j.GetCreatedAt() > prev {
			t.Fatalf("jobs not ordered by created_at DESC: %s after %s", j.GetCreatedAt(), prev)
		}
		prev = j.GetCreatedAt()
	}
	for _, id := range ids {
		if !seen[id] {
			t.Fatalf("enqueued job %s missing from list", id)
		}
	}
}

func TestMediaTranscoder_Health(t *testing.T) {
	ctx := context.Background()
	m := transcoderinteg.NewTestModule(t, transcoderConfig(t))
	if err := m.Health(ctx); err != nil {
		t.Fatalf("health: %v", err)
	}
}
