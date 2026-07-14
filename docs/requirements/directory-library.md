---
parent: Requirements
---
# Directory library

## Directory scan builds the library from sidecars and PDFs
`req~directory-library.scan~4`

Opening a directory as a library must fill the library from the directory tree: every Hayagriva
`.yml`/`.yaml` file contributes its entries, a PDF with the same base name next to a sidecar is
linked to the sidecar's entry, and PDFs without a sidecar appear immediately as stub entries
titled after the file; their metadata (embedded BibTeX, XMP, content heuristics — the standard
PDF import pipeline) and a generated citation key arrive asynchronously after the library is
shown, without replacing the entry instances. When the PDF yields no DOI, the DOI is looked up
online and the metadata behind it fills only the fields the PDF did not provide. Opening must not block on PDF parsing. Hidden files/directories, gitignored paths, and `.yml` files not recognized as Hayagriva
are skipped; unparseable Hayagriva files are reported as warnings without aborting the scan.
Scanning must not write or modify any file in the directory.
See [ADR 66](../decisions/0066-directory-as-library-with-hayagriva-sidecars.md) for more details.

Needs: impl

## External file changes appear live in an open directory library
`req~directory-library.inbound-sync~1`

While a directory library is open, external creation, modification, deletion, and renaming of
`.yml`/`.yaml`/`.pdf` files under its root must be reflected in the open library. Changed
entries are updated in place (the entry identity is preserved), renames are detected via a
grace window over the monitor's delete + create events and keep the affected entries, and files
written by JabRef itself are recognized by fingerprint and not re-imported. All resulting
database mutations carry a non-local event source so the future write-back direction can ignore
them.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
