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

## Backup reacts to every library change
`req~jabgui.autosaveandbackup.backup-listens~1`

While backups are enabled for a library, every change to the library must lead to a backup shortly afterwards, including changes the change filter marks as minor (a single typed character in a field the user is still editing).
A library must not stay without backup until the user moves on to another field.

Needs: impl, utest

## Accepted backup content is preserved until saved
`req~jabgui.autosaveandbackup.backup-merge-modified~1`

When the user accepts or merges changes from a backup while opening a library, the resulting library differs from the file on disk.
The library must open as modified, so that closing it asks the user to save and the accepted content is not lost.

Needs: impl, utest

## Autosave reacts to library changes
`req~jabgui.autosaveandbackup.autosave-listens~1`

While autosave is enabled for a library, every change to the library must lead to the library being saved shortly afterwards without user interaction.

When the autosave manager is shut down, its periodic task must stop and pending callbacks must not post further autosave events.

Needs: impl, utest

## Keyword delimiter normalization is a cleanup
`req~save.keywords.normalize-delimiters~1`

Rewriting a keyword field from an accepted import delimiter to the library's keyword separator is a field formatter cleanup ("Normalize keyword delimiters").
It is part of the default save actions and available in the cleanup dialog, so users see it, can disable it per library, and can run it on demand.
A field that already uses the library's separator is returned unchanged, including its spacing.
Cleanups that depend on the keyword separator use the separator declared by the library the entry belongs to and fall back to the global preference when the library declares none.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
