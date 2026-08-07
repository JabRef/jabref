---
parent: Requirements
---
# Moving Linked Files Between Configured Directories

JabRef supports up to four directory types for storing linked files (see [Directories for Files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks#directories-for-files)):

| ID  | Name                            | Scope                                                                                 |
|-----|---------------------------------|---------------------------------------------------------------------------------------|
| MD  | Main file directory             | Global preference; current user only                                                  |
| LSD | Library-specific file directory | Per library; all users of that library                                                |
| USD | User-specific file directory    | Per user per library; current user only                                               |
| LD  | Next to library                 | Global preference "store files next to library"; files live alongside the `.bib` file |

Issue: [#12287](https://github.com/JabRef/jabref/issues/12287)

## Context menu shows all configured directories as move targets
`req~jabgui.linked-files.move.context-menu-all-directories~1`

The right-click context menu for a linked file must display one menu item per configured directory type (MD, LSD, USD, LD).
Each item is labelled as follows:

| Directory | Menu label                                   |
|-----------|----------------------------------------------|
| MD        | Move file to main file directory             |
| LSD       | Move file to library-specific file directory |
| USD       | Move file to user-specific file directory    |
| LD        | Move file next to library                    |

Only directories that are actually configured appear in the menu.

TODO Needs: impl, utest

## Context menu disables the item for the file's current directory
`req~jabgui.linked-files.move.context-menu-disable-current~1`

The menu item whose directory contains the linked file must be shown but disabled.
It must not be hidden, so that the user can see which directory the file currently resides in.

TODO Needs: impl, utest

## Context menu is fully disabled when no directory is configured
`req~jabgui.linked-files.move.context-menu-disable-no-config~1`

When no file directory of any type is configured (e.g. an unsaved new library with no preferences set), all move menu items must be disabled.

TODO Needs: impl, utest

## LD is disabled when the library has not been saved to disk
`req~jabgui.linked-files.move.ld-requires-saved-library~1`

The "Move file next to library" item (LD) must be disabled whenever the current library has no file path on disk (i.e. it has never been saved).

TODO Needs: impl, utest

## All selected files move to the same chosen target directory
`req~jabgui.linked-files.move.multi-file-single-target~1`

When multiple linked files are selected and the user invokes a move action, every selected file must be moved to the single target directory the user chose.
The source directory of each individual file is irrelevant to the choice of target.

TODO Needs: impl, utest

## Files already in the target directory are skipped silently
`req~jabgui.linked-files.move.multi-file-skip-existing~1`

If a selected file already resides in the chosen target directory, the system must skip that file without raising an error or displaying a warning.

TODO Needs: impl, utest

## Subdirectory structure is preserved when moving a file
`req~logic.linked-files.move.preserve-subdirectory-structure~1`

When a file is moved between directories, its path relative to the source directory must be reconstructed under the target directory.

Example:

| Item                          | Path                        |
|-------------------------------|-----------------------------|
| Source directory (LSD)        | `E:\lib`                    |
| Target directory (USD)        | `U:\papers`                 |
| Original file path            | `E:\lib\a\b\c\paper.pdf`    |
| Expected file path after move | `U:\papers\a\b\c\paper.pdf` |

TODO Needs: impl, utest

## Alternatives considered for the context menu

Two earlier designs were rejected before settling on "one menu item per configured directory":

* **One dynamic label computed from an algorithm** (e.g. "Move file to [best target]") — rejected because the user cannot choose, and the algorithm (see comments on issue [#12287](https://github.com/JabRef/jabref/issues/12287)) has acknowledged inconsistencies between the two-directory and three-directory cases that are hard to explain to users.
* **A single generic rotating label** ("Move file to next configured directory") — implemented by [PR #15055](https://github.com/JabRef/jabref/pull/15055) but rejected because "next" is opaque: the user must know the rotation order (USD → LSD → MD → USD) to predict where the file lands, and reaching a non-adjacent target requires multiple interactions.

The chosen design — one menu item per configured directory, with the current directory shown but disabled — was selected because behaviour is explicit and discoverable, any target is reachable in one action, and adding or removing a directory configuration is immediately reflected in the menu.

<!-- markdownlint-disable-file MD022 -->
