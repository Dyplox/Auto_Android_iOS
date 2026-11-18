#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Uploads the demo app to the cloud provider's storage.
#
# Usage: ./scripts/upload-app.sh browserstack|saucelabs [version]   (default version 2.2.0)
#
# Credentials come from environment variables or the .env file:
#   BrowserStack: BROWSERSTACK_USERNAME, BROWSERSTACK_ACCESS_KEY
#   Sauce Labs:   SAUCE_USERNAME, SAUCE_ACCESS_KEY, SAUCE_REGION (optional, us-west-1)
#
# The names match app.path in config/env/<provider>-<platform>.properties:
#   BrowserStack -> custom_id "WdioDemoApp-Android" (always points to the latest upload)
#   Sauce Labs   -> storage:filename=<file name>
# -----------------------------------------------------------------------------
set -euo pipefail

PROVIDER="${1:-}"
VERSION="${2:-2.2.0}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APPS_DIR="$ROOT_DIR/apps"
ANDROID_APP="$APPS_DIR/android.wdio.native.app.v${VERSION}.apk"
IOS_APP="$APPS_DIR/ios.simulator.wdio.native.app.v${VERSION}.zip"

# Load .env variables without overriding those already set in the environment
if [[ -f "$ROOT_DIR/.env" ]]; then
  while IFS='=' read -r key value; do
    key="${key#export }"; key="$(echo "$key" | xargs)"
    [[ -z "$key" || "$key" == \#* ]] && continue
    value="$(echo "$value" | xargs)"
    [[ -z "${!key:-}" ]] && export "$key=$value"
  done < "$ROOT_DIR/.env"
fi

require() {
  for name in "$@"; do
    if [[ -z "${!name:-}" ]]; then
      echo "Missing variable $name (define it in the environment or in .env)" >&2
      exit 1
    fi
  done
}

require_file() {
  if [[ ! -s "$1" ]]; then
    echo "$1 does not exist. Run ./scripts/download-apps.sh $VERSION first" >&2
    exit 1
  fi
}

case "$PROVIDER" in
  browserstack)
    require BROWSERSTACK_USERNAME BROWSERSTACK_ACCESS_KEY
    require_file "$ANDROID_APP"
    echo "Uploading $(basename "$ANDROID_APP") to BrowserStack (custom_id WdioDemoApp-Android)..."
    curl -fsS -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
      -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
      -F "file=@$ANDROID_APP" \
      -F 'data={"custom_id": "WdioDemoApp-Android"}'
    echo
    echo "iOS: BrowserStack only offers real iPhones (signed .ipa) and the demo app only has a simulator build."
    ;;
  saucelabs)
    require SAUCE_USERNAME SAUCE_ACCESS_KEY
    REGION="${SAUCE_REGION:-us-west-1}"
    for app in "$ANDROID_APP" "$IOS_APP"; do
      require_file "$app"
      echo "Uploading $(basename "$app") to Sauce Labs ($REGION)..."
      curl -fsS -u "$SAUCE_USERNAME:$SAUCE_ACCESS_KEY" \
        -X POST "https://api.${REGION}.saucelabs.com/v1/storage/upload" \
        -F "payload=@$app" \
        -F "name=$(basename "$app")"
      echo
    done
    ;;
  *)
    echo "Usage: $0 browserstack|saucelabs [version]" >&2
    exit 1
    ;;
esac
