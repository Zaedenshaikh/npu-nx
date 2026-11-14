#!/bin/sh
# Gradle wrapper script - downloads Gradle if not cached
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_VERSION=8.2
GRADLE_DIR="$GRADLE_USER_HOME/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    mkdir -p "$GRADLE_USER_HOME"
    TMPDIR=$(mktemp -d)
    cd "$TMPDIR"
    curl -L -s -o gradle.zip "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    unzip -q -o gradle.zip -d "$GRADLE_USER_HOME"
    cd "$APP_HOME"
    rm -rf "$TMPDIR"
fi

exec "$GRADLE_BIN" "$@"
