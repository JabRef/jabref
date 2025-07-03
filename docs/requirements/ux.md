---
parent: Requirements
---
# UX

This page collects general UX requirements

## Auto close of merge entries dialog
`req~ux.auto-close.merge-entries~1`

The merge entries dialog collects and merges data from multiple sources.
In case there is only one source, it should not be shown.
Since some data fetchers take time, we need to open the dialog and wait until all sources are available.
[As soon as only one source is available, the dialog should be closed to speed up the user's workflow](https://github.com/JabRef/jabref/issues/13262).

Needs: impl

<!-- markdownlint-disable-file MD022 -->
