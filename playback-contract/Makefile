.PHONY: proto
proto:
	PATH="$(HOME)/go/bin:$$PATH" protoc --go_out=. --go_opt=paths=source_relative \
		proto/playbackv1/session.proto \
		proto/playbackv1/query.proto
