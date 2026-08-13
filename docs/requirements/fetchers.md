---
parent: Requirements
---
# Fetchers

## Respect provider request limits
`req~fetchers.rate-limiting~1`

Fetchers with a documented request limit throttle requests across all fetcher instances. Limits expressed as requests per time interval are converted consistently to requests per second.

Needs: impl

## Use Crossref's polite pool
`req~fetchers.crossref-polite-pool~1`

When an email address is configured for Crossref, requests include it in the `mailto` parameter to use Crossref's polite pool.

Needs: impl

## Retry identifier lookups after rate limiting
`req~fetchers.identifier-rate-limit-retries~1`

Identifier fetchers retry a request rejected with HTTP 429 using bounded exponential backoff, while preserving all other client errors.

Needs: impl

## Retrieve journal information from public sources
`req~fetchers.journal-information~1`

The journal-information popup retrieves journal identity information and metrics directly from public metadata providers.

Needs: impl

## Reject external entities in XML responses
`req~fetchers.xml-xxe-prevention~1`

MODS and Medline XML imports and PICA, MARC, ISIDORE, and arXiv XML fetcher responses disable DTD processing so that external entities cannot be resolved.

<!-- markdownlint-disable-file MD022 -->
