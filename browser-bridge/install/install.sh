#!/usr/bin/env bash
#
# Linux installer for the JabRef Browser-Extension fulltext bridge.
# Registers the native-messaging manifest for every locally-installed
# Chromium / Firefox browser.
#
# Usage:
#   ./install.sh [--bridge-path <path>]
#
# Defaults: bridge-path = ../build/jabext-bridge (next to install.sh)

set -euo pipefail

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    echo "error: this is the Linux installer. Windows registers the" >&2
    echo "native-messaging host under HKCU, not in a dotfile dir." >&2
    echo "Run the PowerShell installer instead:" >&2
    echo "  pwsh browser-bridge/install/install.ps1" >&2
    exit 1
    ;;
  Darwin)
    echo "error: this is the Linux installer. On macOS run:" >&2
    echo "  sh browser-bridge/install/install.command" >&2
    exit 1
    ;;
esac

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
  asset="jabext-bridge_linux_${arch}"
  cache_dir="${XDG_CACHE_HOME:-$HOME/.cache}/jabext-browser-bridge/$download_version"
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

# Firefox + forks.
install_firefox "$HOME/.mozilla/native-messaging-hosts"
install_firefox "$HOME/.librewolf/native-messaging-hosts"

# Chromium family.
install_chromium "$HOME/.config/google-chrome/NativeMessagingHosts"
install_chromium "$HOME/.config/chromium/NativeMessagingHosts"
install_chromium "$HOME/.config/microsoft-edge/NativeMessagingHosts"
install_chromium "$HOME/.config/BraveSoftware/Brave-Browser/NativeMessagingHosts"
install_chromium "$HOME/.config/vivaldi/NativeMessagingHosts"

echo "[install] done. Reload the JabRef Browser Extension to launch the bridge."
