#!/usr/bin/env bash
set -euo pipefail
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is required. Use Gradle 8.x or import the project into IntelliJ IDEA." >&2
  exit 1
fi
gradle build --no-daemon
