FROM golang:1.26-alpine AS builder
COPY core/ /build/core/
COPY metadata-musicbrainz/ /build/metadata-musicbrainz/
WORKDIR /build/metadata-musicbrainz
RUN go mod download
RUN CGO_ENABLED=0 go build -o /metadata-musicbrainz ./cmd/module
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=builder /metadata-musicbrainz /
ENTRYPOINT ["/metadata-musicbrainz"]
