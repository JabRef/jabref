#!/usr/bin/env bash
#
# macOS installer for the JabRef Browser-Extension fulltext bridge host.
# Registers the native-messaging manifest (pointing at jabext_host.py) for
# every locally-installed Chromium / Firefox browser. Double-click in Finder
# also works.
#
# Usage:
#   ./install.command [--host-path <path>]

set -euo pipefail

# The host runs on python3 (no interpreter is bundled); fail early and clearly.
if ! command -v python3 >/dev/null 2>&1; then
  echo "error: python3 was not found on PATH." >&2
  echo "       The fulltext host (jabext_host.py) runs on python3 via its shebang;" >&2
  echo "       install Python 3 (e.g. from python.org or 'brew install python') and re-run." >&2
  exit 1
fi

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

install_firefox "$HOME/Library/Application Support/Mozilla/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Google/Chrome/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Chromium/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Microsoft Edge/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/BraveSoftware/Brave-Browser/NativeMessagingHosts"
install_chromium "$HOME/Library/Application Support/Vivaldi/NativeMessagingHosts"

echo "[install] done. Reload the JabRef Browser Extension to launch the host."
