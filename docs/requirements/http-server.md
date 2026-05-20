---
parent: Requirements
---
# HTTP Server

## Cross-library search
`req~jabsrv.query.search~1`

The HTTP server exposes `POST /libraries/query` accepting `{ "queries": ["…"] }`, where each query is a Search.g4 expression.
Each query is run independently against all open libraries and returns the matching entries.

Results are returned in the same order as the input queries, so a caller matching a list of references can align the n-th query with the n-th reference.
An entry that matches in more than one library produces one match per library.

Needs: impl, utest

## List library groups
`req~jabsrv.groups.list~1`

The HTTP server exposes `GET /libraries/{id}/groups` returning the groups of the library as a flat, depth-first pre-order list.

Each group carries its name and its breadcrumb path from the top-level group down to and including itself.
The root `AllEntriesGroup` is not part of the result.
Group names are unique within a library, so the name identifies the group.

Needs: impl, utest

## Import entries into a group
`req~jabsrv.import.group~1`

`POST /libraries/{id}/entries` accepts an optional `group` query parameter naming a group the imported entries are additionally assigned to.
If no group with that name exists, it is created as a top-level group.
JabRef merges the entries into the library, so the regular duplicate handling applies.

This is currently available in GUI mode only (the import is dispatched through the GUI message handler).

TODO: if the named group exists but is not assignable (e.g. a search or automatic group), the assignment silently does nothing and no error is reported. Rejecting such requests is not yet implemented.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
