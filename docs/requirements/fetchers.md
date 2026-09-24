---
parent: Requirements
---
# Fetchers

## Fetchers must respect provider request limits
`req~fetchers.rate-limiting~1`

Fetchers with a documented request limit throttle requests across all fetcher instances. Limits expressed as requests per time interval are converted consistently to requests per second.

Needs: impl

## Crossref fetcher must use polite pool when email is configured
`req~fetchers.crossref-polite-pool~1`

When an email address is configured for Crossref, requests include it in the `mailto` parameter to use Crossref's polite pool.

Needs: impl

## Identifier fetchers must retry rate-limited requests using exponential backoff
`req~fetchers.identifier-rate-limit-retries~1`

Identifier fetchers retry a request rejected with HTTP 429 using bounded exponential backoff, while preserving all other client errors.

Needs: impl

## Journal information popup must retrieve metadata from public sources
`req~fetchers.journal-information~1`

The journal-information popup retrieves journal identity information and metrics directly from public metadata providers.

Needs: impl

## XML importers and fetchers must disable external entity resolution
`req~fetchers.xml-xxe-prevention~1`

MODS and Medline XML imports and PICA, MARC, ISIDORE, and arXiv XML fetcher responses disable DTD processing so that external entities cannot be resolved.

## JabRef must create entry from arbitrary URL
`req~fetchers.generic-url~1`

The user can enter an arbitrary URL to create an entry from it. JabRef tries URL-based fetchers first; if none handles the URL, it falls back to creating a `@Misc` entry with the plain URL, using the linked page's title when it can be reached (falling back to the URL itself otherwise) and recording the date the link was added (`urldate`).

Needs: impl

## ScholarAPI fetcher must download full text when available
`req~fetchers.scholarapi-fulltext~1`

For entries that contain a ScholarAPI identifier and have a PDF available, JabRef discovers the ScholarAPI PDF endpoint and uses the configured ScholarAPI API key to download the full text.

## Full text search must run as background task
`req~fetchers.fulltext-background-search~1`

The search for full text documents runs as a background task shown in the status bar with progress and a cancel option, so JabRef stays usable while it runs. Its results are applied to the library the entries were selected in, and are discarded if that library was closed meanwhile.

Needs: impl

## User can fetch entry by Software Heritage identifier (SWHID)
`feat~fetchers.swhid~1`

The user can look up and import bibliography entries by providing a Software Heritage identifier (SWHID).

Needs: impl

## DNB fetcher must search German National Library catalog
`req~fetchers.dnb-search~1`

The user can search the German National Library (DNB) catalog as a search-based
fetcher, retrieving matching bibliographic entries by title, author, or other
search terms.

Needs: impl

## DNB fetcher must look up entries in German National Library by ISBN
`req~fetchers.dnb-isbn-lookup~1`

The user can retrieve a bibliographic entry from the German National Library
(DNB) by providing an ISBN.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
