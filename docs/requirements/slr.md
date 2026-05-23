---
parent: Requirements
---

# Systematic Literature Reviews (SLR)
`feat~slr~1`

JabRef supports researchers conducting Systematic Literature Reviews by letting them define a study and crawl multiple academic catalogs from a single specification. The study definition lives in `study.yml`; crawl results are persisted to a Git-tracked repository for reproducibility.

This document captures requirements for SLR functionality. Architectural decisions are recorded in linked ADRs.

## Requirements sources

- GitHub issue track: [Issues](https://github.com/users/koppor/projects/2/views/1)
- Foundational: [New study.yml format](https://github.com/JabRef/jabref/issues/12642)
- Background paper: Dominik Voigt, Oliver Kopp, and Karoline Wild [Systematic Literature Tools: Are we there yet?](https://ceur-ws.org/Vol-2839/paper13.pdf)
- [ADR 0015](../decisions/0015-support-an-abstract-query-syntax-for-query-conversion.md): Lucene-style abstract query syntax
- [ADR 0021](../decisions/0021-keep-study-as-a-dto.md): `Study` as a DTO

## Per-catalog query overrides
`req~slr.per-catalog-query-override~1`

Covers: `feat~slr~1`

When defining a study, a researcher shall be able to provide a query in the native syntax of a specific catalog. The system shall send that query unchanged to the catalog, without translating it through JabRef's abstract query syntax.

Catalogs without a per-catalog query shall continue to receive the global study query, translated for that catalog.

Issue: [#12642](https://github.com/JabRef/jabref/issues/12642)

## Fetcher raw-query execution
`req~slr.fetcher-raw-execution~1`

Covers: `feat~slr~1`

Each catalog fetcher shall provide a way to execute a query exactly as written, without applying JabRef's abstract-query translation. Queries written in JabRef's abstract syntax shall continue to be translated for the target catalog before execution.

> Implementation in progress. Catalogs are migrated incrementally.

## Lock file for reproducibility
`req~slr.lock-file~1`

Covers: `feat~slr~1`

After each crawl, the system shall record the exact query sent to each catalog so the crawl can be reproduced. The record shall be machine-readable and produce identical content when the same study is crawled again with no changes.

> Planned. Not yet implemented.

## First implementation step

The first PR is small on purpose. It sets up the new fetcher pattern on one catalog before we migrate the rest.

**Interface change on `SearchBasedFetcher`:**

- Add `performRawSearch(String)` as a default method that throws `UnsupportedOperationException`. This is the new method that actually calls the API. Fetchers implement it to send the query string as is, without running it through the transformer.
- Change `performSearch(BaseQueryNode)` from abstract to a default wrapper. The wrapper runs `DefaultQueryTransformer` on the AST and then calls `performRawSearch`. Fetchers that implement `performRawSearch` no longer need their own `performSearch(BaseQueryNode)`.

**IEEE migration:**

We migrate IEEE first to prove the pattern works. Its current `getURLForQuery(BaseQueryNode, int)` logic moves into `performRawSearch`. The raw string goes into the `querytext` URL parameter using `URIBuilder.addParameter`, which handles encoding. We then remove the old override so the new wrapper takes over.

All existing IEEE tests must still pass. That is how we confirm the wrapper behaves the same as before for callers that use the abstract syntax. New tests cover `performRawSearch` directly.

**What the first PR does not include:**

The first PR does not connect `catalogSpecific` to the fetchers yet. Until that work lands, nothing in production calls `performRawSearch`, only the tests do. This is on purpose. We want to confirm the interface change works on its own before SLR routing depends on it.

**After the first PR:**

The next PRs migrate Scopus, Springer, and ACM, one fetcher per PR. After that, we connect `catalogSpecific` through `StudyRepository` and `StudyFetcher` so overrides reach the dispatch point. Lock file generation comes next. The other 25 fetchers (`CrossRef`, `DBLP`, `OpenAlex`, and so on) are migrated one by one in later PRs. Each one keeps throwing `UnsupportedOperationException` until its own migration PR lands.

> In progress.

<!-- markdownlint-disable-file MD022 -->
