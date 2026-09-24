---
parent: Requirements
---
# File Transfer Between Bib Entries

*Note:*  
"Reachable" here denotes that the linked file can be accessed via a relative path that does **not** climb up the directory structure (i.e., no "`..`" segments beyond the root directory).  
Additionally, this check respects all configured **directories for files** as defined in JabRef's file linking settings (see [directories for files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files)).

## File transfer must link reachable file without copying
`req~logic.externalfiles.file-transfer.reachable-no-copy~1`

When a linked file is reachable from the target context, the system must adjust the relative path in the target entry but must not copy the file again.

Needs: impl

## File transfer must preserve relative path and copy non-reachable file
`req~logic.externalfiles.file-transfer.not-reachable-same-path~1`

When a linked file is not reachable from the target context, the relative path within the source library should be kept in the target library.
As a consequence, the file is copied.

Needs: impl

## Automatic file linking must resolve broken file links
`req~logic.externalfiles.file-transfer.auto-link~1`

The function `Quality -> Automatically set file links` relinks moved files based on the broken file name or the entry citation key.

Rationale:

Users frequently move or organize files on disk outside of JabRef, leaving behind broken file links that need automated recovery.

Needs: impl, utest

## Unlinked-files search must display results without blocking tree scrolling
`req~jabgui.externalfiles.unlinked-files.search.non-blocking-results~1`

When searching for unlinked local files, related entries must be resolved before displaying the results.
Rendering or scrolling the result tree must not perform file-system searches.

Needs: impl, utest

## PDF preview pane in unlinked-files dialog must allow closing and release resources
`req~jabgui.externalfiles.unlinked-files.preview.close~1`

The user can close the PDF preview side pane in the unlinked-files dialog and show it again when needed.
Closing the side pane releases the displayed PDF document.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
