---
parent: Requirements
---
# Saving a Library

## Concurrent modification of the library file is detected on save
`req~logic.exporter.concurrent-save-detection~1`

Two processes (e.g., two JabRef instances on different machines using a network share) may write the same library file at overlapping times.
When the target file is modified by another process between the start of a save and its commit, the save must be aborted with an error instead of silently overwriting the other process's changes.
The detection is best-effort: it is based on the file's size and modification time, so it is subject to the file system's timestamp resolution.

Needs: impl, utest

## Failed backup writes do not replace recoverable files
`req~jabgui.autosaveandbackup.complete-backup~1`

When creating a backup, a serialization failure must not replace a previous backup with incomplete content. JabRef must not restore an empty backup over an existing library.

Needs: impl, utest

## A backup differing only in modification dates is not offered for restoring
`req~jabgui.autosaveandbackup.ignore-modification-date~1`

When JabRef opens a library and finds a newer backup, it must not ask the user to restore that backup if the only difference between the two is the automatically maintained `modificationdate` of entries, since restoring it would change nothing the user cares about.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
