---
parent: Requirements
---
# HTTP Server

## Cross-library DOI lookup
`req~jabsrv.query.doi~1`

The HTTP server exposes `POST /libraries/query` accepting `{ "dois": ["…"] }` and returns all entries whose DOI field matches any of the requested DOIs, across all open libraries.

DOI normalisation: strip `https://doi.org/` prefix and compare case-insensitively.
Non-matching DOIs are absent from the response.
A DOI that appears in more than one library produces one match per library.

Needs: impl, utest

## Cross-library URL lookup
`req~jabsrv.query.url~1`

The same `POST /libraries/query` endpoint also accepts an optional `"urls": ["…"]` list and returns all entries whose URL field exactly matches any of the requested URLs, across all open libraries.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
