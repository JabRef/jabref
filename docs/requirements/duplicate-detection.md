---
parent: Requirements
---
# Duplicate detection

## Duplicate detection must recognize matching publications across compatible entry types
`req~duplicate-detection.compatible-entry-types~2`

Issue: [#17293](https://github.com/JabRef/jabref/issues/17293)

The compatible types are `InBook`, `InCollection`, `InProceedings`, and its BibTeX alias `Conference`.
For different compatible types, both field definitions must identify the same publication.
Parent-title variations are scored with the other metadata instead of being rejected before scoring.
A shared ISBN identifies the parent volume and is not enough to match two contributions.
The existing `Misc` fallback is preserved, and both argument orders describe the same pair.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
