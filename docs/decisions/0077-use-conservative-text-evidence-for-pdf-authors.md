---
nav_order: 77
parent: Decision Records
status: proposed
date: 2026-10-10
---
# Use conservative text evidence for PDF authors
`adr~conservative-pdf-author-evidence~1`

Needs: impl

## Context and Problem Statement

PDF document properties can name the account that exported a file rather than the work's author. Text extracted from the leading pages may contain bylines, prose, and cited names. How should JabRef decide whether to keep or replace an untrusted author value?

## Considered Options

* Trust the first nonempty author value from PDF metadata.
* Accept any parseable author candidate whose family name occurs in the text.
* Require a plausible name list and a family-name match in the leading-page text.
* Detect the byline's position and structure before accepting a candidate.

## Decision Outcome

Chosen option: "Require a plausible name list and a family-name match in the leading-page text", because a byline confirms a name without requiring layout detection. A confirmed merged author is kept; otherwise the candidate with the most confirmed family names wins. A lone unconfirmed author is removed if no candidate matches. Bibliographic metadata and PDFs without extractable text retain their author value.

The plausibility check uses capitalization for scripts with case and allows common lowercase name particles. Scripts without case, including Arabic and CJK, do not need entries in that particle list. This remains a heuristic: legitimate lowercase names and organization names can be rejected, while a name occurring outside a byline can produce a false match.

### Confirmation

`PdfMergeMetadataImporterOfflineTest` covers creator metadata, confirmed replacements, prose rejection, and Arabic and CJK names. The lowercase-name limitation remains open for a stronger byline-aware approach.

## More Information

* [PDF import requirements](../requirements/pdf-import.md) specify the author and year rules.
* [PR 16247](https://github.com/JabRef/jabref/pull/16247) introduced the author cross-check.

<!-- markdownlint-disable-file MD022 -->
