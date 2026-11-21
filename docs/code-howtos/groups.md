---
parent: Code Howtos
---
# Groups

Diagram showing aspects of groups: [Groups.uml](https://github.com/JabRef/jabref/tree/3b3716b1e05a0d3273c886e102a8efe5e96472e0/docs/Groups.uml).

## Editing grouping methods

Changing an explicit group to any other grouping method removes its old name from the entries' `groups` fields.
The old assignment is preserved if another group has the same name.
Other group assignments are preserved.

Cleanup is independent of the choice to assign the original entries to the replacement group:

- **Assign** adds the entries when the replacement supports assignment, potentially changing its target field.
- **Do not assign** changes the group definition and removes obsolete explicit assignments without adding entries to the replacement.
- **Cancel** leaves the group definition and entry assignments unchanged.

Word keyword groups support assignment by adding the required search term to their configured field.
Regular expression, search, automatic, and TeX groups derive membership from their criteria and cannot force entries to match.
Converting a keyword group preserves its old search terms in bibliographic fields, such as `keywords` or `author`.

For example, converting explicit group `1` into a word keyword group searching `groups` for `2`, then choosing **Assign**,
changes `groups = {1}` to `groups = {2}` when the original group name is unique.
Both entry-field changes and the group definition are recorded in one undo step.
