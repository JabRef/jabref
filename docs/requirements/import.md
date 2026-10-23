---
parent: Requirements
---
# Import

## JabRef must import MARC21-XML bibliographic records
`req~import.marc21-xml~1`

JabRef supports importing MARC21 bibliographic records encoded as [MARCXML](https://www.loc.gov/standards/marcxml/) and maps recognised fields according to the [MARC 21 Format for Bibliographic Data](https://www.loc.gov/marc/bibliographic/) and the [DNB MARC21 export format](https://www.dnb.de/DE/Professionell/Metadatendienste/Exportformate/MARC21/marc21.html).

An importer should prefer proper fulltext links (e.g. Volltext) for PDFs over other content links.

Needs: impl, utest

## Import MARCXML files and BIBFRAME descriptions
`req~import.bibliographic.xml-formats~1`

JabRef imports standalone MARCXML records and collections as well as MARCXML in SRU responses. It also imports linked BIBFRAME 2.0 Work and Instance descriptions in RDF/XML, keeping multiple entries separate and rejecting external entities.

Needs: impl, utest

## Importer must normalize imported BibTeX keyword delimiters
`req~import.bibtex.keywords.normalize-delimiters~1`

When importing BibTeX entries, JabRef applies the "Normalize keyword delimiters" cleanup (see `req~save.keywords.normalize-delimiters~1`) to every imported entry, so groups, search, and the keyword editor split the field on the library's separator from the start.
The library's separator is the one declared in the library's metadata; if none is declared, it is the accepted delimiter that the library's keyword fields already use most; if the keyword fields contain no delimiter, it is the globally configured keyword separator.
Keyword fields that already use the library's separator are left untouched, so opening a library does not rewrite them.

Delimiter characters that are part of a keyword remain part of that keyword and are escaped when necessary.

Needs: impl, utest

## BibTeX importer must accept whitespace before citation keys
`req~import.bibtex.key-leading-whitespace~1`

This includes LF, CRLF, and multiple line breaks immediately after the entry's opening brace.

For example, the following entry retains the citation key `somebody2023` and title `Example` without warnings:

```bibtex
@inproceedings{
somebody2023,
  title = {Example}
}
```

Origin: <https://github.com/JabRef/jabref/issues/9622>

Needs: impl, utest

## BibTeX importer must recover entries missing their citation key
`req~import.bibtex.missing-key-recovery~1`

Recovery includes entries where both the key and its comma are omitted before the first field assignment.
Empty entries enclosed in braces or parentheses are retained, including an entry ending at the end of the file.
Fields are retained even with long custom field names or extensive whitespace before `=`.
Recovered assignments produce a warning naming the line of the first field; diagnostics for a missing key refer to the complete entry.

For example, the following entry omits both the citation key and its comma:

```bibtex
@article{
  author = {Ed von Test},
  title = {Test}
}
```

The importer retains both fields, leaves the citation key empty, and reports `Line 2: Found entry without citation key.`.

Context: <https://github.com/JabRef/jabref/pull/17496>

Needs: impl, utest

## BibTeX importer must preserve balanced parentheses in citation keys
`req~import.bibtex.citation-key-parentheses~1`

This applies to entries enclosed in parentheses as well as braces.
For `@article(Smith(2000), title={x})`, the citation key is `Smith(2000)` and the title is `x`.
Context: <https://github.com/JabRef/jabref/pull/17496#discussion_r4308280956>

Needs: impl, utest

## Library must maintain imported entries sorted by internal id
`req~import.entries.sorted-by-id~1`

Entries are kept in the library in the order of their internal ids, regardless of the order in which a batch of imported entries arrives (e.g. after per-entry background duplicate checks). Looking up an entry's position in the library therefore succeeds for every imported entry, so the main table can select and update it.

Needs: impl, utest

## Importer must abort when unresolved merge conflict markers are detected
`req~import.bibtex.merge-conflict-markers~1`

A BibTeX file that still contains version control conflict markers is rejected with an error naming the line of the first marker, instead of importing an arbitrary side of the conflict or storing the markers inside an entry.
A marker is a line starting with at least seven `<` or `>` characters.
The `=======` and `|||||||` lines of a conflict are not looked for on their own: they always follow a `<<<<<<<` line, and such lines also occur as decorative rules in field values.

Needs: impl, utest

## JabRef must report unreadable libraries and close temporary tab
`req~import.library.unreadable-reported~1`

When a library file cannot be read or parsed at all, JabRef names the file and the reason it failed, and leaves no open tab behind. A file that parses with warnings is not affected: it still opens, and its warnings are reported separately.

Rationale: Closing the tab prevents leaving an empty, untitled library that the user could accidentally save over the original file that failed to load.

Needs: impl, utest

## Infer DOIs from other fields
`req~import.doi-from-other-fields~1`

If an entry's `doi` field is empty and another field contains a DOI, JabRef makes a best-effort attempt to identify that DOI and store it in the `doi` field.

Needs: impl, utest

## Legacy libraries are migrated when opened
`req~import.bibtex.legacy-migrations~1`

Opening a library written by JabRef 2.x/3.x converts its legacy content to the current representation: explicit group memberships stored inside the group tree move to the entries' `groups` field, `__markedentry` markings become groups, and special field values stored in `keywords` move to their own fields.
The keyword separator used for splitting is the library's own, falling back to the configured one.

Needs: impl, utest

## Libraries without legacy content must skip legacy migration
`req~import.bibtex.legacy-migrations.only-when-needed~1`

After conversion and saving, reopening the library does not convert entries or rebuild the entries list again.
Legacy content is identified by group membership keys, entry markings, and special field values in keywords, including in mixed-format libraries.

Needs: impl, utest

## Legacy library migration must announce conversion
`req~import.bibtex.legacy-migrations.announcement~1`

When legacy content is detected during opening, a localized notification names the library before conversion starts.

Needs: impl, utest

## Custom entry types of a library are only offered once
`req~import.entry-types.offered-once~1`

When a library declares entry types that differ from the ones stored in the preferences, JabRef offers to store them. This holds both for entry types JabRef does not know and for customizations of entry types JabRef ships.

The definition the user accepts is the one from the library file, and it replaces the stored one. Entry types the user leaves unchecked when confirming the dialog are remembered as declined. Opening the same library again therefore offers nothing, unless the definition in the library or the stored one has changed since. Cancelling the dialog decides nothing, so the entry types are offered again.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
