---
nav_order: 55
parent: Decision Records
status: proposed
date: 2026-03-06
---
# Use Hybrid Architecture (Protocol Handler + HTTP) for Browser Extension Communication

## Context and Problem Statement

JabRef's browser extension imports bibliographic data from web pages into the desktop application via HTTP requests to a local server (`jabsrv`).
If JabRef is not running, the request fails silently and the import is lost.
Additionally, the server sets `Access-Control-Allow-Origin: *` without authentication, allowing any website to send requests.

How should the extension communicate with the desktop application to solve both problems while remaining cross-platform, cross-browser, and maintainable?

## Decision Drivers

* Must work on Windows, macOS, and Linux
* Must work in Chrome and Firefox under Manifest V3
* Must handle the case when JabRef is not running
* Must authenticate the extension and protect against CSRF
* Changes must be maintainable by JabRef's open-source community

## Considered Options

* Native Messaging
* Local HTTP API (status quo)
* Protocol Handler only
* Hybrid — Protocol Handler + HTTP
* WebSocket
* Companion / Daemon

## Decision Outcome

Chosen option: "Hybrid — Protocol Handler + HTTP", because it comes out best (see below).

The extension sends data via HTTP to `jabsrv` on localhost.
When JabRef is not running, a `jabref://` protocol handler starts the application; the extension then polls until the HTTP endpoint becomes reachable.

### Consequences

* Good, because HTTP is platform- and browser-independent using standard `fetch()` API
* Good, because the protocol handler starts JabRef when it is not running, without encoding data in the URL
* Good, because `jabsrv` already provides the HTTP infrastructure
* Good, because graceful degradation to pure HTTP when handler is not registered
* Bad, because a localhost listener requires CSRF mitigations (custom headers, origin checks)
* Bad, because the protocol handler must be registered on three operating systems
* Bad, because two communication channels increase implementation complexity

## Pros and Cons of the Options

### Native Messaging

* Good, because no network listener exposed — best security properties
* Good, because the browser manages the host process lifecycle
* Bad, because six manifest variants and two host script languages required
* Bad, because high packaging overhead across multiple OS and package formats
* Bad, because JabRef already aims to remove this infrastructure due to maintenance burden (see PR #14884)

### Local HTTP API (status quo)

* Good, because platform- and browser-independent, easy to test
* Bad, because no mechanism to start JabRef when not running — import is lost

### Protocol Handler only

* Good, because can launch JabRef when not running
* Bad, because no authentication possible — any website can trigger the URL
* Bad, because limited URL length
* Bad, because unidirectional, no response channel

### Hybrid (Protocol Handler + HTTP)

* Good, because HTTP handles data transfer with full response channel
* Good, because protocol handler carries no data, avoiding the security issues of "Protocol Handler only"
* Good, because REST endpoints are extensible for future features
* Bad, because localhost listener requires CSRF mitigations
* Bad, because two communication channels increase complexity

### WebSocket

* Good, because persistent bidirectional connection with low latency
* Bad, because no mechanism to start JabRef when not running
* Bad, because WebSocket is not subject to Same-Origin Policy — any website can connect

### Companion / Daemon

* Good, because handles the offline-app case via local queue
* Bad, because three fundamentally different service managers per OS
* Bad, because effectively a second software project with own build system and CI/CD

## More Information

* [Issue #17: Architecture discussion](https://github.com/JabRef/JabRef-Browser-Extension-fresh/issues/17)
* [PR #18: Protocol Handler PoC](https://github.com/JabRef/JabRef-Browser-Extension-fresh/pull/18)
* [PR #14884: Remove Native Messaging infrastructure](https://github.com/JabRef/jabref/pull/14884)
