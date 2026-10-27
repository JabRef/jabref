---
parent: Requirements
---

# Export

## Export BIBFRAME 2.0 RDF/XML
`req~export.bibframe.rdfxml~1`

JabRef exports each entry as a linked BIBFRAME 2.0 Work and Instance in striped RDF/XML. Resource identifiers are deterministic for unchanged bibliographic content, unique for duplicate entries within one file, and independent of citation keys.

See [ADR 0076](../decisions/0076-support-bibframe-alongside-marc21.md) for the decision to support BIBFRAME alongside MARC 21.

Needs: impl, utest

## User can export a Markdown publication list
`feat~export.markdown-publications~1`

The "Markdown publications" export format produces a publication list for use on Markdown websites.
Entries include available authors, title, publication venue, year, and links.

See [issue 12729](https://github.com/JabRef/jabref/issues/12729).

Needs: impl, utest

## Markdown publications must appear newest first
`req~export.markdown-publications.date-order~1`

Publication dates determine descending order using year, month, and day where available.
Entries without a usable publication date appear last.
The recent publications section contains at most five entries.

Needs: impl, utest

## Markdown publication sections must reflect Export subgroups
`req~export.markdown-publications.groups~1`

Subgroups beneath the group named "Export" produce sections containing matching exported entries.
A subgroup description supplies its heading, with the group name as the fallback.
Empty sections are omitted unless they contain a nonempty descendant section.

Needs: impl, utest

## Markdown publication links must support portable output
`req~export.markdown-publications.links~1`

Titles use a DOI link when available, otherwise a URL.
PDF links use online locations or local paths relative to the exported Markdown file.
Local attachments that cannot be resolved or represented relatively are omitted.

Needs: impl, utest

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
<!-- markdownlint-disable-file MD022 -->
