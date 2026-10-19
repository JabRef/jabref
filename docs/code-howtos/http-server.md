---
parent: Code Howtos
---
# HTTP Server

JabRef has a built-in http server.
The source is located in the project `jabsrv`.

The resource for a library is implemented at [`org.jabref.http.server.resources.LibraryResource`](https://github.com/JabRef/jabref/blob/main/jabsrv/src/main/java/org/jabref/http/server/resources/LibraryResource.java).

Some offered resources and possible interactions are shown at [`jabsrv/src/test/*.http`](https://github.com/JabRef/jabref/tree/main/jabsrv/src/test) using [IntelliJ's http syntax](https://www.jetbrains.com/help/idea/exploring-http-syntax.html).

## Design principles

The JabRef HTTP API tries to be "RESTful".
It wants to reach [Level 2 of Richardson's maturity model](https://martinfowler.com/articles/richardsonMaturityModel.html).

The main reason to follow this principle is to be consistent with other RESTful HTTP APIs.

See [`jabsrv/src/test/rest-api.http`](https://github.com/JabRef/jabref/blob/main/jabsrv/src/test/rest-api.http) for example interactions.

Recommended reading:

- [REST API Design Rulebook](https://www.oreilly.com/library/view/rest-api-design/9781449317904/)
- [RESTful Web Services Cookbook](https://www.oreilly.com/library/view/restful-web-services/9780596809140/)

### Limits of RESTful HTTP design

RESTful HTTP design reaches its limits when performing "commands".
For instance, when focussing an entry, one should not `POST` `command: select` to an entry resource.
So, we opted to introduce a `command` resource to serve UI commands, such as selecting and entry or focussing the current JabRef instance.

See [`jabsrv/src/test/commands.http`](https://github.com/JabRef/jabref/blob/main/jabsrv/src/test/commands.http) for example interactions.

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

## Logging in to CiteDrive without the GUI

`jabsrv --citedrive-login` prints the CiteDrive login address, waits for the browser to come back to `/callback`, stores the refresh token in the system keyring, and exits.
JabRef's HTTP server port from the preferences has to be the port `jabsrv` listens on, since CiteDrive redirects the browser there.

```shell
./gradlew :jabsrv-cli:run --args="--citedrive-login"
```

### CiteDrive authentication security

CiteDrive's API and web application addresses must use HTTPS; only the local browser callback uses HTTP.
Each authentication operation captures its server settings, and uploads use the address associated with their access token.
Changing the API address clears the login and prevents pending responses from restoring credentials for the previous configuration.
The keyring stores the API address alongside the refresh token; existing tokens without that binding require another login.

Concurrent token requests share one authentication operation to support refresh token rotation.
Both token exchange and refresh use a ten-second connection timeout and a thirty-second read timeout, with redirects disabled.
The code exchange runs asynchronously so that a slow token endpoint does not block the callback response.
Temporary errors preserve the stored login; an HTTP 400 `invalid_grant` response starts another browser login.

The current upload sends all entries from the active library in a single BibTeX request to `/jabref/push/`.
Each upload therefore uses one request from the endpoint's current allowance of 100 requests per hour per user; the server's payload size limit is a separate constraint.
`CiteDrivePushTest` verifies batches of two and one thousand entries by parsing the prepared request back into entries.
Setting the test JVM system property `citedrive.batch.fixture` to a generated 100,000-entry library adds that fixture to the same check.
The test records the payload size and preparation timings without contacting CiteDrive.

## Developing with IntelliJ

IntelliJ Ultimate offers a Markdown-based http-client. You need to open the file `jabsrv/src/test/rest-api.http`.
Then, there are play buttons appearing for interacting with the server.

In case you want to debug on Windows, you need to choose "WSL" as the target for the debugger ("Run on") to avoid "command line too long" errors.

## Get SSL Working

When interacting with the [Microsoft Word AddIn](https://github.com/JabRef/JabRef-Word-Addin), a SSL-based connection is required.
[The Word-AddIn is currently under development](https://github.com/JabRef/JabRef-Word-Addin/pull/568).

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
