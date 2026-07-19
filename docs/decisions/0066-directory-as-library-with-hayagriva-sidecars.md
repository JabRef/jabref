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

* Hayagriva-based sidecars (Markdown notes files with a Hayagriva YAML frontmatter, plain
  Hayagriva `.yml` also read), directory represented as a third `DatabaseLocation` with an
  empty database path
* Pure Hayagriva YAML sidecars (`X.yml` next to `X.pdf`)
* A hidden auto-maintained `.bib` file inside the directory
* XMP metadata embedded in the PDFs as the only store

## Decision Outcome

Chosen option: "Hayagriva-based sidecars with a third `DatabaseLocation`", because sidecars
keep the folder tool-agnostic (Hayagriva is Typst's bibliography format and JabRef has a
symmetric importer/exporter for it), embedded XMP cannot represent all fields and rewrites the
PDFs themselves, and a hidden `.bib` would duplicate state that immediately drifts from the
files. Pure YAML sidecars lost against the Markdown form because per-entry notes (JabRef's
comment fields) are long-form Markdown that reads terribly as YAML block scalars but naturally
as a Markdown body — the folder then doubles as a plain notes collection (Obsidian, any text
editor).

Key points of the chosen design:

* A JabRef-authored sidecar is a Markdown file (`X.md` next to `X.pdf`): the YAML frontmatter
  (between two `---` lines) is a regular Hayagriva document, the body below is markdownlint-clean
  Markdown — a `# Notes` heading, the entry's comment text beneath it, and one
  `## comment-<name>` section per per-user comment field. Body content under other headings is
  kept but not imported. Plain Hayagriva `.yml`/`.yaml` files are still read and written back
  (they stay directly loadable by Typst); JabRef-only fields are written there as
  `comment`/`comment-<name>` extension keys, which the Hayagriva parser ignores.

* `DatabaseLocation.DIRECTORY`: the context keeps an **empty** database path plus a separate
  directory root. Empty path gives correct default behavior at almost every existing decision
  point (no autosave/backup managers, no `.bib` change monitor, "needs saved local database"
  actions disabled). The directory root is registered as the library-specific file directory, so
  relative PDF links resolve without a database path.
* Pairing is by convention — `X.md`/`X.yml`/`X.yaml` next to `X.pdf` — because Hayagriva has no
  file-path field; nothing JabRef-specific is written into the YAML for the association.
* Non-Hayagriva `.yml` files (CI configs, ...) and `.md` files without a Hayagriva frontmatter
  (READMEs, plain notes) are ignored via format recognition, not reported
  as errors. PDFs without a sidecar appear immediately as stubs; their metadata is extracted
  asynchronously after the library is shown (the standard PDF import pipeline, enriching the
  stub in place); a sidecar is only written once the user edits the entry (scanning never
  writes files).
* Later synchronization mirrors the shared-SQL seam (`convertToSharedDatabase` /
  `DBMSSynchronizer`): a directory synchronizer subscribes to entry events through a
  `CoarseChangeFilter` for write-back and applies inbound file changes with a non-local
  `EntriesEventSource` to prevent echo loops; directory watching uses the Commons-IO
  `DirectoryMonitor` ([ADR-0030](0030-use-apache-commons-io-for-directory-monitoring.md)).
  "Save as" converts a directory library into a regular `.bib` library.
* The library is additionally mirrored into a visible `<root>/<root-name>.bib`. Unlike the
  rejected hidden-`.bib` option, the mirror is not primary state: it is derived from the
  sidecars on every change, and external edits of it are three-way merged back (base = the
  mirror as last written, kept under `.jabref/mirror-base.bib`) using the git-sync semantic
  merge and its conflict-resolution dialog. This gives plain-BibTeX consumers and
  collaborators one file to read and edit without the drift the hidden `.bib` was rejected for.

### Consequences

* Good, because the directory stays the single source of truth; `.yml` sidecars are usable from
  Typst as-is, and `.md` sidecars double as plain Markdown notes.
* Good, because the empty-database-path representation needs only a handful of explicit UI
  branches (tab title, close confirmation, save-as).
* Bad, because a `.md` sidecar is not directly loadable by Typst — its frontmatter must be
  extracted (trivially, e.g. with `sed`/`yq`) or exported to obtain a plain Hayagriva file.
* Bad, because YAML comments in hand-edited sidecars will not survive JabRef rewrites (the YAML
  parser drops them).
* Bad, because library-level metadata (groups, save actions) has no natural home yet; a
  metadata file in the root may be added later.
