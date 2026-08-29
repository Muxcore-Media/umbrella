package internal

import (
	"context"
	"testing"

	musicbrainzv1 "github.com/Muxcore-Media/metadata-musicbrainz/proto/musicbrainzv1"
)

func TestFixtureSearchArtist(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.SearchArtist(context.Background(), &musicbrainzv1.SearchArtistRequest{Query: "radiohead"})
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.GetArtists()) != 1 || resp.GetArtists()[0].GetName() != "Radiohead" {
		t.Fatalf("unexpected search: %+v", resp.GetArtists())
	}
}

func TestFixtureGetArtist(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.GetArtist(context.Background(), &musicbrainzv1.GetArtistRequest{
		MusicbrainzId: fixtureRadioheadMBID,
	})
	if err != nil {
		t.Fatal(err)
	}
	if resp.GetName() != "Radiohead" {
		t.Fatalf("name=%q", resp.GetName())
	}
	if len(resp.GetReleaseGroups()) < 2 {
		t.Fatalf("expected release groups, got %+v", resp.GetReleaseGroups())
	}
}

func TestFixtureSearchEmpty(t *testing.T) {
	m := NewModule(Config{Fixture: true})
	resp, err := m.SearchArtist(context.Background(), &musicbrainzv1.SearchArtistRequest{Query: "unknown-artist-xyz"})
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.GetArtists()) != 0 {
		t.Fatalf("expected empty, got %+v", resp.GetArtists())
	}
}
