#!/usr/bin/env bash
# CodeClear launcher (bash / Git Bash). Bootstrap only: all behavior lives in the backend.
# Env: CODECLEAR_PORT (default 8080), CODECLEAR_JAR (explicit jar path), CODECLEAR_NO_BROWSER=1
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${CODECLEAR_PORT:-8080}"
REQUIRED_JAVA=25

# 1. Locate the prebuilt jar (next to this script, or the Maven output for development).
JAR="${CODECLEAR_JAR:-}"
if [[ -z "$JAR" ]]; then
  for c in "$DIR/codeclear.jar" "$DIR/target/codeclear.jar"; do
    [[ -f "$c" ]] && JAR="$c" && break
  done
fi
[[ -n "$JAR" && -f "$JAR" ]] || { echo "error: codeclear.jar not found next to $0 (set CODECLEAR_JAR to override)" >&2; exit 1; }

# 2. Java check.
command -v java >/dev/null 2>&1 || { echo "error: java not found on PATH; Java $REQUIRED_JAVA or newer is required" >&2; exit 1; }
JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt "$REQUIRED_JAVA" ]]; then
  echo "error: Java $REQUIRED_JAVA or newer is required (found: ${JAVA_MAJOR:-unknown})" >&2; exit 1
fi

# 3. Copilot CLI is optional (only needed for the Copilot provider), so warn rather than fail.
if ! command -v copilot.exe >/dev/null 2>&1 && ! command -v copilot >/dev/null 2>&1; then
  echo "warning: copilot.exe not found on PATH; the Copilot provider will be unavailable" >&2
fi

# 4. Open the browser once the backend reports healthy (background, best effort).
open_browser() {
  local url="http://127.0.0.1:$PORT/"
  for _ in $(seq 1 60); do
    if curl -fs "http://127.0.0.1:$PORT/api/health" >/dev/null 2>&1; then
      case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*) start "" "$url" ;;
        Darwin) open "$url" ;;
        *) xdg-open "$url" >/dev/null 2>&1 || true ;;
      esac
      return
    fi
    sleep 1
  done
}
if [[ "${CODECLEAR_NO_BROWSER:-}" != "1" ]]; then open_browser & fi

echo "Starting CodeClear on http://127.0.0.1:$PORT/ (Ctrl+C to stop)"
exec java -jar "$JAR" --server.port="$PORT" "$@"
