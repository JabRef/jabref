# File Transfer Between Bib Entries

*Note:*  
"Reachable" here denotes that the linked file can be accessed via a relative path that does **not** climb up the directory structure (i.e., no "`..`" segments beyond the root directory).  
Additionally, this check respects all configured **directories for files** as defined in JabRef's file linking settings (see [directories for files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files)).

## A reachable file should be linked (and not copied)
`req~logic.externalfiles.file-transfer.reachable-no-copy~1`

When a linked file is reachable from the target context, the system must adjust the relative path in the target entry but must not copy the file again.

Needs: impl

## A non-reachable file should keep the relative path
`req~logic.externalfiles.file-transfer.not-reachable-same-path~1`

When a linked file is not reachable from the target context, the relative path within the source library should be kept in the target library.
As a consequence, the file is copied.

Needs: impl

## Auto-link broken linked file
`req~logic.externalfiles.file-transfer.auto-link~1`

After a file is linked to an entry, the user might move the file to another directory without JabRef, leading to broken linked file.

The function `Quality -> Automatically set file links` can help user to auto-link the moved files based on the broken file name, or the entry citation key.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
