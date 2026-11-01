---
parent: Requirements
---
# Duplicate detection

## Duplicate detection must recognize matching publications across compatible entry types
`req~duplicate-detection.compatible-entry-types~1`

Issue: [#17293](https://github.com/JabRef/jabref/issues/17293)

The compatible types are `InBook`, `InCollection`, `InProceedings`, and its BibTeX alias `Conference`.
For different compatible types, both field definitions must identify the same publication.
For `InBook`, the `BOOKTITLE` scenario uses BibLaTeX-style entries; typical BibTeX-style entries do not use it the same way.
Parent-title variations are scored with the other metadata instead of being rejected before scoring.
A shared ISBN identifies the parent volume and is not enough to match two contributions.
The existing `Misc` behavior uses the first entry type and can depend on argument order.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
