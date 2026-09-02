#!/usr/bin/env bash
# Source before agent git/ssh operations:  source scripts/muxcore-git-env.sh
export GIT_TERMINAL_PROMPT=0
export GH_PROMPT_DISABLED=1
export GIT_SSH_COMMAND='ssh -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o IdentitiesOnly=yes'
