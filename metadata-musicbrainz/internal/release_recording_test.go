package internal

import (
	"context"
	"testing"

	musicbrainzv1 "github.com/Muxcore-Media/metadata-musicbrainz/proto/musicbrainzv1"
)

func TestFixtureSearchReleaseGroups(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.SearchReleaseGroup(context.Background(), &musicbrainzv1.SearchReleaseGroupRequest{Query: "ok computer", Limit: 5})
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.GetReleaseGroups()) == 0 {
		t.Fatal("expected release groups")
	}
}

func TestFixtureSearchRecordings(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.SearchRecording(context.Background(), &musicbrainzv1.SearchRecordingRequest{Query: "paranoid android", Limit: 5})
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.GetRecordings()) == 0 {
		t.Fatal("expected recordings")
	}
}

func TestFixtureGetReleaseGroup(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.GetReleaseGroup(context.Background(), &musicbrainzv1.GetReleaseGroupRequest{MusicbrainzId: fixtureOKComputerRGID})
	if err != nil {
		t.Fatal(err)
	}
	if resp.GetTitle() != "OK Computer" {
		t.Fatalf("unexpected title %q", resp.GetTitle())
	}
}
