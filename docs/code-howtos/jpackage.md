---
parent: Code Howtos
---
# JPackage: Creating a binary and debug it

JabRef uses [jpackage](https://docs.oracle.com/en/java/javase/25/jpackage/) to build binary application bundles and installers for Windows, Linux, and macOS. For Gradle, we use the [Java Module Packaging Gradle plugin](https://github.com/gradlex-org/java-module-packaging).

## Build Windows binaries locally

Preparation: Install [WiX Toolset](https://wixtoolset.org)

1. Open administrative shell
2. Use [Chocolatey](https://chocolatey.org) to install it: `choco install wixtoolset`

Create the installer:

`./gradlew -PprojVersion="6.0.50013" -PprojVersionInfo="6.0-ci.13--2025-12-19--c8e5924" :jabgui:jpackage`

## Debugging jpackage installations

Sometimes issues with modularity only arise in the installed version and do not occur if you run from source. Using remote debugging, it's still possible to hook your IDE into the running JabRef application to enable debugging.

### Debugging on Windows

1. Open `build-logic\src\main\kotlin\org.jabref.gradle.base.targets.gradle.kts`, remove `--strip-debug`
2. Build using `jpackage` (or let the CI build a new version)
3. Modify the `build\packages\windows-latest\JabRef\runtime\bin\Jabref.bat` file, replace the last line with

    ```shell
    pushd %DIR% & %JAVA_EXEC% -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n -p "%~dp0/../app" -m org.jabref/org.jabref.Launcher  %* & popd
    ```

4. Open your IDE and add a "Remote Debugging Configuration" for `localhost:8000`
5. Start JabRef by running the above `.bat` file
6. Connect with your IDE using remote debugging
