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

## Imported entries stay locatable in the library
`req~import.entries.sorted-by-id~1`

Entries are kept in the library in the order of their internal ids, regardless of the order in which a batch of imported entries arrives (e.g. after per-entry background duplicate checks). Looking up an entry's position in the library therefore succeeds for every imported entry, so the main table can select and update it.

Needs: impl, utest

## Unresolved merge conflict markers abort the import
`req~import.bibtex.merge-conflict-markers~1`

A BibTeX file that still contains version control conflict markers is rejected with an error naming the line of the first marker, instead of importing an arbitrary side of the conflict or storing the markers inside an entry.
A marker is a line starting with at least seven `<` or `>` characters.
The `=======` and `|||||||` lines of a conflict are not looked for on their own: they always follow a `<<<<<<<` line, and such lines also occur as decorative rules in field values.

Needs: impl, utest

## PDF import keeps only authors the document prints
`req~import.pdf.author-confirmed-by-text~1`

When importing a PDF, an author taken from the PDF's document properties is kept only if the text of the leading pages confirms it; otherwise an author list extracted from the document text replaces it.
If no candidate is confirmed, a single unconfirmed person from the document properties is dropped, because office suites store the account name of whoever exported the file there.
Metadata previously written by JabRef (an entry with citation key or explicit type) is kept even when the text does not confirm it.

Needs: impl, utest

## PDF import extracts only plausible years
`req~import.pdf.plausible-year~1`

When extracting the year from the text of a PDF's first page, JabRef takes only a standalone four-digit number that is not part of a four-digit range (such as a page range), between 1900 and two years after the current year, so postal codes, ISSNs, and page ranges are not imported as the year.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
