---
nav_order: 83
parent: Decision Records
---

# Share bundled LTWA data
`adr~share-bundled-ltwa~1`

Needs: impl, utest

## Context and Problem Statement

Reloading journal abbreviations reconstructs the bundled LTWA prefix and suffix trees even though list preferences do not change this data.
A JFR profile of the journal tests attributed about 78% of sampled allocation volume to LTWA loading.
LTWA trees are private and populated only by the repository constructor; each abbreviation lookup creates its own parser and listener state.

## Considered Options

* Reconstruct LTWA data for each journal repository.
* Reuse LTWA data only within one journal repository's reloads.
* Lazily share successfully loaded bundled LTWA data within each JVM.

## Decision Outcome

Chosen option: "Lazily share successfully loaded bundled LTWA data within each JVM", because the bundled data is independent of list preferences and remains read-only after construction.

Synchronize initialization so concurrent loads cannot deserialize duplicate copies or observe partially constructed trees.
Keep the successful result for the JVM lifetime, while retaining independent journal-list maps and custom abbreviations in each journal repository.
If initialization fails, preserve the existing fallback behavior for that request and let a later request retry loading.
The shared repository holds no open MVStore or temporary file after construction.

This deliberately retains one LTWA dataset after first use and avoids repeated allocation during preference changes, new repositories, and test setup.
Regression tests cover lazy reuse, concurrent initialization, failure retries, and existing abbreviation results.

<!-- markdownlint-disable-file MD022 MD041 -->
