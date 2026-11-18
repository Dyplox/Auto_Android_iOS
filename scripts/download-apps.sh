#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Descarga la app demo (WebdriverIO Native Demo App, MIT) para Android e iOS Simulator.
# La misma app (React Native) existe en ambas plataformas: ideal para un framework
# donde lo único que cambia son los locators.
#
# Uso: ./scripts/download-apps.sh [version]     (por defecto 2.2.0)
# Si cambias la versión, actualiza app.path en src/test/resources/config/*.properties
# -----------------------------------------------------------------------------
set -euo pipefail

VERSION="${1:-2.2.0}"
BASE_URL="https://github.com/webdriverio/native-demo-app/releases/download/v${VERSION}"
APPS_DIR="$(cd "$(dirname "$0")/.." && pwd)/apps"

mkdir -p "$APPS_DIR"

for file in "android.wdio.native.app.v${VERSION}.apk" "ios.simulator.wdio.native.app.v${VERSION}.zip"; do
  if [[ -s "$APPS_DIR/$file" ]]; then
    echo "OK   $file ya existe"
    continue
  fi
  echo "GET  $BASE_URL/$file"
  curl -fL --retry 3 --progress-bar -o "$APPS_DIR/$file.part" "$BASE_URL/$file"
  mv "$APPS_DIR/$file.part" "$APPS_DIR/$file"
done

echo "Apps listas en $APPS_DIR"
