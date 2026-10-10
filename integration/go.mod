module github.com/Muxcore-Media/muxcore/integration

go 1.26.6

require (
	github.com/Muxcore-Media/backup-local v0.1.6
	github.com/Muxcore-Media/core v0.6.17
	github.com/Muxcore-Media/core/sdk/go/module v0.6.7
	github.com/Muxcore-Media/media-movies v0.1.18
	github.com/Muxcore-Media/media-transcoder v0.3.9
	github.com/Muxcore-Media/playback-monitor v0.1.7
	google.golang.org/grpc v1.83.2
	modernc.org/sqlite v1.55.0
)

require (
	github.com/Muxcore-Media/auth-local v0.1.20 // indirect
	github.com/Muxcore-Media/contracts-media-admin v0.1.1 // indirect
	github.com/Muxcore-Media/contracts-notification v0.1.2 // indirect
	github.com/Muxcore-Media/contracts-playback v0.2.0 // indirect
	github.com/Muxcore-Media/contracts-reconciler v0.1.0 // indirect
	github.com/Muxcore-Media/core/pkg/contracts v0.6.0 // indirect
	github.com/Muxcore-Media/core/sdk/go/client v0.6.1 // indirect
	github.com/Muxcore-Media/media-automation v0.1.49 // indirect
	github.com/Muxcore-Media/media-root-folders v0.1.9 // indirect
	github.com/Muxcore-Media/media-transcoder-pool v0.2.3 // indirect
	github.com/Muxcore-Media/metadata-tmdb v0.1.7 // indirect
	github.com/boombuler/barcode v1.0.1-0.20190219062509-6c824513bacc // indirect
	github.com/dustin/go-humanize v1.0.1 // indirect
	github.com/fxamacker/cbor/v2 v2.9.2 // indirect
	github.com/go-viper/mapstructure/v2 v2.5.0 // indirect
	github.com/go-webauthn/webauthn v0.17.4 // indirect
	github.com/go-webauthn/x v0.2.6 // indirect
	github.com/golang-jwt/jwt/v5 v5.3.1 // indirect
	github.com/google/go-tpm v0.9.8 // indirect
	github.com/google/uuid v1.6.0 // indirect
	github.com/jackc/pgpassfile v1.0.0 // indirect
	github.com/jackc/pgservicefile v0.0.0-20240606120523-5a60cdf6a761 // indirect
	github.com/jackc/pgx/v5 v5.7.5 // indirect
	github.com/jackc/puddle/v2 v2.2.2 // indirect
	github.com/kr/text v0.2.0 // indirect
	github.com/mattn/go-isatty v0.0.20 // indirect
	github.com/ncruces/go-strftime v1.0.0 // indirect
	github.com/oschwald/geoip2-golang v1.11.0 // indirect
	github.com/oschwald/maxminddb-golang v1.13.0 // indirect
	github.com/philhofer/fwd v1.2.0 // indirect
	github.com/pquerna/otp v1.5.0 // indirect
	github.com/remyoudompheng/bigfft v0.0.0-20230129092748-24d4a6f8daec // indirect
	github.com/robfig/cron/v3 v3.0.1 // indirect
	github.com/rogpeppe/go-internal v1.16.0 // indirect
	github.com/tinylib/msgp v1.6.4 // indirect
	github.com/x448/float16 v0.8.4 // indirect
	golang.org/x/crypto v0.56.0 // indirect
	golang.org/x/net v0.58.0 // indirect
	golang.org/x/sync v0.22.0 // indirect
	golang.org/x/sys v0.47.0 // indirect
	golang.org/x/text v0.41.0 // indirect
	google.golang.org/genproto/googleapis/rpc v0.0.0-20260610212136-7ab31c22f7ad // indirect
	google.golang.org/protobuf v1.36.11 // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
	modernc.org/libc v1.74.1 // indirect
	modernc.org/mathutil v1.7.1 // indirect
	modernc.org/memory v1.11.0 // indirect
)

// Built by erasure_catchup_test.go (ADR-0035 E11): a real muxcored and auth-local
// over mesh TLS. Listed as tools so go mod tidy keeps their dependencies.
tool (
	github.com/Muxcore-Media/auth-local/cmd/module
	github.com/Muxcore-Media/core/cmd/muxcored
)
