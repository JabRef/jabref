---
parent: Requirements
---
# Backup restore dialog

## Restore dialog must compare the library with its backup
`req~jabgui.backup.restore-dialog-comparison~1`

When JabRef finds a newer backup of a library, the "Backup found" dialog shows the current file and the latest backup side by side in a table: file name (with the backup file name linking to its folder), size, date modified, number of entries, and the entry changes from the current file to the backup (added, removed, modified, or "No entry changes" when none changed).
A value that cannot be determined reliably, for example because a file cannot be read or parsed (such as when it contains unresolved merge-conflict markers), is shown as "–" instead of a misleading number.
An empty backup is valid and reports zero entries.
Below the table, one line explains that restoring overwrites the library file with the backup, while ignoring opens the library file as it is and keeps the backup file.

Rationale: Users need to see how the backup differs before deciding whether to restore, review or ignore it.

Needs: impl, utest

## Comparing for the restore dialog must not block
`req~jabgui.backup.restore-dialog-non-blocking~1`

Entry counts and changes are computed with `BackupComparator` in the task that opens the library, before the dialog is shown, so the comparison never runs on the JavaFX thread.
It does not wait for another task of the shared task pool: when several libraries with backups are opened at once, each loading task waits for its own dialog, and a comparison queued behind them would never finish.
Reviewing the backup reuses the files parsed for the comparison, unless they changed on disk in the meantime.
If opening the library is cancelled, the comparison stops between its steps and no dialog is shown.
A comparison that fails shows "–".

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
