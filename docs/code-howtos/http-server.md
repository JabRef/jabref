---
parent: Code Howtos
---
# HTTP Server

JabRef has a built-in http server.
The source is located in the project `jabsrv`.

The resource for a library is implemented at [`org.jabref.http.server.resources.LibraryResource`](https://github.com/JabRef/jabref/blob/main/jabsrv/src/main/java/org/jabref/http/server/resources/LibraryResource.java).

## Start http server

### Starting with IntelliJ

The class starting the server is located in the project `jabsrv-cli` and is called `org.jabref.http.server.cli.ServerCli`.

Test files to server can be passed as arguments.
If that list is also empty, the file `src/main/resources/org/jabref/http/server/http-server-demo.bib` is served.

### Starting with JBang

In case you want to interact only with the http server and do not want to set up or run IntelliJ, [JBang](https://www.jbang.dev/download/) can be used.

In the repository root, run following command:

```shell
jbang .jbang/JabSrvLauncher.java
```

JBang also offers running without explicit installation, if you have node installed (and WSL available in the case of Windows):

```shell
npx @jbangdev/jbang .jbang/JabSrvLauncher.java
```

### Starting with gradle

```shell
./gradlew run :jabsrv:run
```

Gradle output:

```shell
> Task :jabsrv:run
2025-05-12 11:52:57 [main] org.glassfish.grizzly.http.server.NetworkListener.start()
INFO: Started listener bound to [localhost:6050]
2025-05-12 11:52:57 [main] org.glassfish.grizzly.http.server.HttpServer.start()
INFO: [HttpServer] Started.
JabSrv started.
Stop JabSrv using Ctrl+C
<============-> 96% EXECUTING [43s]
> :jabsrv:run
```

IntelliJ output, if `org.jabref.http.server.ServerCli#main` is executed:

```shell
DEBUG: Starting server...
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.glassfish.grizzly.http.server.NetworkListener.start()
INFO: Started listener bound to [localhost:6051]
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.glassfish.grizzly.http.server.HttpServer.start()
INFO: [HttpServer] Started.
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.jabref.http.server.ServerCli.lambda$startServer$4()
DEBUG: Server started.
```

## Served libraries

The last opened libraries are served.
`demo` serves Chocolate.bib.
Additional libraries can be served by passing them as arguments.

## Developing with IntelliJ

IntelliJ Ultimate offers a Markdown-based http-client. You need to open the file `jabsrv/src/test/rest-api.http`.
Then, there are play buttons appearing for interacting with the server.

In case you want to debug on Windows, you need to choose "WSL" as the target for the debugger ("Run on") to avoid "command line too long" errors.

## Get SSL Working

When interacting with the [Microsoft Word AddIn](https://github.com/JabRef/JabRef-Word-Addin), a SSL-based connection is required.
[The Word-AddIn is currentely under development](https://github.com/JabRef/JabRef-Word-Addin/pull/568).

(Based on <https://stackoverflow.com/a/57511038/873282>)

Howto for Windows - other operating systems work similar:

1. As admin `choco install mkcert`
2. As admin: `mkcert -install`
3. `cd %APPDATA%\..\local\org.jabref\jabref\ssl`
4. `mkcert -pkcs12 jabref.desktop jabref localhost 127.0.0.1 ::1`
5. Rename the file to `server.p12`

Note: If you do not do this, you get following error message:

```text
Could not find server key store C:\Users\USERNAME\AppData\Local\org.jabref\jabref\ssl\server.p12.
```
