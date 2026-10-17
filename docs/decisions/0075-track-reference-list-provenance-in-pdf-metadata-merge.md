---
nav_order: 75
parent: Decision Records
status: proposed
date: 2026-10-10
---
# Track reference-list provenance in PDF metadata merging
`adr~track-pdf-reference-list-provenance~1`

Needs: impl

## Context and Problem Statement

`PdfMergeMetadataImporter` merges entries returned by several PDF importers into one entry for the PDF being imported. `PdfGrobidImporter` instead returns works cited in that PDF's reference list. The common import loop still adds those cited works to the metadata candidates.

For example, Alice's paper may cite a paper by Bob. Grobid returns Bob's paper, and its DOI can trigger an online lookup. Fetched entries are placed before PDF-extracted entries in the merge, so Bob's fetched author or title may win over Alice's. The cited work's entry type describes Bob's paper, not Alice's PDF.

How should the merge distinguish reference-list data from metadata about the imported PDF?

## Considered Options

* Exclude reference-list entries from metadata candidates.
* Keep them as candidates and track which exact entries came from the reference list.
* Add source information to every candidate and merged field.

## Decision Outcome

Proposed interim option: keep reference-list entries as candidates and track their source by object identity. This preserves the existing merge pipeline while preventing an original cited-work entry's type from making its author automatically trusted. `BibEntry` equality compares content, so content equality cannot identify which returned object came from the reference-list importer.

The identity set does not include entries subsequently fetched from a cited work's DOI. Those fetched entries can still take priority and appear to be trusted bibliographic metadata. It also does not prevent original reference-list entries from contributing other fields. The decision to separate cited works from metadata candidates remains open before this ADR can be accepted.

### Confirmation

`PdfMergeMetadataImporterOfflineTest` covers a cited-work author being replaced by a byline confirmed in the PDF text.

## More Information

* [ADR 43](0043-show-merge-dialog-when-importing-a-single-pdf.md) describes the candidate merge used for PDF import.
* [PR 16247](https://github.com/JabRef/jabref/pull/16247) introduced the author cross-check.

<!-- markdownlint-disable-file MD022 -->
