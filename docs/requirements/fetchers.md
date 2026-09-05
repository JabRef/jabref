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

## Create an entry from an arbitrary URL
`req~fetchers.generic-url~1`

The user can enter an arbitrary URL to create an entry from it. JabRef tries URL-based fetchers first; if none handles the URL, it falls back to creating a `@Misc` entry with the plain URL, using the linked page's title when it can be reached (falling back to the URL itself otherwise) and recording the date the link was added (`urldate`).

Needs: impl

## Download full text from ScholarAPI
`req~fetchers.scholarapi-fulltext~1`

For entries that contain a ScholarAPI identifier and have a PDF available, JabRef discovers the ScholarAPI PDF endpoint and uses the configured ScholarAPI API key to download the full text.

## Fetch entry by Software Heritage identifier (SWHID)
`req~fetchers.swhid~1`

The user can look up and import bibliography entries by providing a Software Heritage identifier (SWHID).
<!-- markdownlint-disable-file MD022 -->
