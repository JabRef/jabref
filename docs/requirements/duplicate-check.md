---
parent: Requirements
---

# User can detect duplicate entries
`feat~duplicate-check~1`

JabRef can detect whether two bibliographic entries represent the same publication.

Needs: req

## Entries with identical persistent identifiers must be considered duplicates
`req~duplicate-check.same-identifier~1`

This rule has the highest priority over the others listed below.

Needs: impl, utest

Covers:

- feat~duplicate-check~1

## Entries with different entry types must not be considered duplicates unless one is misc
`req~duplicate-check.entry-type-compatibility~1`

Rationale: Entries with the generic `misc` type may represent unclassified records of any type and can therefore match entries of other types.

Needs: impl, utest

Covers:

- feat~duplicate-check~1

## Entries with differing editions must not be considered duplicates
`req~duplicate-check.different-editions~1`

Needs: impl, utest

Covers:

- feat~duplicate-check~1

## Entries for different parts of the same book must not be considered duplicates
`req~duplicate-check.book-parts-different-chapters-or-pages~1`

Needs: impl, utest

Covers:

- feat~duplicate-check~1

## Non-part entries with identical ISBNs must be considered duplicates
`req~duplicate-check.isbn-matching~1`

Rationale: Distinct articles or book chapters frequently share the parent book or journal volume ISBN and require further field comparison to determine duplication.

Needs: impl, utest

Covers:

- feat~duplicate-check~1

## Field comparison must normalize specialized fields before similarity matching
`req~duplicate-check.field-normalization~1`

Rationale: Formatting variations in author names (comparing last names), page range dashes, and journal names (ignoring periods and casing) are normalized before comparison so minor formatting differences do not prevent duplicate detection.

Needs: impl, utest

Covers:

- feat~duplicate-check~1

<!-- markdownlint-disable-file MD022 -->
