---
nav_order: 70
parent: Decision Records
status: accepted
date: 2026-08-28
---

# Keep the import and fulltext native-messaging hosts separate

## Context and Problem Statement

The JabRef browser extension needs two capabilities from a local JabRef companion reached over WebExtension native messaging:

* **Import** (browser to JabRef): hand a BibTeX entry to JabRef. The extension attempts this first over JabRef's HTTP server (`jabsrv`, `localhost:23119`) and, only if that is unavailable, falls back to a native-messaging host, `org.jabref.jabref` (the `jabrefHost.py` / `JabRefHost.ps1` scripts JabRef registers today), invoked one-shot via `runtime.sendNativeMessage`.
* **Fulltext** (JabRef to browser to PDF): the Browser-Extension Fulltext Protocol (`req~bxf.*`). Here JabRef is the client of a loopback HTTP server that the companion runs; JabRef locates the companion by reading a discovery file the companion writes to `<config>/fulltext-providers/<name>.json`. The extension owns this host over a persistent `runtime.connectNative` port (`jabext_bridge`), because an MV3 service worker cannot itself bind a TCP port.

PR #16124 (JabRef) folded the import handling into the fulltext host, and PR #81 (extension) rerouted import and the options-page "validate" check from `org.jabref.jabref` onto the persistent `jabext_bridge` connection. This record settles the question that raised: should the extension talk to one native-messaging host or two?

## Decision Drivers

* Distribution: how many native-messaging host manifests JabRef's installer registers per browser, and how many host scripts it ships.
* Compatibility: the extension is distributed independently of JabRef and must degrade gracefully against a released JabRef that registers only `org.jabref.jabref` and has no fulltext consumer.
* Lifecycle fit: import is stateless and request-scoped; fulltext is a long-lived local server whose discovery file must have a single owner.
* Robustness of the discovery singleton: the fulltext provider is discoverable only while exactly one process owns its discovery file. A design in which a second host instance can rewrite or delete that file is fragile.
* Host and extension complexity: single-purpose scripts and direct per-capability calls, versus one script that multiplexes two lifecycles and one extension that relays all traffic through its background worker.
* Direction of the import channel: import already prefers `jabsrv` HTTP; the native-messaging import host is a fallback, not the primary path.

## Considered Options

* Two hosts — import on `org.jabref.jabref` (one-shot), fulltext on `jabext_bridge` (persistent)
* One host, all traffic over the persistent connection — the background worker owns a single `connectNative` port and relays import and validate over it
* One host, dual connection modes with a singleton guard — one registration serving both one-shot import and persistent fulltext, with a cross-process lock so only one instance owns the HTTP server and discovery file

## Decision Outcome

Chosen option: "Two hosts", because it matches the two capabilities' different lifecycles, keeps the discovery singleton correct by construction, preserves compatibility with released JabRef, and avoids adding coordination machinery — a background relay or a cross-process lock — whose only purpose is to make two unlike lifecycles share one process. The single-host benefit, one fewer registration, is modest, and it is further eroded by the import channel's move toward `jabsrv` HTTP: the steady state that trend points at is one native-messaging host (fulltext) plus HTTP import, not one merged host.

### Consequences

* Good, because each host stays single-purpose: `org.jabref.jabref` is a stateless one-shot, the fulltext host is a persistent server; neither carries the other's concerns.
* Good, because import keeps working against released JabRef and the options-page validate stays a direct `sendNativeMessage`, with no background-worker relay.
* Good, because the discovery file has exactly one writer (the persistent fulltext host), so no locking is needed to keep it correct.
* Bad, because JabRef registers and ships two native-messaging hosts rather than one, and the extension manifest references two host names.
* Bad, because the "one companion" mental model is not realized; a reader must know that import and fulltext are served by different processes.
* Neutral, because the merge already implemented — import folded into `jabext_host.py` / `jabext_host.ps1`, PR #81's routing, and the e2e import case — is reverted or left dormant; the fulltext host and its cross-platform e2e are unaffected.

### Confirmation

The extension sends import and validate to `org.jabref.jabref` and fulltext traffic to `jabext_bridge`; no extension context other than the background worker opens a `connectNative` port, and no process except the persistent fulltext host writes the fulltext discovery file. A review of the extension's native-messaging call sites confirms the two host names are each used for their capability.

## Pros and Cons of the Options

### Two hosts

* Good, because import (one-shot, stateless) and fulltext (persistent server) each get a host whose lifecycle matches.
* Good, because the fulltext discovery file has a single owner by construction.
* Good, because import stays compatible with released JabRef and needs no extension-internal relay.
* Neutral, because the native-messaging import host is a fallback behind `jabsrv` HTTP and may itself be retired later, leaving a single native-messaging host.
* Bad, because two host registrations must be installed and kept in the extension's allowed-extensions lists.

### One host, all traffic over the persistent connection

This is the state introduced by PR #81 (commit 466087f).

* Good, because a single host name and one registration serve both capabilities.
* Bad, because import can no longer use one-shot `sendNativeMessage`: a one-shot instance would write and then delete the shared discovery file, so import must be relayed over the single persistent port owned by the background worker.
* Bad, because non-background contexts (the options page) must message the background to reach the host, an indirection that is harder to follow than a direct call.
* Bad, because it drops `org.jabref.jabref`, so the extension no longer imports against a released JabRef that predates the merged host.

### One host, dual connection modes with a singleton guard

* Good, because one registration serves both capabilities while import keeps its one-shot `sendNativeMessage` call and its compatibility.
* Good, because it can retain the `org.jabref.jabref` name, leaving the extension's import path unchanged.
* Bad, because it needs a cross-process singleton (a lock file or equivalent, on POSIX and Windows) so that only the primary instance owns the HTTP server and discovery file while one-shot import instances run import-only.
* Bad, because one script then multiplexes two lifecycles and a locking protocol — more to get right than two single-purpose scripts, and harder to reason about under concurrent import and fulltext.

## More Information

Import's primary transport is `jabsrv` HTTP (`localhost:23119`); the native-messaging import host is the fallback. This record neither changes that nor decides whether the native-messaging import fallback should eventually be dropped in favour of HTTP-only import — a step that would leave a single native-messaging host (fulltext) without merging anything.

Related material: the Browser-Extension Fulltext Protocol spec (`docs/requirements/browser-extension-fulltext.md`) and the fulltext host (`browser-bridge/jabext_host.py`, `browser-bridge/jabext_host.ps1`). If "Two hosts" is accepted, PR #81's routing commit and the import fold in the fulltext host are reverted; if "dual connection modes" is later preferred, this record is superseded.
