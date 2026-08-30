---
parent: Requirements
---
# Duplicate finding

This page collects requirements on finding duplicate entries within a library.
A user triggers the search via Quality → Find duplicates.

## Scalable duplicate candidate generation
`req~logic.duplicates.candidate-blocking~1`

Issue: [#16579](https://github.com/JabRef/jabref/issues/16579)

For libraries with more than 1000 entries, the duplicate search determines candidate pairs by deterministic blocking keys (canonical identifiers, leading title words, leading author/editor last names) instead of comparing every pair of entries.
The number of candidate pairs stays far below the exhaustive `n * (n - 1) / 2`, while pairs that the duplicate check classifies as duplicates remain candidates:

- Entries sharing an identifier (DOI in any form, ISBN, eprint, PMID, MR number) are always candidates, regardless of block sizes.
- The entry type never excludes a pair, because different sources may use different entry types for the same publication.
- Entries without any blocking key are compared with every other entry.
- Libraries with at most 1000 entries keep the exhaustive all-pairs search and thus unchanged recall.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
