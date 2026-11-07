---
parent: Requirements
---
# HTTP Server

## HTTP server must support cross-library search via query endpoint
`req~jabsrv.query.search~1`

The HTTP server exposes `POST /libraries:query` accepting `{ "queries": ["…"] }`, where each query is a Search.g4 expression.
Each query is run independently against all open libraries and returns the matching entries.

Results are returned in the same order as the input queries, so a caller matching a list of references can align the n-th query with the n-th reference.
An entry that matches in more than one library produces one match per library.

Needs: impl, utest

## HTTP server must return library groups as pre-order list
`req~jabsrv.groups.list~1`

The HTTP server exposes `GET /libraries/{id}/groups` returning the groups of the library as a flat, depth-first pre-order list.

Each group carries its name and its breadcrumb path from the top-level group down to and including itself.
The root `AllEntriesGroup` is not part of the result.
Group names are unique within a library, so the name identifies the group.

Needs: impl, utest

## HTTP server must allow importing entries directly into group
`req~jabsrv.import.group~1`

`POST /libraries/{id}/entries` accepts an optional `group` query parameter naming a group the imported entries are additionally assigned to.
If no group with that name exists, it is created as a top-level group.
JabRef merges the entries into the library, so the regular duplicate handling applies.

This is currently available in GUI mode only (the import is dispatched through the GUI message handler).

TODO: if the named group exists but is not assignable (e.g. a search or automatic group), the assignment silently does nothing and no error is reported. Rejecting such requests is not yet implemented.

Needs: impl

## HTTP server must list the local PDF files of a single entry
`req~jabsrv.entry.files.list~1`

The HTTP server exposes `GET /libraries/{id}/entries/{citationKey}/files` returning the linked files of that entry as a list of `fileName`, `parentCitationKey` and `path`.
Only files of type PDF are returned; online links are skipped.
An unknown citation key yields `404`.

The files are served per entry (and not for the whole library at once), because in [JabMap](https://github.com/JabRef/jabmap), PDF nodes are attached to the BibEntry node they belong to.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
