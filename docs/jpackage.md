# jpackage

JabRef uses [jpackage](https://jdk.java.net/jpackage/) to build binary distributions for Windows, Linux, and Mac OS X.

## Build Windows binaries locally

Preparation:

1. Install [WiX Toolset](https://wixtoolset.org/)
   1. Open administrative shell
   2. Use [Chocolatey](https://chocolatey.org/) to install it: `choco install wixtoolset`
2. Open [git bash](https://superuser.com/a/1053657/138868)
3. Get [JDK14](https://openjdk.java.net/projects/jdk/14/): `wget https://download.java.net/java/GA/jdk14/076bab302c7b4508975440c56f6cc26a/36/GPL/openjdk-14_windows-x64_bin.zip`
4. Extract JDK14: `unzip openjdk-14_windows-x64_bin.zip`

Compile:

1. `export BADASS_JLINK_JPACKAGE_HOME=jdk-14/`
2. `./gradlew -PprojVersion="5.0.50013" -PprojVersionInfo="5.0-ci.13--2020-03-05--c8e5924" jlinkZip`
3. `./gradlew -PprojVersion="5.0.50013" -PprojVersionInfo="5.0-ci.13--2020-03-05--c8e5924" jpackage`

## Debugging jpackage installations

Sometimes issues with modularity only arise in the installed version and do not occur if you run from source. Using remote debugging, it's still possible to hook your IDE into the running JabRef application to enable debugging.

### Debugging on Windows

1. Open `build.gradle`, under jlink options remove `--strip-debug`
2. Build or let the CI build a new version
3. Download the modified version or portable version go to `\JabRef\runtime\bin\Jabref.bat`
4. Modify the bat file, replace the last line with

   ```cmd
   pushd %DIR% & %JAVA_EXEC% -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n -p "%~dp0/../app" -m org.jabref/org.jabref.JabRefLauncher  %* & popd
   ```

5. Open your IDE and add a "Remote Debugging Configuration" for `localhost:8000`
6. Start JabRef from the bat file
7. Connect with your IDE using remote debugging
