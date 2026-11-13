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

Rationale: Users frequently move or organize files on disk outside of JabRef, leaving behind broken file links that need automated recovery.

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

## Empty file directory fields in library properties must show the effective directory as prompt text
`req~jabgui.libraryproperties.file-directory.effective-prompt~1`

When a directory field in the "General" tab of the library properties is empty, it shows the absolute directory JabRef falls back to as greyed prompt text.
The prompt text is never stored in the library.

- Library-specific file directory: the main file directory or the directory of the `.bib` file, depending on the preference to store files relative to the `.bib` file.
- User-specific file directory: the library-specific file directory as currently entered, otherwise the same directory as for the library-specific file directory.
- LaTeX file directory: the directory of the `.bib` file, otherwise the working directory.

If there is no directory to fall back to (e.g., an unsaved library whose files are stored relative to the `.bib` file), the field stays empty.

[Issue 17572](https://github.com/JabRef/jabref/issues/17572)

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
