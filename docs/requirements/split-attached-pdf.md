---
parent: Requirements
---
# Split attached proceedings PDF and extract paper pages

This page collects requirements on splitting proceedings PDFs into individual paper entries, extracting page ranges when attaching PDFs, and opening cross-referenced PDFs in the document viewer.

Issue: [#8128](https://github.com/JabRef/jabref/issues/8128)

## Resolve PDF file and start page through cross-referenced entries
`req~logic.externalfiles.pdf-location-resolver~1`

When an entry does not have its own linked PDF, the system resolves the PDF and the opening page from cross-referenced parent entries (e.g., proceedings or books). The start page must be parsed from the entry's `pages` field and shifted by any `pageoffset` defined in the parent entry.

Needs: impl, utest

## Document viewer must open cross-referenced PDF at paper start page
`req~jabgui.documentviewer.open-crossref-pdf-at-page~1`

The document viewer action is enabled for entries that have a cross-referenced PDF even without their own attached file. When triggered on such an entry, it confirms with the user, opens the parent PDF in the viewer, and jumps directly to the paper's start page.

Needs: impl, utest

## Split attached proceedings PDF into individual paper entries
`req~logic.pdf.split-proceedings-outline~1`

When attaching a multi-paper PDF to a proceedings entry, the system detects individual paper sections via document outlines/bookmarks and title page font-size heuristics. It creates an `InProceedings` entry for each detected paper, generates a separate PDF containing only that paper's pages, and links it to the new entry without modifying or deleting the original PDF.

Needs: impl, utest

## Extract paper page range when attaching PDF
`req~logic.pdf.extract-paper-page-range~1`

When attaching a PDF to an entry that specifies a page range (e.g., `pages = {10--18}`), the system extracts a copy containing only the entry's pages (adjusted for `pageoffset`) and links the extracted PDF to the entry, keeping the source PDF intact. If the PDF already spans only the entry's pages, no extraction is performed.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
