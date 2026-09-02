#!/usr/bin/env bash
# Generates version.json at the repository root.
#
# The file is what the in-app update check reads over GitHub raw, so it has to be
# committed on the default branch — a release asset would not be reachable through
# a raw mirror. Run from CI after a successful build; pass the release tag as $1.
set -euo pipefail

TAG="${1:-}"
GRADLE_FILE="app/build.gradle.kts"

version_name=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$GRADLE_FILE" | head -1)
version_code=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' "$GRADLE_FILE" | head -1)

if [ -z "$version_name" ] || [ -z "$version_code" ]; then
  echo "could not read versionName/versionCode from $GRADLE_FILE" >&2
  exit 1
fi

commit=$(git rev-parse HEAD)
short=$(git rev-parse --short HEAD)
built_at=$(TZ=Asia/Shanghai date +'%Y-%m-%d %H:%M:%S')

# Changelog entries: every commit since the previous version bump, so the app can
# show what actually changed rather than just a version number.
previous=$(git log -1 --skip=1 --format=%H --pickaxe-regex -S'versionCode = ' -- "$GRADLE_FILE" || true)
range_start="$previous"
if [ -z "$range_start" ]; then
  range_start=$(git rev-list --max-parents=0 HEAD | head -1)
fi

changes_json=$(
  git log --no-merges --format='%s%x1f%h%x1e' "${range_start}..HEAD" |
  python3 -c '
import json, sys
raw = sys.stdin.read()
items = []
for record in raw.split("\x1e"):
    record = record.strip()
    if not record:
        continue
    parts = record.split("\x1f")
    subject = parts[0].strip()
    sha = parts[1].strip() if len(parts) > 1 else ""
    if not subject:
        continue
    items.append({"summary": subject, "commit": sha})
print(json.dumps(items[:60], ensure_ascii=False))
'
)

if [ -z "$changes_json" ]; then
  changes_json="[]"
fi

apk_url=""
if [ -n "$TAG" ]; then
  apk_url="https://github.com/ImUpXuu/XUCMS/releases/download/${TAG}/XUCMS-release.apk"
fi

python3 - "$version_name" "$version_code" "$commit" "$short" "$built_at" "$TAG" "$apk_url" "$changes_json" <<'PY'
import json, sys

name, code, commit, short, built_at, tag, apk_url, changes = sys.argv[1:9]
payload = {
    "versionName": name,
    "versionCode": int(code),
    "commit": commit,
    "shortCommit": short,
    "builtAt": built_at,
    "tag": tag,
    "apkUrl": apk_url,
    "releaseUrl": f"https://github.com/ImUpXuu/XUCMS/releases/tag/{tag}" if tag else
                  "https://github.com/ImUpXuu/XUCMS/releases/latest",
    "changes": json.loads(changes),
}
with open("version.json", "w", encoding="utf-8") as handle:
    json.dump(payload, handle, ensure_ascii=False, indent=2)
    handle.write("\n")
print(json.dumps(payload, ensure_ascii=False, indent=2))
PY
