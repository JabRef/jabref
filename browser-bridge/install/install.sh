#!/usr/bin/env bash
#
# Linux installer for the JabRef Browser-Extension fulltext bridge host.
# Registers the native-messaging manifest (pointing at jabext_host.py) for
# every locally-installed Chromium / Firefox browser.
#
# Usage:
#   ./install.sh [--host-path <path>]
#
# Default host-path: ../jabext_host.py (next to this script's parent).

set -euo pipefail

# The host runs on python3 (no interpreter is bundled); fail early and clearly.
if ! command -v python3 >/dev/null 2>&1; then
  echo "error: python3 was not found on PATH." >&2
  echo "       The fulltext host (jabext_host.py) runs on python3 via its shebang;" >&2
  echo "       install your distribution's python3 package and re-run this installer." >&2
  exit 1
fi

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    echo "error: this is the Linux installer. On Windows run:" >&2
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

host_path="$repo/jabext_host.py"
while [ $# -gt 0 ]; do
  case "$1" in
    --host-path) host_path="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [ ! -f "$host_path" ]; then
  echo "error: host script missing at $host_path" >&2
  exit 1
fi
chmod +x "$host_path"    # NM launches it via its #!/usr/bin/env python3 shebang

install_firefox() {
  local out="$1"
  mkdir -p "$out"
  sed "s|@BRIDGE_PATH@|$host_path|g" "$repo/native-messaging/firefox.json.template" \
      > "$out/jabext_bridge.json"
  chmod 600 "$out/jabext_bridge.json"
  echo "[install] firefox: $out/jabext_bridge.json"
}

install_chromium() {
  local out="$1"
  mkdir -p "$out"
  sed "s|@BRIDGE_PATH@|$host_path|g" "$repo/native-messaging/chromium.json.template" \
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

echo "[install] done. Reload the JabRef Browser Extension to launch the host."
