---
parent: Requirements
---
# Duplicate detection

> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

## Duplicate detection must recognize matching publications across compatible entry types
`req~duplicate-detection.compatible-entry-types~1`

Issue: [#17293](https://github.com/JabRef/jabref/issues/17293)

The compatible types are `InBook`, `InCollection`, and `InProceedings` in both BibTeX and BibLaTeX libraries.
A pair qualifies when either entry type's field definition identifies the same publication, subject to the existing edition, chapter, page, and identifier rules.
Both argument orders describe the same pair.

Needs: impl, utest

## Duplicate detection must reject compatible cross-type entries with conflicting parent-book titles unless they share a publication identifier
`req~duplicate-detection.conflicting-parent-books~1`

Issue: [#17293](https://github.com/JabRef/jabref/issues/17293)

This applies to pairs of different types from `InBook`, `InCollection`, and `InProceedings` when both `booktitle` fields are present.
Conflicts are determined using the existing title comparison, including case and LaTeX normalization.
A missing parent title does not establish a conflict.
A shared ISBN identifies the parent volume rather than the contribution; a matching publication identifier such as DOI retains precedence.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
