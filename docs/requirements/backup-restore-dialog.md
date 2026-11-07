---
parent: Requirements
---
# Backup restore dialog

## Restore dialog must compare the library with its backup
`req~jabgui.backup.restore-dialog-comparison~1`

When JabRef finds a newer backup of a library, the "Backup found" dialog shows the current file and the latest backup side by side in a table: file name (with the backup file name linking to its folder), size, date modified, number of entries, and the entry changes from the current file to the backup (added, removed, modified, or "No entry changes" when none changed).
A value that cannot be determined reliably, for example because a file cannot be read or parsed (such as when it contains unresolved merge-conflict markers), is shown as "–" instead of a misleading number.
An empty backup is valid and reports zero entries.

Rationale: Users need to see how the backup differs before deciding whether to restore, review or ignore it.

Needs: impl, utest

## Restore dialog must not block while comparing
`req~jabgui.backup.restore-dialog-non-blocking~1`

Entry counts and changes are computed in a background task with `BackupComparator`, so the dialog opens immediately on large libraries and shows "Calculating..." until the results are available (or "–" if the comparison fails).
Closing the dialog cancels the computation.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
