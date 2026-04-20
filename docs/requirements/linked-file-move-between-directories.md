---
parent: Requirements
---
# Moving Linked Files Between Configured Directories

JabRef supports up to four directory types for storing linked files (see [Directories for Files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files)):

| ID | Name | Scope |
|----|------|-------|
| X1 | Main file directory | Global preference; current user only |
| X2 | Library-specific file directory | Per library; all users of that library |
| X3 | User-specific file directory | Per user per library; current user only |
| X4 | Next to library | Global preference "store files next to library"; files live alongside the `.bib` file |

Terminology follows [ADR-0060](../decisions/0060-rename-general-file-directory-to-library-specific-file-directory.md).
Context menu design follows [ADR-0056](../decisions/0056-linked-file-move-context-menu-shows-all-configured-directories.md).
Multi-file behaviour follows [ADR-0057](../decisions/0057-multi-file-move-uses-single-chosen-target-directory.md).
X4 scope follows [ADR-0059](../decisions/0059-next-to-library-is-a-valid-move-target.md).

Issue: [#12287](https://github.com/JabRef/jabref/issues/12287)

## Context menu shows all configured directories as move targets
`req~jabgui.linked-files.move.context-menu-all-directories~1`

The right-click context menu for a linked file must display one menu item per configured directory type (X1–X4).
Each item is labelled as follows:

| Directory | Menu label |
|-----------|-----------|
| X1 | Move file to main file directory |
| X2 | Move file to library-specific file directory |
| X3 | Move file to user-specific file directory |
| X4 | Move file next to library |

Only directories that are actually configured appear in the menu.

Needs: impl, utest

## Context menu disables the item for the file's current directory
`req~jabgui.linked-files.move.context-menu-disable-current~1`

The menu item whose directory contains the linked file must be shown but disabled.
It must not be hidden, so that the user can see which directory the file currently resides in.

Needs: impl, utest

## Context menu is fully disabled when no directory is configured
`req~jabgui.linked-files.move.context-menu-disable-no-config~1`

When no file directory of any type is configured (e.g. an unsaved new library with no preferences set), all move menu items must be disabled.

Needs: impl, utest

## X4 is disabled when the library has not been saved to disk
`req~jabgui.linked-files.move.x4-requires-saved-library~1`

The "Move file next to library" item (X4) must be disabled whenever the current library has no file path on disk (i.e. it has never been saved).

Needs: impl, utest

## All selected files move to the same chosen target directory
`req~jabgui.linked-files.move.multi-file-single-target~1`

When multiple linked files are selected and the user invokes a move action, every selected file must be moved to the single target directory the user chose.
The source directory of each individual file is irrelevant to the choice of target.

Needs: impl, utest

## Files already in the target directory are skipped silently
`req~jabgui.linked-files.move.multi-file-skip-existing~1`

If a selected file already resides in the chosen target directory, the system must skip that file without raising an error or displaying a warning.

Needs: impl, utest

## Subdirectory structure is preserved when moving a file
`req~logic.linked-files.move.preserve-subdirectory-structure~1`

When a file is moved between directories, its path relative to the source directory must be reconstructed under the target directory.

Example:

| | Path |
|--|------|
| Source directory (X2) | `E:\lib` |
| Target directory (X3) | `U:\papers` |
| Original file path | `E:\lib\a\b\c\paper.pdf` |
| Expected file path after move | `U:\papers\a\b\c\paper.pdf` |

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
