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

## New journal reload requests must cancel obsolete loads
`req~journals.cancel-obsolete-reloads~1`

Queued requests superseded by a newer request skip repository loading.
At most one reload per repository constructs journal and LTWA data at a time.
A library read that cannot stop immediately may finish, but its obsolete result is never published.
Existing lookup data remains available until the latest request completes successfully.

Needs: impl, utest

## Journal repository loads must reuse bundled LTWA data within a process
`req~journals.reuse-ltwa~1`

Bundled LTWA data is initialized lazily and shared by journal repositories, including across list-state changes.
Concurrent requests perform at most one initialization at a time and see only a fully constructed repository.
Failed initialization does not cache an empty fallback; a later request can retry.
Journal lists and custom abbreviations remain independent between repositories.

Needs: impl, utest

## Journal reloads must not read CSV files while the same repository rewrites them
`req~journals.coordinate-csv-access~1`

Repository construction and CSV writes share a background-only lock.
Generation checks still reject superseded requests.
Save callbacks and repository publication run after releasing the I/O lock, so callbacks can schedule further work without holding it.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
