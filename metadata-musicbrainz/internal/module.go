package internal

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"sync"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/Muxcore-Media/core/pkg/contracts"
	musicbrainzv1 "github.com/Muxcore-Media/metadata-musicbrainz/proto/musicbrainzv1"
)

type Module struct {
	musicbrainzv1.UnimplementedMusicBrainzServiceServer

	mu       sync.RWMutex
	client   *http.Client
	userAgent string
	baseURL  string
	fixture  bool

	id       string
	grpcAddr string
	grpcSrv  *grpc.Server
	lis      net.Listener
}

type Config struct {
	ID        string
	GRPCAddr  string
	UserAgent string
	Timeout   time.Duration
	BaseURL   string
	Fixture   bool
}

func NewModule(cfg Config) *Module {
	if cfg.ID == "" {
		cfg.ID = "metadata-musicbrainz"
	}
	if cfg.GRPCAddr == "" {
		cfg.GRPCAddr = ":9413"
	}
	if cfg.Timeout == 0 {
		cfg.Timeout = 20 * time.Second
	}
	if cfg.BaseURL == "" {
		cfg.BaseURL = "https://musicbrainz.org/ws/2"
	}
	if cfg.UserAgent == "" {
		cfg.UserAgent = "MuxCore/0.1 (https://github.com/Muxcore-Media/metadata-musicbrainz)"
	}
	if v := os.Getenv("METADATA_GRPC_ADDR"); v != "" {
		cfg.GRPCAddr = v
	}
	if v := os.Getenv("MUSICBRAINZ_BASE_URL"); v != "" {
		cfg.BaseURL = v
	}
	if v := os.Getenv("MUSICBRAINZ_USER_AGENT"); v != "" {
		cfg.UserAgent = v
	}
	if v := os.Getenv("MUSICBRAINZ_FIXTURE"); v == "1" || strings.EqualFold(v, "true") {
		cfg.Fixture = true
	}
	return &Module{
		id:        cfg.ID,
		grpcAddr:  cfg.GRPCAddr,
		userAgent: cfg.UserAgent,
		baseURL:   strings.TrimRight(cfg.BaseURL, "/"),
		fixture:   cfg.Fixture,
		client:    &http.Client{Timeout: cfg.Timeout},
	}
}

func (m *Module) fixtureMode() bool {
	return m.fixture
}

func (m *Module) Info() contracts.ModuleInfo {
	return contracts.ModuleInfo{
		ID:             m.id,
		Name:           "Metadata MusicBrainz",
		Version:        "0.1.0",
		Roles:          []string{"metadata"},
		Description:    "MusicBrainz metadata provider for music artists and release groups",
		Author:         "MuxCore",
		Capabilities:   []string{"metadata.music", "metadata.musicbrainz", "settings"},
		MinCoreVersion: "0.4.0",
		HTTPAddr:       m.grpcAddr,
	}
}

func (m *Module) Init(ctx context.Context) error {
	lis, err := net.Listen("tcp", m.grpcAddr)
	if err != nil {
		return fmt.Errorf("listen %s: %w", m.grpcAddr, err)
	}
	m.lis = lis
	slog.Info("metadata-musicbrainz initialized", "addr", m.grpcAddr, "fixture", m.fixtureMode())
	return nil
}

func (m *Module) Start(ctx context.Context) error {
	m.grpcSrv = grpc.NewServer()
	musicbrainzv1.RegisterMusicBrainzServiceServer(m.grpcSrv, m)
	go func() {
		slog.Info("metadata-musicbrainz gRPC service started", "addr", m.grpcAddr)
		if err := m.grpcSrv.Serve(m.lis); err != nil {
			slog.Error("metadata-musicbrainz gRPC serve error", "error", err)
		}
	}()
	return nil
}

func (m *Module) Stop(ctx context.Context) error {
	if m.grpcSrv != nil {
		m.grpcSrv.GracefulStop()
	}
	if m.client != nil {
		m.client.CloseIdleConnections()
	}
	slog.Info("metadata-musicbrainz stopped")
	return nil
}

func (m *Module) Health(ctx context.Context) error {
	if m.fixtureMode() {
		return nil
	}
	return nil
}

func (m *Module) SearchArtist(ctx context.Context, req *musicbrainzv1.SearchArtistRequest) (*musicbrainzv1.SearchArtistResponse, error) {
	q := strings.TrimSpace(req.GetQuery())
	if q == "" {
		return &musicbrainzv1.SearchArtistResponse{}, nil
	}
	limit := int(req.GetLimit())
	if limit <= 0 {
		limit = 10
	}
	if m.fixtureMode() {
		return toSearchResponse(fixtureSearchArtists(q, limit)), nil
	}
	rows, err := m.searchArtistHTTP(ctx, q, limit)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "search artist: %v", err)
	}
	return toSearchResponse(rows), nil
}

func (m *Module) GetArtist(ctx context.Context, req *musicbrainzv1.GetArtistRequest) (*musicbrainzv1.GetArtistResponse, error) {
	mbid := strings.TrimSpace(req.GetMusicbrainzId())
	if mbid == "" {
		return nil, status.Error(codes.InvalidArgument, "musicbrainz_id required")
	}
	if m.fixtureMode() {
		detail, ok := fixtureGetArtist(mbid)
		if !ok {
			return nil, status.Errorf(codes.NotFound, "artist %s not found in fixture", mbid)
		}
		return toGetResponse(detail), nil
	}
	detail, err := m.getArtistHTTP(ctx, mbid)
	if err != nil {
		return nil, status.Errorf(codes.Internal, "get artist: %v", err)
	}
	return toGetResponse(detail), nil
}

func toSearchResponse(rows []artistSearchRow) *musicbrainzv1.SearchArtistResponse {
	out := make([]*musicbrainzv1.ArtistSearchResult, 0, len(rows))
	for _, r := range rows {
		out = append(out, &musicbrainzv1.ArtistSearchResult{
			Id: r.ID, Name: r.Name, Disambiguation: r.Disambiguation,
			Country: r.Country, Type: r.Type, Score: r.Score,
		})
	}
	return &musicbrainzv1.SearchArtistResponse{Artists: out}
}

func toGetResponse(d *artistDetail) *musicbrainzv1.GetArtistResponse {
	if d == nil {
		return &musicbrainzv1.GetArtistResponse{}
	}
	rgs := make([]*musicbrainzv1.ReleaseGroup, 0, len(d.ReleaseGroups))
	for _, rg := range d.ReleaseGroups {
		rgs = append(rgs, &musicbrainzv1.ReleaseGroup{
			Id: rg.ID, Title: rg.Title, PrimaryType: rg.PrimaryType,
			FirstReleaseDateYear: rg.Year,
		})
	}
	return &musicbrainzv1.GetArtistResponse{
		Id: d.ID, Name: d.Name, Disambiguation: d.Disambiguation,
		Country: d.Country, Type: d.Type, ReleaseGroups: rgs,
	}
}

type mbArtistSearchResponse struct {
	Artists []struct {
		ID             string `json:"id"`
		Name           string `json:"name"`
		Disambiguation string `json:"disambiguation"`
		Country        string `json:"country"`
		Type           string `json:"type"`
		Score          int    `json:"score"`
	} `json:"artists"`
}

type mbArtistDetailResponse struct {
	ID             string `json:"id"`
	Name           string `json:"name"`
	Disambiguation string `json:"disambiguation"`
	Country        string `json:"country"`
	Type           string `json:"type"`
	ReleaseGroups  []struct {
		ID             string `json:"id"`
		Title          string `json:"title"`
		PrimaryType    string `json:"primary-type"`
		FirstReleaseDate string `json:"first-release-date"`
	} `json:"release-groups"`
}

func (m *Module) searchArtistHTTP(ctx context.Context, query string, limit int) ([]artistSearchRow, error) {
	params := url.Values{}
	params.Set("query", query)
	params.Set("fmt", "json")
	params.Set("limit", strconv.Itoa(limit))
	var resp mbArtistSearchResponse
	if err := m.getJSON(ctx, "/artist", params, &resp); err != nil {
		return nil, err
	}
	out := make([]artistSearchRow, 0, len(resp.Artists))
	for _, a := range resp.Artists {
		out = append(out, artistSearchRow{
			ID: a.ID, Name: a.Name, Disambiguation: a.Disambiguation,
			Country: a.Country, Type: a.Type, Score: int32(a.Score),
		})
	}
	return out, nil
}

func (m *Module) getArtistHTTP(ctx context.Context, mbid string) (*artistDetail, error) {
	params := url.Values{}
	params.Set("inc", "release-groups")
	params.Set("fmt", "json")
	var resp mbArtistDetailResponse
	if err := m.getJSON(ctx, "/artist/"+url.PathEscape(mbid), params, &resp); err != nil {
		return nil, err
	}
	detail := &artistDetail{
		ID: resp.ID, Name: resp.Name, Disambiguation: resp.Disambiguation,
		Country: resp.Country, Type: resp.Type,
	}
	for _, rg := range resp.ReleaseGroups {
		if strings.EqualFold(rg.PrimaryType, "Compilation") {
			continue
		}
		detail.ReleaseGroups = append(detail.ReleaseGroups, releaseGroupRow{
			ID: rg.ID, Title: rg.Title, PrimaryType: rg.PrimaryType,
			Year: parseReleaseYear(rg.FirstReleaseDate),
		})
	}
	return detail, nil
}

func parseReleaseYear(raw string) int32 {
	raw = strings.TrimSpace(raw)
	if len(raw) >= 4 {
		if y, err := strconv.Atoi(raw[:4]); err == nil {
			return int32(y)
		}
	}
	return 0
}

func (m *Module) getJSON(ctx context.Context, path string, params url.Values, dest any) error {
	u := m.baseURL + path
	if len(params) > 0 {
		u += "?" + params.Encode()
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return err
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", m.userAgent)
	resp, err := m.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(io.LimitReader(resp.Body, 8<<20))
	if err != nil {
		return err
	}
	if resp.StatusCode != http.StatusOK {
		snip := string(body)
		if len(snip) > 200 {
			snip = snip[:200]
		}
		return fmt.Errorf("musicbrainz HTTP %d: %s", resp.StatusCode, snip)
	}
	if err := json.Unmarshal(body, dest); err != nil {
		return fmt.Errorf("decode musicbrainz JSON: %w", err)
	}
	time.Sleep(1100 * time.Millisecond)
	return nil
}

var _ contracts.Module = (*Module)(nil)
