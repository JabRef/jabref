---
parent: Requirements
---
# PDF import

## PDF import must confirm untrusted authors against leading-page text
`req~import.pdf.author-confirmed-by-text~1`

A plausible merged author is confirmed when a named family name occurs in the PDF's first two pages. If no family name matches, at least two distinct non-initial words from the author value must occur instead. Matching ignores accents, hyphens, and apostrophe variants, but a capitalized family name must appear capitalized or in capitals so ordinary prose does not confirm it.

Needs: impl, utest

## PDF import must preserve authors when leading-page text is unavailable
`req~import.pdf.author-preserved-without-text~1`

If the first two pages have no extractable words or text extraction fails, the author cross-check leaves the merged author unchanged.

Needs: impl, utest

## PDF import must preserve authors from entries with citation keys
`req~import.pdf.author-preserved-with-citation-key~1`

When the first author-bearing candidate has a citation key, the cross-check keeps its author even without text confirmation. The citation key is evidence that the value came from bibliographic metadata rather than PDF document properties.

Needs: impl, utest

## PDF import must preserve authors from entries with known entry types
`req~import.pdf.author-preserved-with-entry-type~1`

When the first author-bearing candidate has a known, non-default entry type, the cross-check keeps its author even without text confirmation. A generic or unknown type does not provide this protection.

Needs: impl, utest

## PDF import must not confirm implausible author values from text
`req~import.pdf.author-candidate-plausible~1`

An author value containing prose-like lowercase words cannot be confirmed or selected as a replacement. Common name particles and separators remain allowed, and scripts without case do not require capital letters.

Needs: impl, utest

## PDF import must prefer the candidate with the most confirmed family names
`req~import.pdf.author-best-confirmed-candidate~1`

When the merged author is unconfirmed and unprotected, the cross-check selects the plausible candidate with the most family names found in the leading-page text. Equal scores retain the earlier candidate in import priority order. A candidate without a confirmed family name cannot replace the merged author.

Needs: impl, utest

## PDF import must remove a lone unconfirmed author without bibliographic evidence
`req~import.pdf.author-unconfirmed-single-removed~1`

If no candidate is confirmed, the unprotected merged author is removed when it contains only one named author. A trailing `et al.` does not count as another named author.

Needs: impl, utest

## PDF import must retain multiple unconfirmed authors without a replacement
`req~import.pdf.author-unconfirmed-multiple-retained~1`

If no candidate is confirmed, the cross-check leaves an unprotected merged author unchanged when it contains more than one named author.

Needs: impl, utest

## PDF import must extract only plausible years from the first page
`req~import.pdf.plausible-year~1`

JabRef accepts a standalone four-digit year outside a year range, between 1500 and two years after the current year. The same rule applies to years extracted from Springer footer text.

Rationale: The first page can contain postal codes, ISSNs, and page ranges that look like years but describe something else.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
