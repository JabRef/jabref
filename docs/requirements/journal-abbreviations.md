---
parent: Requirements
---
# Journal abbreviations

## Journal abbreviation preferences must allow disabling the built-in list without disabling external lists or LTWA
`req~journals.disable-built-in~1`

The built-in list is enabled by default, including when no saved preference exists.
A saved change takes effect without restarting; canceling the preferences dialog leaves the active setting unchanged.
The built-in list remains visible in preferences when disabled.

See <https://github.com/JabRef/jabref/issues/12468>.

Needs: impl, utest

## Journal abbreviation preferences must allow disabling each external list independently
`req~journals.disable-external~1`

Existing and newly added external lists are enabled by default.
Disabling a list excludes its entries from journal lookup without removing the list, changing its precedence, or deleting its file.
Disabled lists remain selectable and editable in preferences.
Saving applies each list's state without restarting and preserves it across restarts; canceling leaves saved states unchanged.
The built-in list remains read-only and cannot be removed.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
