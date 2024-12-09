---
nav_order: 29
parent: Decision Records
---
# Exporting multiple entries to CFF

## Context and Problem Statement

The need for an [exporter](https://github.com/JabRef/jabref/issues/10661) to [CFF format](https://github.com/citation-file-format/citation-file-format/blob/main/schema-guide.md) raised the following issue: How to export multiple entries at once? Citation-File-Format is intended to make software and datasets citable. It should contain one "main" entry of type `software` or `dataset`, a possible preferred citation and/or several references of any type.

## Decision Drivers

* Make exported files compatible with official CFF tools
* Make exporting process logical for users

## Considered Options

* When exporting:
  * Export non-`software` entries with dummy topmost `sofware` and entries as `preferred-citation`
  * Export non-`software` entries with dummy topmost `sofware` and entries as `references`
  * Forbid exporting multiple entries at once
  * Forbid exporting more than one software entry at once
  * Export entries in several files (i.e. one / file)
  * Export several `software` entries with one of them topmost and all others as `references`
* Export several `software` entries with a dummy topmost `software` element and all others as `references`
* When importing:  
  * Only create one entry / file, enven if there is a `preferred-citation` or `references`
  * Add a JabRef `cites` relation from `software` entry to its `preferred-citation`
  * Add a JabRef `cites` relation from `preferred-citation` entry to the main `software` entry
  * Separate `software` entries from their `preferred-citation` or `references`

## Decision Outcome

The decision outcome is the following.

* When exporting, JabRef will have a different behavior depending on entries type.
  * If multiple non-`software` entries are selected, then exporter uses the `references` field with a dummy topmost `software` element.
  * If several entries including a `software` or `dataset` one are selected, then exporter uses this one as topmost element and the others as `references`, adding a potential `preferred-citation` for the potential `cites` element of the topmost `software` entry.
  * If several entries including several `software` ones are selected, then exporter uses a dummy topmost element, and selected entries are exported as `references`. The `cites` or `related` fields won't be exported in this case.
  * JabRef will not handle `cites` or `related` fields for non-`software` elements.
* When importing, JabRef will create several entries: one main entry for the `software` and other entries for the potential `preferred-citation` and `references` fields. JabRef will link main entry to the preferred citation using a `cites` from the main entry, and wil link main entry to the references using a `related` from the main entry.

### Positive Consequences

* Exported results comply with CFF format
* The export process is "logic" : an user who exports multiple files to CFF might find it clear that they are all marked as `references`
* Importing a CFF file and then exporting the "main" (software) created entry is consistent and will produce the same result

### Negative Consequences

* Importing a CFF file and then exporting one of the `preferred-citation` or the `references` created entries won't result in the same file (i.e exported file will contain a dummy topmost `software` instead of the actual `software` that was imported)
* `cites` and `related` fields of non-`software` entries are not supported
