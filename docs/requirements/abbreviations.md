---
parent: Requirements
---
> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](../../AGENTS.md)
> • [CONTRIBUTING.md](../../CONTRIBUTING.md)

# Abbreviations

## Book titles must use conference records for abbreviation and expansion
`req~abbreviations.booktitle~1`

When a conference repository is supplied to cleanup, `booktitle` uses that repository with the existing
lookup and conversion rules for default, dotless, and shortest unique abbreviations.
Unknown titles remain unchanged. Journal fields retain their own repository, LTWA support, and `fjournal` behavior.
For example, a record mapping `International Conference on Business Process Management` to `BPM`
enables conversion in both directions.

Issue: <https://github.com/JabRef/jabref/issues/12728>

Needs: impl, utest

## Book title integrity checks must use the conference abbreviation list
`req~abbreviations.conference-integrity~1`

Integrity checks report recognized abbreviations in book title fields using the configured conference list.
Journal-only abbreviations do not produce conference abbreviation warnings, and checks do not modify entries.
The same conference records support loading, abbreviation, checking, and expansion.

Issue: <https://github.com/JabRef/jabref/issues/12728>

Needs: impl, utest

## Users must be able to manage conference abbreviation lists independently of journal lists
`req~abbreviations.conference-preferences~1`

The conference preferences page supports opening, creating, editing, and removing CSV lists with the shared abbreviation editor.
Saving persists conference paths separately; canceling leaves the saved lists and files unchanged.
Journal-only settings remain on the journal page.

Issue: <https://github.com/JabRef/jabref/issues/12728>

Needs: impl, utest

## Cleanup must apply configured conference lists to book titles
`req~abbreviations.conference-cleanup-ui~1`

The cleanup dialog applies the selected list-based abbreviation or expansion to journal fields and book titles without restricting the entry type.
It loads the conference list once per operation and records changes in the existing undo journal.
LTWA remains limited to journal fields and is labeled accordingly.

Issue: <https://github.com/JabRef/jabref/issues/12728>

Needs: impl, utest

## Builds must package conference abbreviations separately from journal abbreviations
`req~abbreviations.conference-store~1`

The shared generator reads the data submodule's `conferences/` CSV files into
`conferences/conference-list.mv`, preserving the separate journal store.
An older data revision without a conference directory produces an empty conference store.
Users can still load their own conference CSV lists.

Issue: <https://github.com/JabRef/jabref/issues/12728>

Needs: impl, utest

<!-- markdownlint-disable-file MD013 MD022 MD033 MD041 -->
