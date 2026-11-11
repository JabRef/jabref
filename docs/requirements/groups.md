---
parent: Requirements
---
# Groups

## Automatic keyword group must offer subgroup for entries without value
`req~groups.automatic-keyword.missing-value-subgroup~1`

An automatic group based on a keyword field (for example, `ranking`) lists one subgroup per value.
Entries that have no value in that field, or only delimiters, appear in an additional subgroup "No value", so they can be found without a manual search.
The subgroup is only shown when at least one entry has no value.

See <https://github.com/JabRef/jabref/issues/10264>.

Needs: impl, utest

## User can assign selected entries to a group using the keyboard only
`feat~groups.assign-entries-by-keyboard~1`

While the main table has the focus, a keyboard shortcut opens a dialog listing the groups that accept entries.
Typing filters the groups by their path, the arrow keys move the selection, and <kbd>Enter</kbd> assigns the selected entries to the selected group.
The dialog preselects the group used last, so that sorting entries one by one during a systematic literature review takes one shortcut and <kbd>Enter</kbd> per entry.
The assignment can be undone in one step.

Selecting a group in the groups panel is no alternative, because it filters the main table to the entries of that group.

See [#10889](https://github.com/JabRef/jabref/issues/10889).

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
