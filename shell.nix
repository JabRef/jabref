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
# `programs.nix-ld` does not fix this: it supplies an ELF interpreter (which is
# what lets the Gradle-provisioned Corretto toolchain in `~/.gradle/jdks` run at
# all) plus a small default library set, but that set has no X11 or GTK in it.
#
# The JDK is deliberately *not* provided here — the build pins toolchain vendor
# AMAZON, so Gradle auto-provisions Corretto regardless of what is on PATH.
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
in
pkgs.mkShell {
  packages = [
    pkgs.xvfb-run # `xvfb-run --auto-servernum ./gradlew :jabgui:check` — the GUI tests
  ] ++ runtimeLibs;

  LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs;
}
