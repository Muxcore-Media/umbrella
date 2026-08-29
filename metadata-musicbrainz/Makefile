.PHONY: build test proto tidy

GO ?= go
BINARY ?= metadata-musicbrainz

proto:
	PATH="$(HOME)/go/bin:$$PATH" protoc --go_out=. --go_opt=module=github.com/Muxcore-Media/metadata-musicbrainz \
		--go-grpc_out=. --go-grpc_opt=module=github.com/Muxcore-Media/metadata-musicbrainz \
		proto/musicbrainzv1/musicbrainz.proto

build:
	$(GO) build -o $(BINARY) ./cmd/module

test:
	$(GO) test -count=1 ./...

tidy:
	$(GO) mod tidy
