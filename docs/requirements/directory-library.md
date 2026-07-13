---
parent: Requirements
---
# Directory library

## Directory scan builds the library from sidecars and PDFs
`req~directory-library.scan~1`

Opening a directory as a library must fill the library from the directory tree: every Hayagriva
`.yml`/`.yaml` file contributes its entries, a PDF with the same base name next to a sidecar is
linked to the sidecar's entry, and PDFs without a sidecar become stub entries titled after the
file. Hidden files/directories, gitignored paths, and `.yml` files not recognized as Hayagriva
are skipped; unparseable Hayagriva files are reported as warnings without aborting the scan.
Scanning must not write or modify any file in the directory.
See [ADR 66](../decisions/0066-directory-as-library-with-hayagriva-sidecars.md) for more details.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
