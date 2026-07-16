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

<!-- markdownlint-disable-file MD022 -->
