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

The "Main" tab shows the citation key, all required fields (even when unset), and every set field of the entry in a single vertically scrolling list with natural row heights. Identifier, file/link, bibliometrics, comment, and meta fields are grouped into always-present collapsible sections that are collapsed when they contain no field.

Needs: impl

## Main tab offers one-click adding of fields
`req~entry-editor.main-tab.add-chips~1`

Unset optional fields of the entry type are offered as one-click chips below the main fields ("Show more" reveals the secondary-optional ones); each section offers chips for its unset member fields. A free-form field-name box adds arbitrary fields. A field added this way shows an empty, focused editor and stays visible until another entry is opened.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
