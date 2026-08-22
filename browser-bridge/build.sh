#!/usr/bin/env bash
#
# Build the native-image bridge binary into browser-bridge/build/.
#
# Requires `mise` on PATH (https://mise.jdx.dev/getting-started.html).
# The .mise.toml in this directory pins GraalVM 25 and JBang.
#
# Usage:
#   ./build.sh            # native-image build (release)
#   ./build.sh --java     # JVM mode (for quick iteration, no native-image)

set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
cd "$here"

if ! command -v mise >/dev/null 2>&1; then
  echo "error: mise is not installed. Install from https://mise.jdx.dev/." >&2
  exit 1
fi

mise install

build_dir="$here/build"
mkdir -p "$build_dir"

bin_name="jabext-bridge"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) bin_name="jabext-bridge.exe" ;;
esac

if [ "${1:-}" = "--java" ]; then
  echo "[build] JBang JVM mode (no native-image)"
  mise exec -- jbang export portable --force -O "$build_dir/jabext-bridge.jar" JabExtBridge.java
  echo "[build] JAR at $build_dir/jabext-bridge.jar"
  exit 0
fi

echo "[build] native-image build via JBang"

# GraalVM CE 25 ships native-image under lib/svm/bin/; JBang looks under bin/.
# Stage a copy so JBang's exact-path probe finds it.
graalvm_home="$(mise where java)"
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    if [ ! -f "$graalvm_home/bin/native-image.exe" ] && [ -f "$graalvm_home/lib/svm/bin/native-image.exe" ]; then
      cp "$graalvm_home/lib/svm/bin/native-image.exe" "$graalvm_home/bin/native-image.exe"
    fi
    ;;
  *)
    if [ ! -f "$graalvm_home/bin/native-image" ] && [ -f "$graalvm_home/lib/svm/bin/native-image" ]; then
      ln -sf "$graalvm_home/lib/svm/bin/native-image" "$graalvm_home/bin/native-image"
    fi
    ;;
esac

mise exec -- jbang export native --force -O "$build_dir/$bin_name" JabExtBridge.java

echo "[build] binary at $build_dir/$bin_name"
