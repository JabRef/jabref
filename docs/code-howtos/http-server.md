---
parent: Code Howtos
---
# HTTP Server

## Get SSL Working

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

## Start http server

The class starting the server is `org.jabref.http.server.Server`.

Test files to server can be passed as arguments.
If no files are passed, the last opened files are served.
If that list is also empty, the file `src/main/resources/org/jabref/http/server/http-server-demo.bib` is served.

### Starting with gradle

Does not work.

Current try:

```shell
./gradlew run -Pcomment=httpserver
```

However, there are with `ForkJoin` (discussion at <https://discuss.gradle.org/t/is-it-ok-to-use-collection-parallelstream-or-other-potentially-multi-threaded-code-within-gradle-plugin-code/28003>)

Gradle output:

```shell
> Task :run
2023-04-22 11:30:59 [main] org.jabref.http.server.Server.main()
DEBUG: Libraries served: [C:\git-repositories\jabref-all\jabref\src\main\resources\org\jabref\http\server\http-server-demo.bib]
2023-04-22 11:30:59 [main] org.jabref.http.server.Server.startServer()
DEBUG: Starting server...
<============-> 92% EXECUTING [2m 27s]
> :run
```

IntelliJ output, if `org.jabref.http.server.Server#main` is executed:

```shell
DEBUG: Starting server...
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.glassfish.grizzly.http.server.NetworkListener.start()
INFO: Started listener bound to [localhost:6051]
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.glassfish.grizzly.http.server.HttpServer.start()
INFO: [HttpServer] Started.
2023-04-22 11:44:59 [ForkJoinPool.commonPool-worker-1] org.jabref.http.server.Server.lambda$startServer$4()
DEBUG: Server started.
```

## Developing with IntelliJ

IntelliJ Ultimate offers a Markdown-based http-client. One has to open the file `src/test/java/org/jabref/testutils/interactive/http/rest-api.http`.
Then, there are play buttons appearing for interacting with the server.
