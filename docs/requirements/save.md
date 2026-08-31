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

## PDF metadata writes replace the file atomically
`req~logic.xmp.atomic-pdf-write~1`

Writing metadata into a PDF (XMP metadata, embedded bib file, or metadata removal) must never rewrite the original file in place.
The new content is written to a temporary file (preferably in the same directory as the target) and then moved over the original, atomically where the filesystem supports it, so concurrent readers such as file synchronization tools never observe a partially written PDF.
A failed write must leave the original file untouched and must not leave temporary files behind.
Exception: a target with multiple hard links is overwritten in place (non-atomically, matching the pre-existing behavior), because an atomic move would detach it from its sibling links.

Needs: impl, utest

## Failed backup writes do not replace recoverable files
`req~jabgui.autosaveandbackup.complete-backup~1`

When creating a backup, a serialization failure must not replace a previous backup with incomplete content. JabRef must not restore an empty backup over an existing library.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
