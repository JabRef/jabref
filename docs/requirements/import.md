---
parent: Requirements
---
# Import

## Normalize imported BibTeX keyword delimiters
`req~import.bibtex.keywords.normalize-delimiters~1`

When importing BibTeX entries, JabRef interprets the configured accepted keyword delimiters and stores the resulting keywords using the configured keyword separator.

Delimiter characters that are part of a keyword remain part of that keyword and are escaped when necessary.

Needs: impl, utest

## Give the entries-import dialog's download-linked-files checkbox its own preference
`req~import.dialog-download-linked-files.remember-independently~1`

The "download linked online files" checkbox in the entries-import review dialog (used by File > Import, Web Search, LaTeX import, and Extract References) is backed by its own persisted preference, initialized from and written back to that preference on every dialog interaction, including cancel. The dialog's choice controls download behavior for that import directly, independent of and without modifying the general web-search download-linked-files setting.

<!-- markdownlint-disable-file MD022 -->