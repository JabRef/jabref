---
parent: Requirements
---

# Systematic Literature Reviews (SLR)
`feat~slr~1`

Needs: req

JabRef supports researchers conducting Systematic Literature Reviews by letting them define a study and crawl multiple academic catalogs from a single specification. The study definition lives in `study.yml`; crawl results are persisted to a Git-tracked repository for reproducibility.

This document captures requirements for SLR functionality. Architectural decisions are recorded in linked ADRs.

## Requirements sources

- GitHub issue track: [Issues](https://github.com/users/koppor/projects/2/views/1)
- Foundational: [New study.yml format](https://github.com/JabRef/jabref/issues/12642)
- Background paper: Dominik Voigt, Oliver Kopp, and Karoline Wild [Systematic Literature Tools: Are we there yet?](https://ceur-ws.org/Vol-2839/paper13.pdf)
- [ADR 0015](../decisions/0015-support-an-abstract-query-syntax-for-query-conversion.md): Lucene-style abstract query syntax
- [ADR 0021](../decisions/0021-keep-study-as-a-dto.md): `Study` as a DTO

## SLR must support per-catalog query overrides
`req~slr.per-catalog-query-override~1`

When defining a study, a researcher must be able to provide a different query for each catalog, written in that catalog's native syntax.

The system must use the per-catalog query for catalogs where one is provided, and fall back to the global study query for the rest.

Issue: [#12642](https://github.com/JabRef/jabref/issues/12642)

Covers:

- feat~slr~1

## SLR must execute raw queries against catalogs without abstract syntax translation
`req~slr.fetcher-raw-execution~1`

The system must be able to execute a query against any catalog without translating it through JabRef's abstract query syntax.

This applies to all catalogs the system supports, regardless of whether the researcher has provided a per-catalog query for that catalog in their study.

> Implementation in progress. Catalogs are migrated incrementally.

Covers:

- feat~slr~1

## SLR crawl must produce a machine-readable lock file for reproducibility
`req~slr.lock-file~1`

Needs: impl

After each crawl, the system must record the exact query sent to each catalog so the crawl can be reproduced. The record must be machine-readable and produce identical content when the same study is crawled again with no changes.

Covers:

- feat~slr~1

<!-- markdownlint-disable-file MD022 -->
