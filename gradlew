#!/usr/bin/env sh
# Small bootstrap wrapper for this source package; downloads Gradle 8.7 if not present.
set -eu
VERSION=8.7
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/aurora-bootstrap"
GRADLE_HOME="$BASE/gradle-$VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 2 "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  else
    echo "Please install curl or wget to download Gradle $VERSION." >&2; exit 1
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Please install unzip." >&2; exit 1; }
  unzip -oq "$ZIP" -d "$BASE"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
