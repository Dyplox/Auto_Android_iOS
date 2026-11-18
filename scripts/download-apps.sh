#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Downloads the demo app (WebdriverIO Native Demo App, MIT) for Android and the iOS Simulator.
# The same app (React Native) exists on both platforms: ideal for a framework
# where the only thing that changes is the locators.
#
# Usage: ./scripts/download-apps.sh [version]     (default 2.2.0)
# If you change the version, update app.path in src/test/resources/config/*.properties
# -----------------------------------------------------------------------------
set -euo pipefail

VERSION="${1:-2.2.0}"
BASE_URL="https://github.com/webdriverio/native-demo-app/releases/download/v${VERSION}"
APPS_DIR="$(cd "$(dirname "$0")/.." && pwd)/apps"

mkdir -p "$APPS_DIR"

for file in "android.wdio.native.app.v${VERSION}.apk" "ios.simulator.wdio.native.app.v${VERSION}.zip"; do
  if [[ -s "$APPS_DIR/$file" ]]; then
    echo "OK   $file already exists"
    continue
  fi
  echo "GET  $BASE_URL/$file"
  curl -fL --retry 3 --progress-bar -o "$APPS_DIR/$file.part" "$BASE_URL/$file"
  mv "$APPS_DIR/$file.part" "$APPS_DIR/$file"
done

echo "Apps ready in $APPS_DIR"
