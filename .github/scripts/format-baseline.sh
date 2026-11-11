#!/usr/bin/env bash
set -euo pipefail

baseline_from_push() {
  python - "$GITHUB_EVENT_PATH" <<'PY'
import json
import subprocess
import sys

with open(sys.argv[1], encoding="utf-8") as payload:
    commits = [commit["id"] for commit in json.load(payload).get("commits", [])]

# A push payload at GitHub's 2048-commit cap may be incomplete.
if not 0 < len(commits) < 2048:
    sys.exit()

history = subprocess.check_output(
    ["git", "rev-list", "--first-parent", "-n", str(len(commits) + 1), "HEAD"],
    text=True,
).splitlines()
pushed = set(commits)
candidate = next((commit for commit in history if commit not in pushed), None)
if candidate:
    # Check the full range, including commits on merged branches.
    actual = subprocess.check_output(
        ["git", "rev-list", f"{candidate}..HEAD"], text=True
    ).splitlines()
    if sorted(actual) == sorted(commits):
        print(candidate)
PY
}

base="${FORMAT_BASE:-}"
if [[ -n "$base" && ! "$base" =~ ^0+$ ]]; then
  if ! git cat-file -e "${base}^{commit}" 2>/dev/null; then
    git fetch --no-tags origin "$base" || true
  fi
  base="$(git merge-base "$base" HEAD 2>/dev/null || true)"
else
  base=""
fi

if [[ -z "$base" && "$GITHUB_EVENT_NAME" == "push" ]]; then
  base="$(baseline_from_push)"
fi

if [[ -z "$base" ]]; then
  base="$(git rev-parse --verify HEAD^ 2>/dev/null || true)"
  if [[ -z "$base" ]]; then
    echo "::warning::Skipping incremental Java formatting because HEAD has no parent commit."
  elif [[ "$GITHUB_EVENT_NAME" == "workflow_dispatch" ]]; then
    echo "::notice::Manual runs check Java formatting in the latest commit."
  else
    echo "::warning::Could not determine the full formatting range. Checking only the latest commit."
  fi
fi
printf 'base=%s\n' "$base" >> "$GITHUB_OUTPUT"
