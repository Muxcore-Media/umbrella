#!/usr/bin/env bash
# Non-interactive GitHub git credentials via gh auth. Never prompts.
# Requires: gh auth login (token in keyring or hosts.yml).
set -euo pipefail

export GH_PROMPT_DISABLED=1
export GIT_TERMINAL_PROMPT=0

exec gh auth git-credential "$@"
