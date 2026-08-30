---
nav_order: 70
parent: Decision Records
status: accepted
date: 2026-08-30
---
<!-- markdownlint-disable-next-line MD025 -->
# Use Deterministic Blocking for Duplicate Candidate Generation

## Context and Problem Statement

`DuplicateSearch` compared every unordered pair of entries, i.e., `n * (n - 1) / 2` invocations of `DuplicateCheck`.
For large libraries (tens of thousands of entries), this is prohibitively slow ([#16579](https://github.com/JabRef/jabref/issues/16579)).
How should duplicate candidates be generated so that the search scales, while the set of found duplicates stays (nearly) unchanged?

## Decision Drivers

* High candidate recall matters more than precision, particularly for systematic literature reviews; the user reviews each candidate before merging.
* `DuplicateCheck` embodies JabRef's established duplicate semantics and should remain the verifier.
* No new dependency and no port of R/Python code (ASySD, bib-dedupe).
* Results must be deterministic and testable.

## Considered Options

* Keep the exhaustive all-pairs scan
* Index exact identifiers only
* Deterministic multi-key blocking, verified by the existing `DuplicateCheck`
* Port ASySD-style blocking plus Jaro-Winkler scoring
* Locality-sensitive hashing / approximate nearest-neighbor techniques

## Decision Outcome

Chosen option: "Deterministic multi-key blocking, verified by the existing `DuplicateCheck`", because it removes almost all unrelated comparisons while keeping JabRef's duplicate semantics, adds no dependency, and is deterministic.

Details of the chosen design (implemented in `DuplicateCandidateGenerator`):

* Libraries with at most 1000 entries keep the exhaustive scan, so recall there is fully unchanged.
* Each entry receives redundant blocking keys: canonical identifiers (DOI parsed via the existing `DOI` class, ISBN, eprint, PMID, MR number), each of the first four title words (position-namespaced, matching the positional word comparison of `StringSimilarity#correlateByWords`), and each of the first two author/editor last names (normalized like `DuplicateCheck#compareAuthorField`).
* The entry type never excludes a pair, since exporters use different types for the same publication.
* Entries without any key are compared with every other entry (streamed lazily to avoid quadratic memory).
* Blocks of fuzzy keys larger than `max(100, n/50)` are dropped: such a key (e.g., all titles starting with "The") carries no identifying signal, and true duplicates still meet through their other, aligned keys. Blocks of exact identifier keys are never dropped, because a shared identifier alone already decides the duplicate question.

### Consequences

* Good, because measured on a 3060-entry corpus, candidate pairs drop from 4,680,270 to 560 (realistic vocabulary) while every duplicate pair the exhaustive search finds is retained.
* Good, because verification can start while candidates are still being generated, and cancellation is preserved.
* Bad, because blocking is not lossless in theory: for libraries above the exhaustive limit, a duplicate pair is missed if all leading title words and author last names simultaneously differ as near-similar variants with no shared identifier. This is documented and covered by recall tests.

## More Information

* Blocking as a technique: Sample et al., Science Advances 2021, `doi:10.1126/sciadv.abi8021`
* Blocking rule references: [ASySD](https://github.com/camaradesuk/ASySD), [bib-dedupe](https://github.com/CoLRev-Environment/bib-dedupe) (evidence only; not ported)
* Requirement: `req~logic.duplicates.candidate-blocking~1` in [docs/requirements/duplicates.md](../requirements/duplicates.md)
