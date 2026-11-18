#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Sube la app demo al almacenamiento del proveedor cloud.
#
# Uso: ./scripts/upload-app.sh browserstack|saucelabs [version]   (version por defecto 2.2.0)
#
# Credenciales desde variables de entorno o el archivo .env:
#   BrowserStack: BROWSERSTACK_USERNAME, BROWSERSTACK_ACCESS_KEY
#   Sauce Labs:   SAUCE_USERNAME, SAUCE_ACCESS_KEY, SAUCE_REGION (opcional, us-west-1)
#
# Los nombres coinciden con app.path de config/env/<proveedor>-<plataforma>.properties:
#   BrowserStack -> custom_id "WdioDemoApp-Android" (siempre apunta a la última versión subida)
#   Sauce Labs   -> storage:filename=<nombre del archivo>
# -----------------------------------------------------------------------------
set -euo pipefail

PROVIDER="${1:-}"
VERSION="${2:-2.2.0}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APPS_DIR="$ROOT_DIR/apps"
ANDROID_APP="$APPS_DIR/android.wdio.native.app.v${VERSION}.apk"
IOS_APP="$APPS_DIR/ios.simulator.wdio.native.app.v${VERSION}.zip"

# Variables del .env sin pisar las que ya existen en el entorno
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
      echo "Falta la variable $name (defínela en el entorno o en .env)" >&2
      exit 1
    fi
  done
}

require_file() {
  if [[ ! -s "$1" ]]; then
    echo "No existe $1. Ejecuta primero ./scripts/download-apps.sh $VERSION" >&2
    exit 1
  fi
}

case "$PROVIDER" in
  browserstack)
    require BROWSERSTACK_USERNAME BROWSERSTACK_ACCESS_KEY
    require_file "$ANDROID_APP"
    echo "Subiendo $(basename "$ANDROID_APP") a BrowserStack (custom_id WdioDemoApp-Android)..."
    curl -fsS -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
      -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
      -F "file=@$ANDROID_APP" \
      -F 'data={"custom_id": "WdioDemoApp-Android"}'
    echo
    echo "iOS: BrowserStack solo ofrece iPhones reales (.ipa firmado) y la app demo solo tiene build de simulador."
    ;;
  saucelabs)
    require SAUCE_USERNAME SAUCE_ACCESS_KEY
    REGION="${SAUCE_REGION:-us-west-1}"
    for app in "$ANDROID_APP" "$IOS_APP"; do
      require_file "$app"
      echo "Subiendo $(basename "$app") a Sauce Labs ($REGION)..."
      curl -fsS -u "$SAUCE_USERNAME:$SAUCE_ACCESS_KEY" \
        -X POST "https://api.${REGION}.saucelabs.com/v1/storage/upload" \
        -F "payload=@$app" \
        -F "name=$(basename "$app")"
      echo
    done
    ;;
  *)
    echo "Uso: $0 browserstack|saucelabs [version]" >&2
    exit 1
    ;;
esac
