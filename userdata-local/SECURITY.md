# Security

- Userdata files are mode `0600` under the configured data directory.
- Scope by authenticated user id (and tenant id when `TENANT_MODE=1`).
- Do not expose the userdata HTTP port publicly without auth mesh in front.
