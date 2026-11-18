#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Downloads the latest SonarQube Cloud results to build/ so they can be reviewed
# locally (issues, security hotspots and the main measures).
#
# Usage: ./scripts/sonar-report.sh
# Token: SONAR_TOKEN from the environment or the .env file (never committed).
# -----------------------------------------------------------------------------
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_KEY="${SONAR_PROJECT_KEY:-Dyplox_Auto_Android_iOS}"
OUT_DIR="$ROOT_DIR/build"

if [[ -z "${SONAR_TOKEN:-}" && -f "$ROOT_DIR/.env" ]]; then
  SONAR_TOKEN="$(grep -E '^(export )?SONAR_TOKEN=' "$ROOT_DIR/.env" | tail -1 | cut -d= -f2- | tr -d "\"' ")"
fi
if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "Missing SONAR_TOKEN: add SONAR_TOKEN=<token> to .env or export it" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
failed=0
download() {
  local name="$1" path="$2" code
  code=$(curl -s -u "$SONAR_TOKEN:" -o "$OUT_DIR/sonar-$name.json" -w '%{http_code}' "https://sonarcloud.io/$path")
  echo "$name -> HTTP $code ($OUT_DIR/sonar-$name.json)"
  [[ "$code" == "200" ]] || failed=1
}

download issues   "api/issues/search?componentKeys=$PROJECT_KEY&resolved=false&ps=500"
download hotspots "api/hotspots/search?projectKey=$PROJECT_KEY&ps=500"
download measures "api/measures/component?component=$PROJECT_KEY&metricKeys=alert_status,bugs,vulnerabilities,code_smells,security_hotspots,coverage,duplicated_lines_density,ncloc"

if [[ "$failed" -ne 0 ]]; then
  echo "Some downloads failed: 401 = invalid token, 403 = no permission, 404 = wrong project key" >&2
  exit 1
fi
