---
nav_order: 80
parent: Decision Records
---
# Use compact consistency snapshots
`adr~compact-consistency-snapshots~1`

Needs: impl

## Context and Problem Statement

Consistency checks must preserve results after library edits and allow recomputing reports for entries cited in an AUX file. Copying every `BibEntry` also allocates event buses, observable properties, maps, and caches. Keeping complete cell matrices for both the full library and its cited subset increases retained memory further in large libraries.

## Considered Options

* Copy the complete entry model and materialize every report cell.
* Read live entries whenever a cell or export is requested.
* Capture compact immutable field-presence metadata and derive report cells when requested.

## Decision Outcome

Chosen option: "Capture compact immutable field-presence metadata and derive report cells when requested", because it preserves stable reports without a second entry model and without one cell reference per row and column.

Use compact immutable metadata for field presence, alias presence, entry type, citation key, and captured ordering. Keep original entry references solely for navigation. Reports share the snapshot, retain their selected rows and column metadata, and derive symbols on demand. Exports stream those same captured facts rather than reading edited entries.

File loads use cooperative cancellation so superseded requests stop processing and do not retain unnecessary reports. Closing the dialog cancels pending loads while accepted exports can finish.

This preserves stable reports without allocating a second model for the library or retaining one cell reference for every row and column. Equivalence tests compare the compact reports with the existing checker and export writers.

<!-- markdownlint-disable-file MD022 -->
