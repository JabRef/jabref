---
parent: Requirements
---
# Import

## Normalize imported BibTeX keyword delimiters
`req~import.bibtex.keywords.normalize-delimiters~1`

When importing BibTeX entries, JabRef interprets the configured accepted keyword delimiters and stores the resulting keywords using the configured keyword separator.

Delimiter characters that are part of a keyword remain part of that keyword and are escaped when necessary.

Needs: impl, utest

## Remember import-dialog download preference independently
`req~import.download-linked-online-files.remember-independently~1`

The "download linked online files" checkbox in the extension import dialog is backed by its own persisted preference, initialized from and written back to that preference — independent of the general web-search download-linked-files setting.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
