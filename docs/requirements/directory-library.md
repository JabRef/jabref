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

<!-- markdownlint-disable-file MD022 -->
