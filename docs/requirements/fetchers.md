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

## User can view journal information from public metadata sources
`feat~fetchers.journal-information~1`

The journal-information popup retrieves journal identity information and metrics directly from public metadata providers.

Needs: impl

## XML importers and fetchers must disable external entity resolution
`req~fetchers.xml-xxe-prevention~1`

MODS and Medline XML imports and PICA, MARC, ISIDORE, and arXiv XML fetcher responses disable DTD processing so that external entities cannot be resolved.

## User can create entry from arbitrary URL
`feat~fetchers.generic-url~1`

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

## User can search German National Library catalog
`feat~fetchers.dnb-search~1`

The user can search the German National Library (DNB) catalog as a search-based
fetcher, retrieving matching bibliographic entries by title, author, or other
search terms.

Needs: impl

## DNB fetcher must look up entries in German National Library by ISBN
`req~fetchers.dnb-isbn-lookup~1`

The user can retrieve a bibliographic entry from the German National Library
(DNB) by providing an ISBN.

Needs: impl

## User can search BibSonomy
`feat~fetchers.bibsonomy~1`

The user can search BibSonomy from the web search panel and import the found entries. The user enters their BibSonomy username and API key as `username:apikey` in the web search preferences. Without them, JabRef shows a message asking for these credentials.

Needs: impl, utest

## Full text downloads must preserve ambiguous attachments
`req~fetchers.fulltext-attachment-identity~1`

Automatic replacement requires either a sole PDF attachment with no description or a unique attachment whose source URL matches the fetched article URL. Other attachments are preserved, including missing supplements.

Needs: impl, utest

## Full text downloads must persist discovered DOIs
`req~fetchers.fulltext-doi-persistence~1`

A DOI discovered during full text lookup is applied on the JavaFX thread with undo support, only if the original entry has not changed during lookup.

Needs: impl, utest

## Failed full text lookup must remove an unambiguous stale PDF link when no DOI is available
`req~fetchers.fulltext-stale-link~1`

Only the sole undescribed local PDF link is removed, after its absence was checked in the background. The removal is undoable. Entries edited or removed during lookup are preserved.

Origin: <https://github.com/JabRef/jabref-koppor/issues/380#issuecomment-714070246>

Needs: impl, utest

## Duplicate full text downloads must link the surviving file
`req~fetchers.fulltext-duplicate-replacement~1`

When a replacement download duplicates an existing file, the missing attachment is replaced with a link to that surviving file. Undo restores the previous attachment.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
