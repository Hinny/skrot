#!/usr/bin/env bash
#
# Writes a release version into app/build.gradle.kts.
#
# versionName becomes the tag (without its leading "v") and versionCode is
# bumped by one, which is exactly how the bump was done by hand before. The
# script is idempotent: asked for a version the file already carries it changes
# nothing, so re-running a release does not bump versionCode a second time.
#
# Usage: set-version.sh <MAJOR.MINOR.PATCH> [gradle-file]

set -euo pipefail

version_name="${1:?usage: set-version.sh <MAJOR.MINOR.PATCH> [gradle-file]}"
gradle_file="${2:-app/build.gradle.kts}"

if [[ ! "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "error: '$version_name' is not MAJOR.MINOR.PATCH" >&2
  exit 1
fi

current_name=$(sed -n 's/^[[:space:]]*versionName = "\(.*\)".*/\1/p' "$gradle_file")
current_code=$(sed -n 's/^[[:space:]]*versionCode = \([0-9]\{1,\}\).*/\1/p' "$gradle_file")

if [ -z "$current_name" ] || [ -z "$current_code" ]; then
  echo "error: no versionName/versionCode found in $gradle_file" >&2
  exit 1
fi

if [ "$current_name" = "$version_name" ]; then
  echo "$gradle_file is already at $version_name (versionCode $current_code)"
  exit 0
fi

next_code=$((current_code + 1))
sed -i "s/^\([[:space:]]*\)versionCode = .*/\1versionCode = $next_code/" "$gradle_file"
sed -i "s/^\([[:space:]]*\)versionName = \".*\"/\1versionName = \"$version_name\"/" "$gradle_file"

echo "$gradle_file: $current_name -> $version_name (versionCode $current_code -> $next_code)"
