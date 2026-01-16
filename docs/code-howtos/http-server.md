---
parent: Code Howtos
---
# HTTP Server

JabRef has a built-in http server.
The source is located in the project `jabsrv`.

The resource for a library is implemented at [`org.jabref.http.server.resources.LibraryResource`](https://github.com/JabRef/jabref/blob/main/jabsrv/src/main/java/org/jabref/http/server/resources/LibraryResource.java).

Some offered resources and possible interactions are shown at [`jabsrv/src/test/*.http`](../../jabsrv/srv/test/) using [IntelliJ's http syntax](https://www.jetbrains.com/help/idea/exploring-http-syntax.html).

## Design principles

The JabRef http API tries to be "RESTful".
It wants to reach [Level 2 of Richardson's maturity model](https://martinfowler.com/articles/richardsonMaturityModel.html).

The main reason to follow this principle is to be consistent to other RESTful HTTP APIs.

See [`rest-api.http`](../../jabsrv/src/test/rest-api.http) for example interactions.

Recommended reading:

- [REST API Design Rulebook](https://www.oreilly.com/library/view/rest-api-design/9781449317904/)
- [RESTful Web Services Cookbook](https://www.oreilly.com/library/view/restful-web-services/9780596809140/)

### Limits of RESTful HTTP design

RESTful HTTP design reaches its limits when doing "commands".
For instance, when focussing an entry, one should not `POST` `command: select` to an entry resource.
We opted to introduce a `command` resource to serve UI commands, such as selecting and entry or focusing the current JabRef instance.

See [`commands.http`](../../jabsrv/src/test/commands.http) for example interactions.

### Used libraries

To be standards based, [JAX-RS](https://projects.eclipse.org/projects/ee4j.rest) is used as the API specification language.
As implementation of JAX-RS, the reference implementation [Jersey](https://eclipse-ee4j.github.io/jersey/) is used.

## Starting the http server

In IntelliJ: Gradle > JabRef > jabsrv-cli > Tasks > application > run

```shell
./gradlew :jabsrv-cli:run
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

Navigate to <http://localhost:23119/libraries> to see the last opened libraries.

## Served libraries

The last opened libraries are served as default.
Additionally, more files can be passed as arguments.
If that list is also empty, the last opened libraries are served.

At the library resources, the library name `demo` serves `Chocolate.bib`.
The library name `current` denotes the currently focussed library in JabRef.

```shell
./gradlew :jabsrv-cli:run --args="../jablib/src/test/resources/testbib/complex.bib"
```

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
