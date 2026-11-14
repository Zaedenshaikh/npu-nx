#!/bin/sh
# Gradle wrapper script
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

# First, look for wrapper jar in the repo's gradle/wrapper directory
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# If not found, try the Gradle user home
if [ ! -f "$WRAPPER_JAR" ]; then
    WRAPPER_DIR="$GRADLE_USER_HOME/wrapper/dists"
    for dir in "$WRAPPER_DIR"/*/; do
        jar=$(find "$dir" -name "gradle-wrapper.jar" 2>/dev/null | head -1)
        if [ -n "$jar" ]; then
            WRAPPER_JAR="$jar"
            break
        fi
    done
fi

# Download and extract Gradle if still not found
if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle 8.2..."
    WRAPPER_DIR="$GRADLE_USER_HOME/wrapper/dists"
    mkdir -p "$WRAPPER_DIR"
    WRAPPER_ZIP="$WRAPPER_DIR/gradle-8.2-bin.zip"
    curl -L -s -o "$WRAPPER_ZIP" "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
    EXTRACT_DIR="$WRAPPER_DIR/gradle-8.2"
    mkdir -p "$EXTRACT_DIR"
    unzip -q -o "$WRAPPER_ZIP" -d "$EXTRACT_DIR"
    WRAPPER_JAR=$(find "$EXTRACT_DIR" -name "gradle-wrapper.jar" | head -1)
    rm -f "$WRAPPER_ZIP"
fi

exec java -jar "$WRAPPER_JAR" "$@"
