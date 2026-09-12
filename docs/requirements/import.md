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

## Legacy libraries are migrated when opened
`req~import.bibtex.legacy-migrations~1`

Opening a library written by JabRef 2.x/3.x converts its legacy content to the current representation: explicit group memberships stored inside the group tree move to the entries' `groups` field, `__markedentry` markings become groups, and special field values stored in `keywords` move to their own fields.
The keyword separator used for splitting is the library's own, falling back to the configured one.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
