#!/usr/bin/env bash
set -euo pipefail

script="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)/format-baseline.sh"
temp_root="$(cd -- "${TMPDIR:-/tmp}" && pwd -P)"
fixture="$(mktemp -d "${temp_root%/}/format-baseline.XXXXXXXX")"
fixture="$(cd -- "$fixture" && pwd -P)"
case "$fixture" in
  "${temp_root%/}"/format-baseline.*) ;;
  *) echo "Unexpected test directory: $fixture" >&2; exit 1 ;;
esac
trap 'rm -rf -- "$fixture"' EXIT

configure_git() {
  local repository="$1"
  git -C "$repository" config user.name "Format baseline test"
  git -C "$repository" config user.email "format-baseline@example.invalid"
  git -C "$repository" config commit.gpgsign false
  git -C "$repository" config core.autocrlf false
}

commit_file() {
  local repository="$1" file="$2"
  printf '%s\n' "$file" > "$repository/$file" &&
    git -C "$repository" add -- "$file" &&
    git -C "$repository" commit -qm "Add $file" &&
    git -C "$repository" rev-parse HEAD
}

write_payload() {
  local commit separator=""
  {
    printf '{"commits":['
    for commit in "$@"; do
      printf '%s{"id":"%s"}' "$separator" "$commit"
      separator=","
    done
    printf ']}\n'
  } > "$fixture/event.json"
}

expect_baseline() {
  local label="$1" repository="$2" event="$3" before="$4" expected="$5" message="$6"
  shift 6
  local log
  write_payload "$@"
  : > "$fixture/output"
  if ! log="$(
    cd -- "$repository"
    FORMAT_BASE="$before" GITHUB_EVENT_NAME="$event" GITHUB_EVENT_PATH="$fixture/event.json" \
      GITHUB_OUTPUT="$fixture/output" bash "$script" 2>&1
  )"; then
    printf '%s failed:\n%s\n' "$label" "$log" >&2
    return 1
  fi
  if [[ "$(<"$fixture/output")" != "base=$expected" ]] ||
    [[ -n "$message" && "$log" != *"$message"* ]] ||
    [[ -z "$message" && "$log" == *"::warning::"* ]]; then
    printf '%s: expected base=%s and message %s\n%s\n%s\n' \
      "$label" "$expected" "$message" "$(<"$fixture/output")" "$log" >&2
    return 1
  fi
  printf '%s OK\n' "$label"
}

origin="$fixture/origin"
repo="$fixture/repo"
zero=0000000000000000000000000000000000000000
missing=1111111111111111111111111111111111111111
partial="Checking only the latest commit."

git init -q -b main "$origin"
configure_git "$origin"
root="$(commit_file "$origin" Root.java)"
expect_baseline "Root commit" "$origin" push "$zero" "" "Skipping incremental" "$root"
base="$(commit_file "$origin" Legacy.java)"

git clone -q --no-hardlinks "$origin" "$repo"
configure_git "$repo"
early="$(commit_file "$repo" Early.java)"
latest="$(commit_file "$repo" Latest.java)"
old_tip="$(commit_file "$origin" OldTip.java)"

expect_baseline "Normal multi-commit push" "$repo" push "$base" "$base" "" "$early" "$latest"
expect_baseline "Unavailable before" "$repo" push "$missing" "$base" "" "$latest" "$early"
expect_baseline "Fetchable before" "$repo" push "$old_tip" "$base" "" "$early" "$latest"
expect_baseline "All-zero before with payload" "$repo" push "$zero" "$base" "" "$early" "$latest"
expect_baseline "All-zero before without payload" "$repo" push "$zero" "$early" "$partial"
expect_baseline "Payload missing HEAD" "$repo" push "$missing" "$early" "$partial" "$early"
expect_baseline "Manual dispatch" "$repo" workflow_dispatch "" "$early" "Manual runs"
expect_baseline "Pull request" "$repo" pull_request "$old_tip" "$base" ""

capped=()
for ((i = 0; i < 2048; i++)); do
  capped+=("$latest")
done
expect_baseline "Capped payload" "$repo" push "$missing" "$early" "$partial" "${capped[@]}"

git -C "$repo" switch -q -c side "$base"
side="$(commit_file "$repo" Side.java)"
git -C "$repo" switch -q main
git -C "$repo" merge -q --no-ff -m "Merge side" side
merge="$(git -C "$repo" rev-parse HEAD)"

expect_baseline "Merge push" "$repo" push "$missing" "$base" "" "$merge" "$side" "$early" "$latest"
expect_baseline "Incomplete merge payload" "$repo" push "$missing" "$latest" "$partial" "$merge" "$early" "$latest"
expect_baseline "Unrelated commit in payload" "$repo" push "$missing" "$latest" "$partial" "$merge" "$old_tip" "$early" "$latest"
