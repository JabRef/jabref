#!/usr/bin/env bash
#
# macOS installer for the JabRef Browser-Extension fulltext bridge.
# Registers the native-messaging manifest for every locally-installed
# Chromium / Firefox browser. Double-click in Finder also works.
#
# Usage:
#   ./install.command [--bridge-path <path>]

set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/.." && pwd)"

bridge_path="$repo/build/jabext-bridge"
download_version=""

while [ $# -gt 0 ]; do
  case "$1" in
    --bridge-path) bridge_path="$2"; shift 2 ;;
    --download)    download_version="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [ -n "$download_version" ]; then
  arch=$(uname -m)
  case "$arch" in
    x86_64|amd64) arch=x86_64 ;;
    aarch64|arm64) arch=aarch64 ;;
  esac
  asset="jabext-bridge_macos_${arch}"
  cache_dir="$HOME/Library/Caches/jabext-browser-bridge/$download_version"
  mkdir -p "$cache_dir"
  if [ ! -x "$cache_dir/$asset" ]; then
    if [ "$download_version" = "latest" ]; then
      tag=$(curl -fsSL https://api.github.com/repos/JabRef/JabRef-Browser-Extension-experimental/releases/latest \
            | grep -oE '"tag_name":\s*"[^"]+"' | head -1 | sed -E 's/.*"([^"]+)"/\1/')
    else
      tag="$download_version"
    fi
    url="https://github.com/JabRef/JabRef-Browser-Extension-experimental/releases/download/$tag/$asset"
    echo "[install] downloading $url"
    curl -fL --output "$cache_dir/$asset" "$url"
    chmod +x "$cache_dir/$asset"
  fi
  bridge_path="$cache_dir/$asset"
fi

if [ ! -x "$bridge_path" ]; then
  echo "error: bridge binary missing at $bridge_path (run 'browser-bridge/build.sh' or pass --download <version>)" >&2
  exit 1
fi

install_firefox() {
  local out="$1"
  mkdir -p "$out"
  sed "s|@BRIDGE_PATH@|$bridge_path|g" "$repo/native-messaging/firefox.json.template" \
      > "$out/jabext_bridge.json"
  chmod 600 "$out/jabext_bridge.json"
  echo "[install] firefox: $out/jabext_bridge.json"
}

install_chromium() {
  local out="$1"
  mkdir -p "$out"
  sed "s|@BRIDGE_PATH@|$bridge_path|g" "$repo/native-messaging/chromium.json.template" \
      > "$out/jabext_bridge.json"
  chmod 600 "$out/jabext_bridge.json"
  echo "[install] chromium: $out/jabext_bridge.json"
}

install_firefox "$HOME/Library/Application Support/Mozilla/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Google/Chrome/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Chromium/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Microsoft Edge/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/BraveSoftware/Brave-Browser/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Vivaldi/NativeMessagingHosts"

echo "[install] done. Reload the JabRef Browser Extension to launch the bridge."
