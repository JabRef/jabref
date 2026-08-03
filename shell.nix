# Development shell for NixOS / nix-enabled Linux: `nix-shell` in the repo root.
#
# Nothing in JabRef needs Nix — this only supplies what an ordinary Linux
# distribution has in `/usr/lib` and NixOS deliberately does not.
#
# By default JabRef consumes JavaFX as plain Maven artifacts (see
# `build-logic/src/main/kotlin/org.jabref.gradle.base.dependency-rules.gradle.kts`),
# so the JavaFX native libraries ship as prebuilt `.so` files inside the jars.
# JavaFX extracts them to `~/.openjfx/cache/<version>/amd64/` and `dlopen`s them
# from there; they carry no RPATH and are not patchelf'ed, so the GTK/X11 stack
# they link against has to be reachable through `LD_LIBRARY_PATH`. Without it
# `./gradlew :jabgui:run` dies with
# `UnsatisfiedLinkError: no glass in java.library.path`.
#
# `programs.nix-ld` does not fix this: it supplies an ELF interpreter plus a
# small default library set, but that set has no X11 or GTK in it.
#
# On NixOS `programs.nix-ld.enable = true` is nevertheless a prerequisite, and
# this shell cannot provide it — it is system configuration. The build pins
# toolchain vendor AMAZON, so Gradle auto-provisions a Corretto JDK into
# `~/.gradle/jdks` no matter what is on PATH, and that JDK is an ordinary
# dynamically linked binary asking for `/lib64/ld-linux-x86-64.so.2`. NixOS only
# has that path when nix-ld creates it. The shellHook below checks for it and
# points at the fix rather than letting the build fail with a bare
# "No such file or directory". See the troubleshooting guide:
# docs/getting-into-the-code/guidelines-for-setting-up-a-local-workspace/trouble-shooting.md
#
# The JDK here is only a *bootstrap* for the Gradle wrapper: `./gradlew` is a
# shell script that needs an existing `java` to start Gradle at all, and
# toolchain auto-provisioning happens only afterwards, from inside that JVM.
# Gradle still resolves the Amazon toolchain for compiling and running JabRef,
# so this does not change which JDK the project is built against.
#
# `xvfb-run` is for the GUI tests, which CI runs as `xvfb-run --auto-servernum`
# (see `.github/workflows/tests-code.yml`); NixOS ships the `Xvfb` binary in
# `xorg.xorgserver` but the wrapper script in its own package.
{ pkgs ? import <nixpkgs> { } }:

let
  # nixpkgs moved the X libraries out of the `xorg` set and lower-cased them
  # (`xorg.libX11` -> `libx11`); take the new name where it exists so the shell
  # is warning-free on current nixpkgs and still evaluates on an older channel.
  x11 = name: legacy: pkgs.${name} or pkgs.xorg.${legacy};

  # Everything the extracted JavaFX natives link against:
  #   libglass.so                -> libX11
  #   libglassgtk3.so            -> gtk3, gdk, pango, atk, cairo, gdk-pixbuf, glib, libXtst
  #   libprism_es2.so            -> libX11, libXxf86vm, libGL
  #   libjavafx_font_freetype.so -> freetype, fontconfig
  #   libjavafx_font_pango.so    -> pango, glib
  javafxRuntimeLibs = (with pkgs; [
    gtk3
    glib
    pango
    cairo
    gdk-pixbuf
    atk
    freetype
    fontconfig
    libGL
  ]) ++ [
    (x11 "libx11" "libX11")
    (x11 "libxtst" "libXtst")
    (x11 "libxxf86vm" "libXxf86vm")
    (x11 "libxrender" "libXrender")
    (x11 "libxext" "libXext")
  ];

  # The embedded PostgreSQL server behind JabRef's search (`PostgresServer`,
  # io.zonky.test) unpacks vendor binaries to /tmp and runs them. They bundle
  # their own ICU/OpenSSL/libxml2/lzma via RPATH, but take libstdc++ and zlib
  # from the system. nix-ld happens to carry both in its default set, so this is
  # a belt-and-braces entry that keeps the shell working without nix-ld too.
  embeddedPostgresLibs = with pkgs; [
    stdenv.cc.cc.lib
    zlib
  ];

  runtimeLibs = javafxRuntimeLibs ++ embeddedPostgresLibs;

  # Bootstrap JDK for the Gradle wrapper only; see the header. Keep the major
  # version in sync with the toolchain in
  # build-logic/src/main/kotlin/org/jabref/gradle/Toolchains.kt (currently 25).
  bootstrapJdk = pkgs.jdk25;
in
pkgs.mkShell {
  packages = [
    bootstrapJdk
    pkgs.xvfb-run # `xvfb-run --auto-servernum ./gradlew :jabgui:check` — the GUI tests
  ] ++ runtimeLibs;

  # The Gradle wrapper prefers JAVA_HOME over a `java` found on PATH.
  JAVA_HOME = "${bootstrapJdk}";

  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs;

  # Fail loudly and early instead of letting Gradle die on the auto-provisioned
  # Corretto with a bare "No such file or directory". Only meaningful on NixOS:
  # every other distribution ships the interpreter at that path anyway.
  shellHook = ''
    if [ -e /etc/NIXOS ] && [ ! -e /lib64/ld-linux-x86-64.so.2 ]; then
      echo "warning: /lib64/ld-linux-x86-64.so.2 is missing." >&2
      echo "  Gradle auto-provisions an Amazon Corretto JDK into ~/.gradle/jdks and cannot" >&2
      echo "  run it without that ELF interpreter. Enable nix-ld in your system config:" >&2
      echo "" >&2
      echo "    programs.nix-ld.enable = true;" >&2
      echo "" >&2
      echo "  then 'nixos-rebuild switch' and re-enter this shell." >&2
    fi
  '';
}
