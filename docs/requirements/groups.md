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

## Editing an explicit group must remove obsolete explicit assignments
`req~groups.edit.remove-obsolete-explicit-assignments~1`

When an explicit group is converted to another group type, its old name is removed from the `groups` field of its entries.
This applies whether the user chooses to assign the original entries to the new group or not.
If another group has the same old name, the assignment is preserved.
Unrelated group assignments are preserved.
Cancelling the edit leaves both the group definition and its entry assignments unchanged.

Assigning entries to the replacement group follows its existing rules: explicit and word keyword groups can modify fields to retain entries;
regular expression, search, automatic, and TeX groups determine membership from their criteria.
Old keyword-group search terms are not removed from bibliographic fields when changing grouping methods.

See <https://github.com/JabRef/jabref/issues/8824#issuecomment-1169216030>.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
