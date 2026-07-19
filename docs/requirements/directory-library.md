---
parent: Requirements
---
# Directory library

## Directory scan builds the library from sidecars and PDFs
`req~directory-library.scan~5`

Opening a directory as a library must fill the library from the directory tree: every Hayagriva
`.yml`/`.yaml` file and every Markdown sidecar (`.md` opening with a Hayagriva YAML frontmatter
block; the notes body below maps to the entry's comment fields — the text under `# Notes` to
the comment, each `## comment-<name>` section to that per-user comment) contributes its
entries, a PDF with the same base name next to a sidecar is
linked to the sidecar's entry, and PDFs without a sidecar appear immediately as stub entries
titled after the file; their metadata (embedded BibTeX, XMP, content heuristics — the standard
PDF import pipeline) and a generated citation key arrive asynchronously after the library is
shown, without replacing the entry instances. When the PDF yields no DOI, the DOI is looked up
online and the metadata behind it fills only the fields the PDF did not provide. Opening must not block on PDF parsing. Hidden files/directories, gitignored paths, `.yml` files not recognized as Hayagriva,
and `.md` files without a Hayagriva frontmatter
are skipped; unparseable Hayagriva files are reported as warnings without aborting the scan.
Scanning must not write or modify any file in the directory.
See [ADR 66](../decisions/0066-directory-as-library-with-hayagriva-sidecars.md) for more details.

Needs: impl

## Directory libraries are part of the restored session
`req~directory-library.session-restore~1`

When "Open last edited libraries" is enabled, a directory library that was open on shutdown is
reopened on the next start, exactly like `.bib` libraries: its root directory is remembered in
the last-opened list and routed back through the directory-library opener.

Needs: impl

## User changes are written back into the sidecar files
`req~directory-library.write-back~2`

A directory library persists into its sidecar files: user edits rewrite the entry's
file read-modify-write (content JabRef does not understand survives, including body sections of
Markdown sidecars under foreign headings), the first user edit of an
entry without a sidecar creates a Markdown sidecar `X.md` (next to its PDF, sharing the base
name, or named after the citation key) whose frontmatter carries the Hayagriva data and whose
markdownlint-clean body carries the comment fields (`# Notes` intro for the comment, one
`## comment-<name>` section per per-user comment); in plain `.yml` sidecars the comment fields
are written as extension keys. A citation-key edit renames the YAML map key, and deleting an
entry removes it
from its file — the file itself is trashed/deleted once its last entry is gone, the paired PDF
is never touched. Writes are debounced per file with a trailing-edge debounce that is re-armed
by every change event — including the keystroke events the CoarseChangeFilter marks as
filtered, so the tail of a typing burst is never lost; Save (Ctrl+S) flushes them and must never
write a `.bib` file ("Save as" remains the explicit `.bib` snapshot). Closing needs no save
prompt. System-initiated changes (background enrichment, generated citation keys, inbound
synchronization) do not create or rewrite sidecars.

Needs: impl

## External file changes appear live in an open directory library
`req~directory-library.inbound-sync~2`

While a directory library is open, external creation, modification, deletion, and renaming of
`.yml`/`.yaml`/`.md`/`.pdf` files under its root must be reflected in the open library. Changed
entries are updated in place (the entry identity is preserved), renames are detected via a
grace window over the monitor's delete + create events and keep the affected entries, and files
written by JabRef itself are recognized by fingerprint and not re-imported. All resulting
database mutations carry a non-local event source so the future write-back direction can ignore
them.

Needs: impl

## The groups panel mirrors the directory structure
`req~directory-library.groups~1`

An open directory library installs an automatic group that mirrors the folder tree
([#10930](https://github.com/JabRef/jabref/issues/10930)): each subdirectory containing
entries appears as a subgroup, an entry is contained in the group of its source file's
directory (and transitively in the parent directories' groups), and the tree follows
inbound synchronization and write-back live. The directory groups are read-only: no
entries or subgroups can be added to them, they cannot be dragged or edited.

Needs: impl

## The sidecar and its PDF follow the configured filename pattern
`req~directory-library.pattern-rename~1`

When write-back touches a single-entry sidecar, the sidecar and its equally named PDF are
renamed together to the base name the configured filename pattern (Linked files preferences)
generates for the entry, keeping the pair in sync. Multi-entry files have no single generating
entry and keep their name; occupied target names and pattern failures leave the current name
untouched. Entry file links and the catalog follow the rename; the watcher does not re-import
the renamed files.

Needs: impl

## The library is mirrored into a single .bib file
`req~directory-library.bib-mirror~1`

A directory library is continuously mirrored into `<root>/<root-name>.bib` (debounced with the
sidecar write-back), so plain BibTeX consumers and collaborators can read and edit the library
as one file; a snapshot of the last written mirror is kept as the merge base under
`.jabref/mirror-base.bib`. External modifications of the mirror — while the library is open or
while it was closed — are three-way merged into the library using the git-sync semantic merge:
auto-mergeable changes (including added and deleted entries) are applied and persisted into the
sidecars, true conflicts are put to the user via the git conflict resolution dialog, and a
cancelled resolution keeps the library's state. A pre-existing `.bib` without a recorded base is
adopted against an empty base, which can only add entries or raise conflicts, never delete
library content. Entries are matched across the mirror by citation key; entries without one are
not matched. The mirror itself is recreated when deleted and never imported as a sidecar.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
