#!/bin/sh
# Gradle wrapper script
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_DIR="$GRADLE_USER_HOME/wrapper/dists"
WRAPPER_ZIP="$WRAPPER_DIR/gradle-8.2-bin.zip"
WRAPPER_JAR=""

# Find the extracted wrapper jar
for dir in "$WRAPPER_DIR"/*/; do
    jar=$(find "$dir" -name "gradle-wrapper.jar" 2>/dev/null | head -1)
    if [ -n "$jar" ]; then
        WRAPPER_JAR="$jar"
        break
    fi
done

# Download and extract Gradle if not found
if [ -z "$WRAPPER_JAR" ]; then
    echo "Downloading Gradle 8.2..."
    mkdir -p "$WRAPPER_DIR"
    curl -L -s -o "$WRAPPER_ZIP" "https://services.gradle.org/distributions/gradle-8.2-bin.zip"
    EXTRACT_DIR="$WRAPPER_DIR/gradle-8.2"
    mkdir -p "$EXTRACT_DIR"
    unzip -q -o "$WRAPPER_ZIP" -d "$EXTRACT_DIR"
    WRAPPER_JAR=$(find "$EXTRACT_DIR" -name "gradle-wrapper.jar" | head -1)
    rm -f "$WRAPPER_ZIP"
fi

exec java -jar "$WRAPPER_JAR" "$@"
