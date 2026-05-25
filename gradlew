#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="8.10.2"
DIST_DIR="${HOME}/.gradle/manual-dists/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${DIST_DIR}/gradle-${GRADLE_VERSION}/bin/gradle"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

mkdir -p "$DIST_DIR"
if [ ! -x "$GRADLE_BIN" ]; then
  ZIP_PATH="${DIST_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -L --retry 3 --output "$ZIP_PATH" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_PATH" "$URL"
  else
    echo "curl or wget is required to download Gradle ${GRADLE_VERSION}." >&2
    exit 1
  fi
  unzip -q -o "$ZIP_PATH" -d "$DIST_DIR"
fi

exec "$GRADLE_BIN" "$@"
