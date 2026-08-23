# browser-bridge — JabRef Browser-Extension Fulltext host

A loopback HTTP native-messaging host that lets [JabRef][jabref]'s
`BrowserExtensionFulltextFetcher` reach the JabRef Browser Extension via the
[Browser-Extension Fulltext Protocol][spec] (`req~bxf.*~1`).

Shipped **as source**, exactly like `jabgui/buildres/*/jabrefHost.py`: no build
step, no GraalVM/JBang/native-image, no per-OS binary. Python on Linux/macOS,
PowerShell on Windows.

## Why

MV3 service workers cannot bind TCP ports. The protocol requires the provider
to host an HTTP endpoint on `127.0.0.1`. This host process owns that port and
forwards each request to the extension over native messaging.

```text
JabRef --HTTP--> jabext_host --native-messaging--> extension --> tab/PDF
```

## Naming

| Symbol                                           | Value                              |
| ------------------------------------------------ | ---------------------------------- |
| Native-messaging host (registry / connectNative) | `jabext_bridge`                    |
| JabRef provider name (discovery file)            | `jabext-bridge`                    |
| Host script (Linux/macOS)                        | `jabext_host.py`                   |
| Host script + launcher (Windows)                 | `jabext_host.ps1` + `jabext_host.bat` |
| Firefox extension gecko id                       | `@jabfox`                          |
| Chromium extension id (pinned)                   | `bifehkofibaamoeaopjglfkddgkijdlh` |

The Chromium extension id is fixed by the `manifest.key` field in the
extension's [`wxt.config.ts`][ext] and matches the store-published JabRef
Browser Extension, so the native-messaging manifests work for dev builds and
the store install alike.

## Layout

| Path                                      | Role                                                                |
| ----------------------------------------- | ------------------------------------------------------------------- |
| `jabext_host.py`                          | The host: loopback HTTP server + native-messaging dispatch. Stdlib only. |
| `jabext_host.ps1`                         | Windows mirror (HttpListener + NM reader runspace)                  |
| `jabext_host.bat`                         | NM launcher for Windows (browsers launch only `.exe`/`.bat`)        |
| `e2e_test.py`                             | End-to-end harness; drives either host as a subprocess              |
| `install/install.sh`                      | Linux installer (Firefox + Chromium family)                         |
| `install/install.ps1`                     | Windows installer                                                   |
| `install/install.command`                 | macOS installer                                                     |
| `native-messaging/firefox.json.template`  | NM manifest template, Firefox                                       |
| `native-messaging/chromium.json.template` | NM manifest template, Chromium                                      |

## Testing

```sh
python3 jabext_host.py --selftest   # in-process check: health, auth, correlation
python3 e2e_test.py                 # spawns the host as a subprocess, drives the full protocol
```

`e2e_test.py` drives *either* host: the Python host always, and the PowerShell
host wherever `pwsh` is on `PATH` — the same assertions for both prove protocol
parity. CI runs it on `ubuntu-latest` + `windows-latest`.

## Installing

```sh
./browser-bridge/install/install.sh          # Linux
pwsh browser-bridge/install/install.ps1       # Windows
sh browser-bridge/install/install.command     # macOS
```

The installers point the native-messaging manifest at `jabext_host.py`
(Linux/macOS, run via its `#!/usr/bin/env python3` shebang) or
`jabext_host.bat` → `jabext_host.ps1` (Windows).

## Protocol mirror

The spec is the canonical copy at
[`docs/requirements/browser-extension-fulltext.md`][spec] in this repo.
Identifiers (`req~bxf.*~N`) are protocol-scoped, shared across every provider
implementation. The host satisfies the provider half of `req~bxf.health~1`,
`req~bxf.fetch~1`, `req~bxf.fetch-errors~1`, `req~bxf.discovery-dir~1`,
`req~bxf.discovery-schema~1`, `req~bxf.loopback-bind~1`, `req~bxf.auth-bearer~1`,
and `req~bxf.origin-check~1`. The extension side ([`src/utils/fulltextBridge.js`][ext])
covers the browser-tab fetch loop.

`req~bxf.cancellation~1` is implemented per the spec's stated fallback: the
loopback server cannot detect mid-request client disconnect, so cancellation
flows via the provider-side fetch timeout (5 min). The spec explicitly permits
this trade-off.

## Merge with jabrefHost.py

This host and `jabgui/buildres/*/jabrefHost.py` are both browser-launched
native-messaging hosts for the same extension. The intent is to fold them into
one process: `jabext_host.handle_import_message()` is the seam where the
existing import dispatch lands, once the extension moves the import path to the
same `connectNative` connection. Until then they run as two hosts.

## Lifecycle

The extension starts the host by calling `runtime.connectNative` on
service-worker startup. The host inherits stdin/stdout from the browser, binds
an ephemeral 127.0.0.1 port, writes
`<JabRef-config>/fulltext-providers/jabext-bridge.json`, and serves HTTP until
stdin EOF. On EOF it deletes the discovery file and exits.

[jabref]: https://github.com/JabRef/jabref
[spec]: ../docs/requirements/browser-extension-fulltext.md
[ext]: https://github.com/JabRef/JabRef-Browser-Extension-experimental
