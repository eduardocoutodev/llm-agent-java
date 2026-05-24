#!/bin/sh

set -e

(
  cd "$(dirname "$0")"
  mvn -q -B package -Ddir=/tmp/llm-agent-build
)

exec java --enable-native-access=ALL-UNNAMED --enable-preview -jar /tmp/llm-agent-build/llm-agent.jar "$@"
