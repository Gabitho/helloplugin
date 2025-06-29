#!/usr/bin/env sh

# ------------------------------------------------------------------------------
# Gradle start-up script for UN*X
# ------------------------------------------------------------------------------

# Determine the directory of the script
DIRNAME=$(dirname "$0")
APP_BASE_NAME=$(basename "$0")

# Locate the wrapper jar
WRAPPER_JAR="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"

# Execute the wrapper jar
exec java -jar "$WRAPPER_JAR" "$@"
