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

## Keyword delimiter normalization is a cleanup
`req~save.keywords.normalize-delimiters~1`

Rewriting a keyword field from an accepted import delimiter to the library's keyword separator is a field formatter cleanup ("Normalize keyword delimiters").
It is part of the default save actions and available in the cleanup dialog, so users see it, can disable it per library, and can run it on demand.
A field that already uses the library's separator is returned unchanged, including its spacing.
Cleanups that depend on the keyword separator use the separator declared by the library the entry belongs to and fall back to the global preference when the library declares none.

Needs: impl, utest
