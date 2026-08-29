ARG GO_VERSION=1.26
FROM golang:${GO_VERSION}-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /out/module ./cmd/module

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
COPY --from=build /out/module /usr/local/bin/module
ENTRYPOINT ["/usr/local/bin/module"]
