# Debugging jpackage installations

Sometimes issuses with modularity only arise in the installed version and do not occur if you run from source.
Using remote debugging, it's still possible to hook your IDE into the running JabRef application to enable debugging.

## Debugging on Windows

1. Open `build.gradle`, under jlink options remove`'--strip-debug',`
2. Build or let the CI build a new version
3. Download the modified version or portable version go to `\JabRef\runtime\bin\Jabref.bat`
4. Modify the bat file, replace the last line with 
```cmd
pushd %DIR% & %JAVA_EXEC% -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n -p "%~dp0/../app" -m org.jabref/org.jabref.JabRefLauncher  %* & popd
```
5. Open your IDE and add a Remote Debugging Configuration for `localhost:8000 `
6. Start JabRef from the bat file
7. Connect with your IDE using remote debugging
