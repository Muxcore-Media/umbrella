package internal

import (
	"strings"
)

const (
	fixtureRadioheadMBID     = "a74b1b7f-71a5-396a-9c10-038d927792b6"
	fixtureDaftPunkMBID      = "45609a31-061c-4236-a368-f4d182d247ba"
	fixtureBjorkMBID         = "4e0f5cc2-4a16-4607-8d77-762177b4251a"
	fixtureOKComputerRGID    = "c559b2f8-c644-4c3a-a141-622f95c88d97"
	fixtureParanoidRecID     = "b5a57158-9b1f-4b55-9949-8b275d4ca1b4"
)

func fixtureQueryMatch(q string, needles ...string) bool {
	q = strings.ToLower(strings.TrimSpace(q))
	for _, n := range needles {
		n = strings.ToLower(strings.TrimSpace(n))
		if n != "" && strings.Contains(q, n) {
			return true
		}
	}
	return false
}

func fixtureSearchArtists(query string, limit int) []artistSearchRow {
	if limit <= 0 {
		limit = 10
	}
	var all []artistSearchRow
	switch {
	case fixtureQueryMatch(query, "radiohead"):
		all = append(all, artistSearchRow{
			ID: "a74b1b7f-71a5-396a-9c10-038d927792b6", Name: "Radiohead",
			Disambiguation: "British alternative rock band", Country: "GB", Type: "Group", Score: 100,
		})
	case fixtureQueryMatch(query, "daft punk", "daft"):
		all = append(all, artistSearchRow{
			ID: "45609a31-061c-4236-a368-f4d182d247ba", Name: "Daft Punk",
			Disambiguation: "French electronic duo", Country: "FR", Type: "Group", Score: 100,
		})
	case fixtureQueryMatch(query, "björk", "bjork"):
		all = append(all, artistSearchRow{
			ID: "4e0f5cc2-4a16-4607-8d77-762177b4251a", Name: "Björk",
			Disambiguation: "Icelandic singer-songwriter", Country: "IS", Type: "Person", Score: 100,
		})
	}
	if len(all) > limit {
		return all[:limit]
	}
	return all
}

func fixtureGetArtist(mbid string) (*artistDetail, bool) {
	switch strings.ToLower(strings.TrimSpace(mbid)) {
	case fixtureRadioheadMBID:
		return &artistDetail{
			ID: fixtureRadioheadMBID, Name: "Radiohead",
			Disambiguation: "British alternative rock band", Country: "GB", Type: "Group",
			ReleaseGroups: []releaseGroupRow{
				{ID: "c559b2f8-c644-4c3a-a141-622f95c88d97", Title: "OK Computer", PrimaryType: "Album", Year: 1997},
				{ID: "6a479c5b-ef77-4858-9e30-61a6b4d7d477", Title: "Kid A", PrimaryType: "Album", Year: 2000},
			},
		}, true
	case fixtureDaftPunkMBID:
		return &artistDetail{
			ID: fixtureDaftPunkMBID, Name: "Daft Punk",
			Disambiguation: "French electronic duo", Country: "FR", Type: "Group",
			ReleaseGroups: []releaseGroupRow{
				{ID: "c559b2f8-c644-4c3a-a141-622f95c88d98", Title: "Discovery", PrimaryType: "Album", Year: 2001},
			},
		}, true
	case fixtureBjorkMBID:
		return &artistDetail{
			ID: fixtureBjorkMBID, Name: "Björk",
			Disambiguation: "Icelandic singer-songwriter", Country: "IS", Type: "Person",
			ReleaseGroups: []releaseGroupRow{
				{ID: "c559b2f8-c644-4c3a-a141-622f95c88d99", Title: "Post", PrimaryType: "Album", Year: 1995},
			},
		}, true
	default:
		return nil, false
	}
}

func fixtureSearchReleaseGroups(query string, limit int) []releaseGroupSearchRow {
	if limit <= 0 {
		limit = 10
	}
	var all []releaseGroupSearchRow
	if fixtureQueryMatch(query, "ok computer", "radiohead") {
		all = append(all, releaseGroupSearchRow{
			ID: fixtureOKComputerRGID, Title: "OK Computer", ArtistID: fixtureRadioheadMBID,
			ArtistName: "Radiohead", PrimaryType: "Album", Year: 1997, Score: 100,
		})
	}
	if fixtureQueryMatch(query, "discovery", "daft") {
		all = append(all, releaseGroupSearchRow{
			ID: "c559b2f8-c644-4c3a-a141-622f95c88d98", Title: "Discovery", ArtistID: fixtureDaftPunkMBID,
			ArtistName: "Daft Punk", PrimaryType: "Album", Year: 2001, Score: 95,
		})
	}
	if len(all) > limit {
		return all[:limit]
	}
	return all
}

func fixtureGetReleaseGroup(mbid string) (*releaseGroupDetail, bool) {
	switch strings.ToLower(strings.TrimSpace(mbid)) {
	case fixtureOKComputerRGID:
		return &releaseGroupDetail{
			ID: fixtureOKComputerRGID, Title: "OK Computer", ArtistID: fixtureRadioheadMBID,
			ArtistName: "Radiohead", PrimaryType: "Album", Year: 1997,
		}, true
	case "c559b2f8-c644-4c3a-a141-622f95c88d98":
		return &releaseGroupDetail{
			ID: "c559b2f8-c644-4c3a-a141-622f95c88d98", Title: "Discovery", ArtistID: fixtureDaftPunkMBID,
			ArtistName: "Daft Punk", PrimaryType: "Album", Year: 2001,
		}, true
	default:
		return nil, false
	}
}

func fixtureSearchRecordings(query string, limit int) []recordingSearchRow {
	if limit <= 0 {
		limit = 10
	}
	var all []recordingSearchRow
	if fixtureQueryMatch(query, "paranoid android", "radiohead") {
		all = append(all, recordingSearchRow{
			ID: fixtureParanoidRecID, Title: "Paranoid Android", ArtistID: fixtureRadioheadMBID,
			ArtistName: "Radiohead", ReleaseGroupID: fixtureOKComputerRGID, ReleaseGroupTitle: "OK Computer", Score: 100,
		})
	}
	if len(all) > limit {
		return all[:limit]
	}
	return all
}

func fixtureGetRecording(mbid string) (*recordingDetail, bool) {
	switch strings.ToLower(strings.TrimSpace(mbid)) {
	case fixtureParanoidRecID:
		return &recordingDetail{
			ID: fixtureParanoidRecID, Title: "Paranoid Android", ArtistID: fixtureRadioheadMBID,
			ArtistName: "Radiohead", ReleaseGroupID: fixtureOKComputerRGID, ReleaseGroupTitle: "OK Computer",
		}, true
	default:
		return nil, false
	}
}

type artistSearchRow struct {
	ID             string
	Name           string
	Disambiguation string
	Country        string
	Type           string
	Score          int32
}

type releaseGroupRow struct {
	ID          string
	Title       string
	PrimaryType string
	Year        int32
}

type artistDetail struct {
	ID             string
	Name           string
	Disambiguation string
	Country        string
	Type           string
	ReleaseGroups  []releaseGroupRow
}
