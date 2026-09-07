---
parent: Requirements
---
# Import

## Normalize imported BibTeX keyword delimiters
`req~import.bibtex.keywords.normalize-delimiters~1`

When importing BibTeX entries, JabRef applies the "Normalize keyword delimiters" cleanup (see `req~save.keywords.normalize-delimiters~1`) to every imported entry, so groups, search, and the keyword editor split the field on the library's separator from the start.
The library's separator is the one declared in the library's metadata; if none is declared, it is the accepted delimiter that the library's keyword fields already use most; if the keyword fields contain no delimiter, it is the globally configured keyword separator.
Keyword fields that already use the library's separator are left untouched, so opening a library does not rewrite them.

Delimiter characters that are part of a keyword remain part of that keyword and are escaped when necessary.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
