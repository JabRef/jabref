---
parent: Requirements
---
# Import

## Normalize imported BibTeX keyword delimiters
`req~import.bibtex.keywords.normalize-delimiters~1`

When importing BibTeX entries, JabRef interprets the configured accepted keyword delimiters and stores the resulting keywords using the configured keyword separator.

Delimiter characters that are part of a keyword remain part of that keyword and are escaped when necessary.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
