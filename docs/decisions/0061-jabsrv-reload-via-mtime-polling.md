---
title: Reload edited libraries in jabsrv via mtime polling
nav_order: 61
parent: Decision Records
status: accepted
date: 2026-05-27
---
<!-- markdownlint-disable-next-line MD025 -->
# Reload edited libraries in jabsrv via mtime polling

## Context and Problem Statement

The stand-alone HTTP server (`jabsrv`) snapshots a fixed set of `.bib` files at startup in `JabRefSrvStateManager`. If a user edits one of those files on disk, the cached `BibDatabaseContext` and its associated `SearchContext` become stale. How should `jabsrv` detect and reload edited libraries?

The rest of JabRef uses the `FileUpdateMonitor` abstraction for this purpose, but the only production implementation, `DefaultFileUpdateMonitor`, lives in the `jabgui` module and is not visible from `jabsrv`. `jabsrv` currently uses `DummyFileUpdateMonitor` everywhere.

## Decision Drivers

* Cost per request when the server is idle or lightly used
* Detection latency on external edits
* Code duplication across modules
* Robustness on filesystems where `java.nio.file.WatchService` degrades (NFS, some containers, network mounts)
* Thread lifecycle: spawn, shutdown, failure surfacing
* HTTP request/response model: state only needs to be fresh when a client asks

## Considered Options

* Compare `Files.getLastModifiedTime` against a snapshot per call to `getOpenDatabases()` (mtime polling)
* Move `DefaultFileUpdateMonitor` from `jabgui` to `jablib` and wire it into `JabRefSrvStateManager`
* Add a server-local `FileUpdateMonitor` implementation inside `jabsrv`

## Decision Outcome

Chosen option: "mtime polling," because

* The HTTP server is pull-based: freshness only matters when a request arrives, and an mtime check is one `stat` per tracked library per request — cheap and lazy.
* No background thread, no async reparse, no synchronization of the open-database list.
* No new module-level refactor (the `DefaultFileUpdateMonitor` move would touch ~10 `jabgui` callsites and reshape module boundaries for no observable server benefit).
* `WatchService` silently degrades to polling on NFS and some container filesystems, so the "real" monitor would not actually be push-based in every deployment anyway.

## Pros and Cons of the Options

### Mtime polling

Per-call `Files.getLastModifiedTime` compared against an in-memory `Map<Path, FileTime>` snapshot taken at the last successful parse. Re-parse happens inline on the calling thread when the mtime has changed.

* Good, because lazy — no work on an idle server.
* Good, because synchronous — no concurrent access to `openDatabases`.
* Good, because the failure mode is "parse error logged, previous context retained," which is easy to reason about.
* Good, because no new threads, no shutdown hook, no `WatchService` capability check.
* Bad, because cost scales with `requests × libraries`.
* Bad, because does not align with the rest of the codebase's `FileUpdateMonitor`-based approach.

### Move `DefaultFileUpdateMonitor` from `jabgui` to `jablib`

The deps of `DefaultFileUpdateMonitor` (`JabRefException`, `WatchServiceUnavailableException`, Guava, `java.nio`) are all already available in `jablib`, so the move is mechanically straightforward.

* Good, because a single `FileUpdateMonitor` implementation is shared between GUI and server.
* Good, because reload becomes push-based: edits trigger an immediate (async) reparse rather than waiting for the next request.
* Good, because aligns `jabsrv` with the rest of JabRef's freshness model.
* Neutral, because the move touches ~10 `jabgui` import sites and re-shapes module boundaries for a non-GUI use case.
* Bad, because reparse runs asynchronously on the watcher thread; `openDatabases` and the search-context registry need synchronization.
* Bad, because `WatchService` silently downgrades to polling on NFS and certain container filesystems — the "real-time" benefit disappears in those deployments.
* Bad, because the watcher thread runs (and consumes resources) even when the server receives no traffic.

### Server-local `FileUpdateMonitor` implementation in `jabsrv`

A new class inside `jabsrv` that implements `FileUpdateMonitor`, mirroring `DefaultFileUpdateMonitor`.

* Good, because avoids a cross-module refactor.
* Good, because the implementation can be tuned for the server (dedicated executor, no GUI assumptions).
* Bad, because ~80 lines of code are duplicated from `DefaultFileUpdateMonitor`.
* Bad, because two implementations of the same interface drift over time.
* Bad, because all the async/sync and `WatchService`-degradation downsides of option 2 apply.
