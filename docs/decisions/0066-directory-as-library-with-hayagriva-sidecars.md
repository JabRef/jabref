---
nav_order: 0066
parent: Decision Records
---

# Directory as library with Hayagriva sidecars

## Context and Problem Statement

Users organize papers as PDFs in folder trees (often synced via cloud storage) and want JabRef
to treat such a folder directly as a library, without maintaining a separate `.bib` file. Each
work's bibliographic data (including notes) should live in a plain-text file next to its PDF, so
the folder stays usable from other tools (e.g. Typst). How should such a "directory library" be
represented in JabRef, and where does the per-entry data live?

## Decision Drivers

* The folder must remain self-describing and usable outside JabRef.
* JabRef's existing architecture assumes a `BibDatabaseContext` per library tab.
* File changes must eventually sync in both directions (external edits appear live, JabRef edits
  persist back) without echo loops.
* The groups panel should later mirror the folder structure ([#10930](https://github.com/JabRef/jabref/issues/10930)).

## Considered Options

* Hayagriva YAML sidecars (`X.yml` next to `X.pdf`), directory represented as a third
  `DatabaseLocation` with an empty database path
* A hidden auto-maintained `.bib` file inside the directory
* XMP metadata embedded in the PDFs as the only store

## Decision Outcome

Chosen option: "Hayagriva YAML sidecars with a third `DatabaseLocation`", because sidecars keep
the folder tool-agnostic (Hayagriva is Typst's bibliography format and JabRef has a symmetric
importer/exporter for it, including the `note` field), embedded XMP cannot represent all fields
and rewrites the PDFs themselves, and a hidden `.bib` would duplicate state that immediately
drifts from the files.

Key points of the chosen design:

* `DatabaseLocation.DIRECTORY`: the context keeps an **empty** database path plus a separate
  directory root. Empty path gives correct default behavior at almost every existing decision
  point (no autosave/backup managers, no `.bib` change monitor, "needs saved local database"
  actions disabled). The directory root is registered as the library-specific file directory, so
  relative PDF links resolve without a database path.
* Pairing is by convention — `X.yml`/`X.yaml` next to `X.pdf` — because Hayagriva has no
  file-path field; nothing JabRef-specific is written into the YAML for the association.
* Non-Hayagriva `.yml` files (CI configs, ...) are ignored via format recognition, not reported
  as errors. PDFs without a sidecar become entries with metadata extracted from the PDF itself (the
  standard PDF import pipeline, falling back to a stub titled after the file); a sidecar is
  only written once the user edits the entry (scanning never writes files).
* Later synchronization mirrors the shared-SQL seam (`convertToSharedDatabase` /
  `DBMSSynchronizer`): a directory synchronizer subscribes to entry events through a
  `CoarseChangeFilter` for write-back and applies inbound file changes with a non-local
  `EntriesEventSource` to prevent echo loops; directory watching uses the Commons-IO
  `DirectoryMonitor` ([ADR-0030](0030-use-apache-commons-io-for-directory-monitoring.md)).
  "Save as" converts a directory library into a regular `.bib` library.

### Consequences

* Good, because the directory stays the single source of truth and is usable from Typst as-is.
* Good, because the empty-database-path representation needs only a handful of explicit UI
  branches (tab title, close confirmation, save-as).
* Bad, because YAML comments in hand-edited sidecars will not survive JabRef rewrites (the YAML
  parser drops them), and JabRef-only fields need an extension mechanism inside the entry.
* Bad, because library-level metadata (groups, save actions) has no natural home yet; a
  metadata file in the root may be added later.
