package internal

import (
	"context"
	"net/url"
	"strconv"
	"strings"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	musicbrainzv1 "github.com/Muxcore-Media/metadata-musicbrainz/proto/musicbrainzv1"
)

func (m *Module) SearchReleaseGroup(ctx context.Context, req *musicbrainzv1.SearchReleaseGroupRequest) (*musicbrainzv1.SearchReleaseGroupResponse, error) {
	q := strings.TrimSpace(req.GetQuery())
	if q == "" {
		return &musicbrainzv1.SearchReleaseGroupResponse{}, nil
	}
	limit := int(req.GetLimit())
	if limit <= 0 {
		limit = 10
	}
	if m.fixtureMode() {
		return toSearchReleaseGroupResponse(fixtureSearchReleaseGroups(q, limit)), nil
	}
	rows, err := m.searchReleaseGroupHTTP(ctx, q, limit)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "search release group: %v", err)
	}
	return toSearchReleaseGroupResponse(rows), nil
}

func (m *Module) GetReleaseGroup(ctx context.Context, req *musicbrainzv1.GetReleaseGroupRequest) (*musicbrainzv1.GetReleaseGroupResponse, error) {
	mbid := strings.TrimSpace(req.GetMusicbrainzId())
	if mbid == "" {
		return nil, status.Error(codes.InvalidArgument, "musicbrainz_id required")
	}
	if m.fixtureMode() {
		detail, ok := fixtureGetReleaseGroup(mbid)
		if !ok {
			return nil, status.Errorf(codes.NotFound, "release group %s not found in fixture", mbid)
		}
		return toGetReleaseGroupResponse(detail), nil
	}
	detail, err := m.getReleaseGroupHTTP(ctx, mbid)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "get release group: %v", err)
	}
	return toGetReleaseGroupResponse(detail), nil
}

func (m *Module) SearchRecording(ctx context.Context, req *musicbrainzv1.SearchRecordingRequest) (*musicbrainzv1.SearchRecordingResponse, error) {
	q := strings.TrimSpace(req.GetQuery())
	if q == "" {
		return &musicbrainzv1.SearchRecordingResponse{}, nil
	}
	limit := int(req.GetLimit())
	if limit <= 0 {
		limit = 10
	}
	if m.fixtureMode() {
		return toSearchRecordingResponse(fixtureSearchRecordings(q, limit)), nil
	}
	rows, err := m.searchRecordingHTTP(ctx, q, limit)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "search recording: %v", err)
	}
	return toSearchRecordingResponse(rows), nil
}

func (m *Module) GetRecording(ctx context.Context, req *musicbrainzv1.GetRecordingRequest) (*musicbrainzv1.GetRecordingResponse, error) {
	mbid := strings.TrimSpace(req.GetMusicbrainzId())
	if mbid == "" {
		return nil, status.Error(codes.InvalidArgument, "musicbrainz_id required")
	}
	if m.fixtureMode() {
		detail, ok := fixtureGetRecording(mbid)
		if !ok {
			return nil, status.Errorf(codes.NotFound, "recording %s not found in fixture", mbid)
		}
		return toGetRecordingResponse(detail), nil
	}
	detail, err := m.getRecordingHTTP(ctx, mbid)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "get recording: %v", err)
	}
	return toGetRecordingResponse(detail), nil
}

func toSearchReleaseGroupResponse(rows []releaseGroupSearchRow) *musicbrainzv1.SearchReleaseGroupResponse {
	out := make([]*musicbrainzv1.ReleaseGroupSearchResult, 0, len(rows))
	for _, r := range rows {
		out = append(out, &musicbrainzv1.ReleaseGroupSearchResult{
			Id: r.ID, Title: r.Title, ArtistId: r.ArtistID, ArtistName: r.ArtistName,
			PrimaryType: r.PrimaryType, FirstReleaseDateYear: r.Year, Score: r.Score,
		})
	}
	return &musicbrainzv1.SearchReleaseGroupResponse{ReleaseGroups: out}
}

func toGetReleaseGroupResponse(d *releaseGroupDetail) *musicbrainzv1.GetReleaseGroupResponse {
	if d == nil {
		return &musicbrainzv1.GetReleaseGroupResponse{}
	}
	return &musicbrainzv1.GetReleaseGroupResponse{
		Id: d.ID, Title: d.Title, ArtistId: d.ArtistID, ArtistName: d.ArtistName,
		PrimaryType: d.PrimaryType, FirstReleaseDateYear: d.Year,
	}
}

func toSearchRecordingResponse(rows []recordingSearchRow) *musicbrainzv1.SearchRecordingResponse {
	out := make([]*musicbrainzv1.RecordingSearchResult, 0, len(rows))
	for _, r := range rows {
		out = append(out, &musicbrainzv1.RecordingSearchResult{
			Id: r.ID, Title: r.Title, ArtistId: r.ArtistID, ArtistName: r.ArtistName,
			ReleaseGroupId: r.ReleaseGroupID, ReleaseGroupTitle: r.ReleaseGroupTitle, Score: r.Score,
		})
	}
	return &musicbrainzv1.SearchRecordingResponse{Recordings: out}
}

func toGetRecordingResponse(d *recordingDetail) *musicbrainzv1.GetRecordingResponse {
	if d == nil {
		return &musicbrainzv1.GetRecordingResponse{}
	}
	return &musicbrainzv1.GetRecordingResponse{
		Id: d.ID, Title: d.Title, ArtistId: d.ArtistID, ArtistName: d.ArtistName,
		ReleaseGroupId: d.ReleaseGroupID, ReleaseGroupTitle: d.ReleaseGroupTitle,
	}
}

type releaseGroupSearchRow struct {
	ID          string
	Title       string
	ArtistID    string
	ArtistName  string
	PrimaryType string
	Year        int32
	Score       int32
}

type releaseGroupDetail struct {
	ID          string
	Title       string
	ArtistID    string
	ArtistName  string
	PrimaryType string
	Year        int32
}

type recordingSearchRow struct {
	ID                string
	Title             string
	ArtistID          string
	ArtistName        string
	ReleaseGroupID    string
	ReleaseGroupTitle string
	Score             int32
}

type recordingDetail struct {
	ID                string
	Title             string
	ArtistID          string
	ArtistName        string
	ReleaseGroupID    string
	ReleaseGroupTitle string
}

type mbReleaseGroupSearchResponse struct {
	ReleaseGroups []struct {
		ID               string `json:"id"`
		Title            string `json:"title"`
		PrimaryType      string `json:"primary-type"`
		FirstReleaseDate string `json:"first-release-date"`
		Score            int    `json:"score"`
		ArtistCredit     []struct {
			Artist struct {
				ID   string `json:"id"`
				Name string `json:"name"`
			} `json:"artist"`
		} `json:"artist-credit"`
	} `json:"release-groups"`
}

type mbReleaseGroupDetailResponse struct {
	ID               string `json:"id"`
	Title            string `json:"title"`
	PrimaryType      string `json:"primary-type"`
	FirstReleaseDate string `json:"first-release-date"`
	ArtistCredit     []struct {
		Artist struct {
			ID   string `json:"id"`
			Name string `json:"name"`
		} `json:"artist"`
	} `json:"artist-credit"`
}

type mbRecordingSearchResponse struct {
	Recordings []struct {
		ID           string `json:"id"`
		Title        string `json:"title"`
		Score        int    `json:"score"`
		ArtistCredit []struct {
			Artist struct {
				ID   string `json:"id"`
				Name string `json:"name"`
			} `json:"artist"`
		} `json:"artist-credit"`
		Releases []struct {
			ReleaseGroup struct {
				ID    string `json:"id"`
				Title string `json:"title"`
			} `json:"release-group"`
		} `json:"releases"`
	} `json:"recordings"`
}

type mbRecordingDetailResponse struct {
	ID           string `json:"id"`
	Title        string `json:"title"`
	ArtistCredit []struct {
		Artist struct {
			ID   string `json:"id"`
			Name string `json:"name"`
		} `json:"artist"`
	} `json:"artist-credit"`
	Releases []struct {
		ReleaseGroup struct {
			ID    string `json:"id"`
			Title string `json:"title"`
		} `json:"release-group"`
	} `json:"releases"`
}

func (m *Module) searchReleaseGroupHTTP(ctx context.Context, query string, limit int) ([]releaseGroupSearchRow, error) {
	params := url.Values{}
	params.Set("query", query)
	params.Set("fmt", "json")
	params.Set("limit", strconv.Itoa(limit))
	var resp mbReleaseGroupSearchResponse
	if err := m.getJSON(ctx, "/release-group", params, &resp); err != nil {
		return nil, err
	}
	out := make([]releaseGroupSearchRow, 0, len(resp.ReleaseGroups))
	for _, rg := range resp.ReleaseGroups {
		artistID, artistName := firstArtistCredit(rg.ArtistCredit)
		out = append(out, releaseGroupSearchRow{
			ID: rg.ID, Title: rg.Title, ArtistID: artistID, ArtistName: artistName,
			PrimaryType: rg.PrimaryType, Year: parseReleaseYear(rg.FirstReleaseDate),
			Score: int32(rg.Score),
		})
	}
	return out, nil
}

func (m *Module) getReleaseGroupHTTP(ctx context.Context, mbid string) (*releaseGroupDetail, error) {
	params := url.Values{}
	params.Set("inc", "artist-credits")
	params.Set("fmt", "json")
	var resp mbReleaseGroupDetailResponse
	if err := m.getJSON(ctx, "/release-group/"+url.PathEscape(mbid), params, &resp); err != nil {
		return nil, err
	}
	artistID, artistName := firstArtistCredit(resp.ArtistCredit)
	return &releaseGroupDetail{
		ID: resp.ID, Title: resp.Title, ArtistID: artistID, ArtistName: artistName,
		PrimaryType: resp.PrimaryType, Year: parseReleaseYear(resp.FirstReleaseDate),
	}, nil
}

func (m *Module) searchRecordingHTTP(ctx context.Context, query string, limit int) ([]recordingSearchRow, error) {
	params := url.Values{}
	params.Set("query", query)
	params.Set("fmt", "json")
	params.Set("limit", strconv.Itoa(limit))
	var resp mbRecordingSearchResponse
	if err := m.getJSON(ctx, "/recording", params, &resp); err != nil {
		return nil, err
	}
	out := make([]recordingSearchRow, 0, len(resp.Recordings))
	for _, rec := range resp.Recordings {
		artistID, artistName := firstArtistCredit(rec.ArtistCredit)
		rgID, rgTitle := firstReleaseGroup(rec.Releases)
		out = append(out, recordingSearchRow{
			ID: rec.ID, Title: rec.Title, ArtistID: artistID, ArtistName: artistName,
			ReleaseGroupID: rgID, ReleaseGroupTitle: rgTitle, Score: int32(rec.Score),
		})
	}
	return out, nil
}

func (m *Module) getRecordingHTTP(ctx context.Context, mbid string) (*recordingDetail, error) {
	params := url.Values{}
	params.Set("inc", "artist-credits+releases")
	params.Set("fmt", "json")
	var resp mbRecordingDetailResponse
	if err := m.getJSON(ctx, "/recording/"+url.PathEscape(mbid), params, &resp); err != nil {
		return nil, err
	}
	artistID, artistName := firstArtistCredit(resp.ArtistCredit)
	rgID, rgTitle := firstReleaseGroup(resp.Releases)
	return &recordingDetail{
		ID: resp.ID, Title: resp.Title, ArtistID: artistID, ArtistName: artistName,
		ReleaseGroupID: rgID, ReleaseGroupTitle: rgTitle,
	}, nil
}

func firstArtistCredit(credits []struct {
	Artist struct {
		ID   string `json:"id"`
		Name string `json:"name"`
	} `json:"artist"`
}) (id, name string) {
	for _, c := range credits {
		if strings.TrimSpace(c.Artist.Name) != "" {
			return c.Artist.ID, c.Artist.Name
		}
	}
	return "", ""
}

func firstReleaseGroup(releases []struct {
	ReleaseGroup struct {
		ID    string `json:"id"`
		Title string `json:"title"`
	} `json:"release-group"`
}) (id, title string) {
	for _, rel := range releases {
		if strings.TrimSpace(rel.ReleaseGroup.Title) != "" {
			return rel.ReleaseGroup.ID, rel.ReleaseGroup.Title
		}
	}
	return "", ""
}
