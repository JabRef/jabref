---
parent: Requirements
---
# Entry Editor

## Entry Editor should show the last entry
`req~entry-editor.keep-showing~1`

The Entry Editor should "always" show a valid entry.

When users search or select a group not containing the entry shown in the Entry Editor, the Entry Editor should keep showing until user select a new entry explicitly.

Needs: impl

## Citations tab should show citation preview on hover
`req~entry-editor.citations.hover-preview~1`

When the user hovers over a citation entry inside the Entry Editor's "Citations" tab, a tooltip containing the entry preview rendered in the current selected style should be displayed.

Needs: impl

## Main tab shows all fields in one scrollable list
`req~entry-editor.main-tab.single-list~1`

The "Main" tab shows the citation key, all required fields of the entry type (even when unset), and every set field of the entry in a single vertically scrolling list with natural row heights. Field order: citation key, required fields (entry-type order), set optional fields (important before secondary, each in entry-type order), remaining set fields sorted by name, then fields added by the user that are still empty.

Needs: impl

## Fields are grouped into collapsible sections
`req~entry-editor.main-tab.sections~1`

Identifier fields (DOI, ISBN, ISSN, eprint variants, PMID, MR number), file and link fields (file, URL, URI, urldate), bibliometrics fields (citation count, ICORE ranking), comment fields (comment plus user-specific comment fields), and meta fields (crossref, groups, owner, timestamps, special fields — data about the library entry rather than the paper) are shown in their own always-present, collapsible sections in this order after the main fields. A section is collapsed by default when it contains no shown field and expanded when it contains at least one; a manual expand/collapse by the user survives rebuilds until another entry is opened.

Needs: impl

## Unset optional fields are offered as one-click chips
`req~entry-editor.main-tab.add-chips~1`

The entry type's unset important-optional fields that belong to the main group are offered as one-click "+" chips directly below the main fields; a "Show more" toggle reveals chips for the unset secondary-optional fields ("Show less" hides them again). Clicking a chip shows an empty, focused editor for that field, removes the chip, and keeps the field visible — even while still empty — until another entry is opened.

Needs: impl

## Each section offers chips for its unset member fields
`req~entry-editor.main-tab.section-chips~1`

Every section offers "+" chips for its unset member fields: the identifiers section collects all identifier fields, the files and links and bibliometrics sections their respective fields, the comments section the general comment plus the current user's personal comment field (only when user-specific comment fields are enabled), and the meta section crossref, groups, owner, and the special fields (ranking, priority, read status, quality, relevance, printed). The automatically managed timestamp fields have no chip.

Needs: impl

## Arbitrary fields can be added via a field-name box
`req~entry-editor.main-tab.free-form-add~1`

Below the sections, an editable combo box pre-filled with all known field names plus an "Add" button (Enter works as well) adds an editor for any field name; unknown names create a custom field. Blank input is ignored.

Needs: impl

## The list refreshes on external field changes without disturbing typing
`req~entry-editor.main-tab.live-refresh~1`

When fields of the shown entry are set or unset outside the Main tab (source tab, fetchers, undo), the list updates to reflect the new field set. Typing inside a visible editor never rebuilds the list or steals focus; a visible field whose content is deleted stays visible until another entry is opened.

Needs: impl

## A focused, empty, non-required field can be removed from the list
`req~entry-editor.main-tab.remove-field~1`

A field's row shows a small gray "remove field" icon button pinned to its top-right corner while the field's editor is focused and the field is currently blank. Clicking it hides the row again. The citation key and the entry type's required fields never show this button, so they cannot be removed this way.

Needs: impl

## Special fields are edited with the same icon controls as the main table
`req~entry-editor.special-field-editors~1`

Special fields (ranking, priority, read status, printed, quality, relevance) are edited with the same icon-based controls the main table's special field columns use: a five-star rating for the ranking, one icon toggle per value for priority and read status (deselecting the active toggle clears the field), and a single icon toggle for the one-value fields printed, quality, and relevance. Field labels and add-chips show the localized special field name instead of the raw field name. Values changed elsewhere (main table, source tab, undo) are reflected live.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
