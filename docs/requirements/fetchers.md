---
parent: Requirements
---
# Fetchers

## Respect provider request limits
`req~fetchers.rate-limiting~1`

Fetchers with a documented request limit throttle requests across all fetcher instances. Limits expressed as requests per time interval are converted consistently to requests per second.

Needs: impl

## Reject external entities in XML responses
`req~fetchers.xml-xxe-prevention~1`

MODS and Medline XML imports and PICA, MARC, ISIDORE, and arXiv XML fetcher responses disable DTD processing so that external entities cannot be resolved.

## Create an entry from an arbitrary URL
`req~fetchers.generic-url~1`

The "New Entry" dialog's "Enter URL" tab accepts any URL and creates a `@Misc` entry for it, using the linked page's title when it can be reached (falling back to the URL itself otherwise) and recording the date the link was added (`urldate`). This does not attempt to recognize the URL's shape (e.g. a DOI or other identifier embedded in it) — it is the generic fallback for any URL not handled by a more specific fetcher.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
