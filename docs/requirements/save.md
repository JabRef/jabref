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

## File attributes survive an atomic save
`req~logic.exporter.preserve-file-attributes~1`

An atomic save replaces the target file with a newly created one.
The group, DOS attributes (hidden, system, archive, read-only), ACL, and user-defined extended attributes (e.g., macOS Finder tags) of the previous file must be carried over to the new file.
This is best-effort: an attribute class the file system does not support, or the OS refuses to set (e.g., a group the user is not a member of), is skipped without failing the save. Ownership is not restored, because that requires elevated privileges.

Needs: impl, utest

## Failed backup writes do not replace recoverable files
`req~jabgui.autosaveandbackup.complete-backup~1`

When creating a backup, a serialization failure must not replace a previous backup with incomplete content. JabRef must not restore an empty backup over an existing library.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
