---
parent: Requirements
---
# Duplicate detection

## Match publications independently of entry type
`req~duplicates.type-independent-matching~1`

Entries describing the same publication can be detected as duplicates even when their entry types differ.
Changing an entry's type or swapping the order of the entries does not change the duplicate decision.
For example, the Article and Misc entries in [issue #16580](https://github.com/JabRef/jabref/issues/16580) are duplicates.

A shared unique publication identifier, such as a DOI, continues to identify a duplicate directly.
Otherwise, a matching title and at least one matching author, editor, date, year, or ISBN are required before evaluating weighted field similarity.
A shared author alone does not identify a publication.
Conflicting editions and different chapters or pages of the same publication retain their existing exclusions.
Citation keys, group memberships, and linked-file locations do not affect the publication comparison.

Needs: impl, utest

### Implementation notes

`DuplicateCheck.isDuplicate` compares the union of populated bibliographic fields with the existing field comparison helpers and a threshold of 0.75.
Core publication fields receive the previous factor of three consistently across all entry types; the required and optional fields of an entry type no longer determine the score.
The constructor and database-mode argument remain available for existing callers.

`compareEntriesStrictly` continues to compare complete records, including their entry types.
Finding a possible duplicate across types therefore does not make it an exact duplicate for automatic removal.

<!-- markdownlint-disable-file MD022 -->
