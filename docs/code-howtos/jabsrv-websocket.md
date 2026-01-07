# JabRef jabsrv WebSocket integration

This document explains the WebSocket endpoint implemented by the `JabRefWebSocketApp` and how clients and the application integrate with it.

## Files

- `JabRefWebSocketApp` implementation: [jabsrv/src/main/java/org/jabref/http/server/ws/JabRefWebSocketApp.java](jabsrv/src/main/java/org/jabref/http/server/ws/JabRefWebSocketApp.java)
- Server startup: [jabsrv/src/main/java/org/jabref/http/server/Server.java](jabsrv/src/main/java/org/jabref/http/server/Server.java)

## Endpoint

- Path: `/ws`
- Protocol: WebSocket (RFC 6455)
- Transport: Text messages (UTF-8)

## Usage notes

- The WebSocket application (JabRef) expects clients to connect to the path `/ws` on the same HTTP server that serves the HTTP server.
- Clients connect using standard WebSocket libraries available in most programming languages.
- The server supports both unencrypted (`ws://`) and encrypted (`wss://`) connections depending on the server configuration.

## Message formats

1) JSON command messages (recommended)

- Structure:

```json
{ "command": "<command>", "argument": "<argument>" }
```

- Supported `command` values:
  - `ping` — returns `{ "status": "success", "response": "pong" }`.
  - `focus` — instructs the application to request UI focus. No `argument` required.
  - `open` — instructs the app to open/add a file by path. Example: `{ "command": "open", "argument": "/path/to/library.bib" }`.
  - `add` — add a BibTeX entry provided in the `argument` string. Example: `{ "command": "add", "argument": "@article{...}" }`.

- Successful responses are JSON objects with `status` and `response` (or `message` on error).
- Unrecognized commands return an error response.

## Library interaction

- The WS app delegates work to a `RemoteMessageHandler` looked up from the HK2 `ServiceLocator`.
- Two primary startup paths register the handler:
  - CLI / standalone `jabsrv`: When starting via `Server.run(List<Path>, URI, RemoteMessageHandler)` the provided handler is registered in the `ServiceLocator` and used by the WS app.
  - GUI: The GUI code passes an instance of `CLIMessageHandler` into the HTTP manager/start flow. See `HttpServerManager.start(...)` and `HttpServerThread` which forward the handler into `Server.run(SrvStateManager, URI, RemoteMessageHandler)` so it becomes available to the WS app.

## Client example (using `wscat`)

- Connect:

```bash
wscat -c ws://localhost:23119/ws
```

- Add an entry using JSON:

```bash
# send JSON on one line
{ "command": "add", "argument": "@article{smith2020, title={Example}, author={Smith, A.}}" }
```

## Troubleshooting

- If you receive an HTTP 200/404 when attempting an upgrade, verify the client connects to `/ws` and that the server was created with add-ons attached before binding (the server is created with `start=false` and the `WebSocketAddOn` is registered to each `NetworkListener` prior to `server.start()`).
- If the handler appears null at runtime, confirm the GUI startup path is passing a `RemoteMessageHandler` into `HttpServerManager.start(...)` or start `Server` with the CLI overload that registers the handler.
- For TLS/WSS ensure the server has a valid keystore and is started with TLS configuration.
