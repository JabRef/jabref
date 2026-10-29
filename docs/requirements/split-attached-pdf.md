---
parent: Requirements
---
# Split attached proceedings PDF and extract paper pages

This page collects requirements on splitting proceedings PDFs into individual paper entries, extracting page ranges when attaching PDFs, and opening cross-referenced PDFs in the document viewer.

Issue: [#8128](https://github.com/JabRef/jabref/issues/8128)

## Resolve PDF file and start page through cross-referenced entries
`req~logic.externalfiles.pdf-location-resolver~1`

The target PDF file and opening page are identified from `crossref` or parent entries. The page is computed from the entry's `pages` field and shifted by the parent's `pageoffset`.

Needs: impl, utest

## Document viewer must open cross-referenced PDF at paper start page
`req~jabgui.documentviewer.open-crossref-pdf-at-page~1`

The document viewer action is enabled for entries that have a cross-referenced PDF even without their own attached file. When triggered on such an entry, it confirms with the user, opens the parent PDF in the viewer, and jumps directly to the paper's start page.

Needs: impl, utest

## Split attached proceedings PDF into individual paper entries
`req~logic.pdf.split-proceedings-outline~1`

When attaching a multi-paper PDF to a proceedings, collection or book entry (including their multi-volume types), the system detects individual paper sections via document outlines/bookmarks and title page font-size heuristics. It creates an entry for each detected paper, typed after the volume (`InProceedings`, `InCollection` or `InBook`), generates a separate PDF containing only that paper's pages, and links it to the new entry without modifying or deleting the original PDF. The papers get their printed pages, read with the volume entry's `pageoffset` or else the PDF's page labels, and the volume entry stores that offset if it has none. A paper already in the library that cross-references the volume entry under the same title gets its PDF instead of a second entry. In a BibTeX library, the volume entry repeats its title as `booktitle`, and a paper in a book is an `InCollection`, because the title of a BibTeX `inbook` is that of the book.

Needs: impl, utest

## Extract paper page range when attaching PDF
`req~logic.pdf.extract-paper-page-range~1`

When attaching a PDF to an entry that specifies a page range (e.g., `pages = {10--18}`), the system extracts a copy containing only the entry's pages (adjusted for the entry's `pageoffset`, or else that of the entry it cross-references) and links the extracted PDF to the entry, keeping the source PDF intact. If the PDF already spans only the entry's pages, no extraction is performed. An entry without a `pageoffset` of its own opens a PDF that the cross-referenced entry also links with that entry's offset, and any other PDF without an offset, so the document viewer opens the attached PDF, and the copy once the original is removed, at the entry's first page.

Needs: impl, utest

## Importing a multi-paper PDF must create one entry per paper that cross-references an entry for the whole PDF
`req~logic.importer.multi-paper-pdf~1`

Applies to File → Import, Find unlinked files, and dropping a PDF onto the entry table. The papers are detected as for splitting. The entry for the whole PDF holds the file. Its type is proceedings when the title pages or the bookmarks name the event or its committees, a collection when they name editors or contributors or the papers give their authors' e-mail addresses, and a book otherwise. Each paper entry holds its title and printed pages. All entries hold the `pageoffset` read from the PDF's page labels. Once the entries are in the library, the PDF is split among the papers as when attaching it to the entry for the whole PDF, so each paper also gets a PDF of its own pages. In a BibTeX library, a collection becomes a book, the entry for the whole PDF repeats its title as `booktitle`, and a paper in a book is an `InCollection`, because BibTeX has no collection type and passes on only fields of the same name.

Needs: impl, utest

## Import must keep citation keys that other imported entries cross-reference
`req~jabgui.import.keep-cross-referenced-keys~1`

With "Generate a new key on import" enabled, a new key would break the `crossref` of the entries imported together with it.

Needs: impl, utest

## A generated paper entry must cross-reference the citation key of its volume entry
`req~logic.pdf.paper-entry-crossref~1`

The `crossref` of a generated paper entry is the citation key of the entry whose PDF was split.

Issue: [#8128](https://github.com/JabRef/jabref/issues/8128)

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
