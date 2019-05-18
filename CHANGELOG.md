# Changelog
All notable changes to this project will be documented in this file.
This project **does not** adhere to [Semantic Versioning](http://semver.org/).
This file tries to follow the conventions proposed by [keepachangelog.com](http://keepachangelog.com/).
Here, the categories "Changed" for added and changed functionality,
"Fixed" for fixed functionality, and
"Removed" for removed functionality are used.

We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`.

## [Unreleased]

### Changed
- We changed the latex command removal for docbook exporter. [#3838](https://github.com/JabRef/jabref/issues/3838)
- We changed the location of some fields in the entry editor (you might need to reset your preferences for these changes to come into effect)
  - Journal/Year/Month in biblatex mode -> Deprecated (if filled)
  - DOI/URL: General -> Optional
  - Internal fields like ranking, read status and priority: Other -> General
  - Moreover, empty deprecated fields are no longer shown
- Added server timezone parameter when connecting to a shared database.
- We updated the dialog for setting up general fields.
- URL field formatting is updated. All whitespace chars, located at the beginning/ending of the url, are trimmed automatically
- We changed the behavior of the field formatting dialog such that the `bibtexkey` is not changed when formatting all fields or all text fields.
- We added a "Move file to file directory and rename file" option for simultaneously moving and renaming of document file. [#4166](https://github.com/JabRef/jabref/issues/4166)
- Use integrated graphics card instead of discrete on macOS [#4070](https://github.com/JabRef/jabref/issues/4070)
- We added a cleanup operation that detects an arXiv identifier in the note, journal or url field and moves it to the `eprint` field.
  Because of this change, the last-used cleanup operations were reset.
- We changed the minimum required version of Java to 1.8.0_171, as this is the latest release for which the automatic Java update works.  [4093](https://github.com/JabRef/jabref/issues/4093)
- The special fields like `Printed` and `Read status` now show gray icons when the row is hovered.
- We added a button in the tab header which allows you to close the database with one click. https://github.com/JabRef/jabref/issues/494
- Sorting in the main table now takes information from cross-referenced entries into account. https://github.com/JabRef/jabref/issues/2808
- If a group has a color specified, then entries matched by this group have a small colored bar in front of them in the main table.
- Change default icon for groups to a circle because a colored version of the old icon was hard to distinguish from its black counterpart.
- In the main table, the context menu appears now when you press the "context menu" button on the keyboard. [feature request in the forum](http://discourse.jabref.org/t/how-to-enable-keyboard-context-key-windows)
- We added icons to the group side panel to quickly switch between `union` and `intersection` group view mode https://github.com/JabRef/jabref/issues/3269.
- We use `https` for [fetching from most online bibliographic database](https://help.jabref.org/en/#-using-online-bibliographic-database).
- We changed the default keyboard shortcuts for moving between entries when the entry editor is active to ̀<kbd>alt</kbd> + <kbd>up/down</kbd>.
- Opening a new file now prompts the directory of the currently selected file, instead of the directory of the last opened file.
- Window state is saved on close and restored on start.
- We made the MathSciNet fetcher more reliable.
- We added the ISBN fetcher to the list of fetcher available under "Update with bibliographic information from the web" in the entry editor toolbar.
- Files without a defined external file type are now directly opened with the default application of the operating system
- We streamlined the process to rename and move files by removing the confirmation dialogs.
- We removed the redundant new lines of markings and wrapped the summary in the File annotation tab. [#3823](https://github.com/JabRef/jabref/issues/3823)
- We add auto url formatting when user paste link to URL field in entry editor. [koppor#254](https://github.com/koppor/jabref/issues/254)
- We added a minimal height for the entry editor so that it can no longer be hidden by accident. [#4279](https://github.com/JabRef/jabref/issues/4279)
- We added a new keyboard shortcut so that the entry editor could be closed by <kbd>Ctrl</kbd> + <kbd>E</kbd>. [#4222] (https://github.com/JabRef/jabref/issues/4222)
- We added an option in the preference dialog box, that allows user to pick the dark or light theme option. [#4130] (https://github.com/JabRef/jabref/issues/4130)
- We updated updated the Related Articles tab to accept JSON from the new version of the Mr. DLib service
- We added an option in the preference dialog box that allows user to choose behavior after dragging and dropping files in Entry Editor. [#4356](https://github.com/JabRef/jabref/issues/4356)
- We added the ability to have an export preference where previously "File"-->"Export"/"Export selected entries" would not save the user's preference[#4495](https://github.com/JabRef/jabref/issues/4495)
- We optimized the code responsible for connecting to an external database, which should lead to huge improvements in performance.
- For automatically created groups, added ability to filter groups by entry type. [#4539](https://github.com/JabRef/jabref/issues/4539)
- We added the ability to add field names from the Preferences Dialog [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added the ability change the column widths directly in the main table. [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added the ability to execute default action in dialog by using with <kbd>Ctrl</kbd> + <kbd>Enter</kbd> combination [#4496](https://github.com/JabRef/jabref/issues/4496)
- We grouped and reordered the Main Menu (File, Edit, Library, Quality, Tools, and View tabs & icons). [#4666](https://github.com/JabRef/jabref/issues/4666) [#4667](https://github.com/JabRef/jabref/issues/4667) [#4668](https://github.com/JabRef/jabref/issues/4668) [#4669](https://github.com/JabRef/jabref/issues/4669) [#4670](https://github.com/JabRef/jabref/issues/4670) [#4671](https://github.com/JabRef/jabref/issues/4671) [#4672](https://github.com/JabRef/jabref/issues/4672) [#4673](https://github.com/JabRef/jabref/issues/4673)
- We added additional modifiers (capitalize, titlecase and sentencecase) to the Bibtex key generator. [#1506](https://github.com/JabRef/jabref/issues/1506)
- We grouped the toolbar icons and changed the Open Library and Copy icons. [#4584](https://github.com/JabRef/jabref/issues/4584)
- We added a browse button next to the path text field for aux-based groups. [#4586](https://github.com/JabRef/jabref/issues/4586)
- We changed the title of Group Dialog to "Add subgroup" from "Edit group" when we select Add subgroup option.
- We enable import button only if entries are selected. [#4755](https://github.com/JabRef/jabref/issues/4755)
- We made modifications to improve contrast of UI elements. [#4583](https://github.com/JabRef/jabref/issues/4583)
- We added an option in the settings to set the default action in JabRef when right clicking on any entry in any database and selecting "Open folder". [#4763](https://github.com/JabRef/jabref/issues/4763)
- The Medline fetcher now normalizes the author names according to the BibTeX-Standard [#4345](https://github.com/JabRef/jabref/issues/4345)
- We added an option on the Linked File Viewer to rename the attached file of an entry directly on the JabRef. [#4844](https://github.com/JabRef/jabref/issues/4844)
- We added an option in the preference dialog box that allows user to enable helpful tooltips.[#3599](https://github.com/JabRef/jabref/issues/3599)


### Fixed
- We fixed an issue where JabRef died silently for the user without enough inotify instances [#4874](https://github.com/JabRef/jabref/issues/4847)
- We fixed an issue where corresponding groups are sometimes not highlighted when clicking on entries [#3112](https://github.com/JabRef/jabref/issues/3112)
- We fixed an issue where custom exports could not be selected in the 'Export (selected) entries' dialog [#4013](https://github.com/JabRef/jabref/issues/4013)
- Italic text is now rendered correctly. https://github.com/JabRef/jabref/issues/3356
- The entry editor no longer gets corrupted after using the source tab. https://github.com/JabRef/jabref/issues/3532 https://github.com/JabRef/jabref/issues/3608 https://github.com/JabRef/jabref/issues/3616
- We fixed multiple issues where entries did not show up after import if a search was active. https://github.com/JabRef/jabref/issues/1513 https://github.com/JabRef/jabref/issues/3219
- We fixed an issue where the group tree was not updated correctly after an entry was changed. https://github.com/JabRef/jabref/issues/3618
- We fixed an issue where a right-click in the main table selected a wrong entry. https://github.com/JabRef/jabref/issues/3267
- We fixed an issue where in rare cases entries where overlayed in the main table. https://github.com/JabRef/jabref/issues/3281
- We fixed an issue where selecting a group messed up the focus of the main table / entry editor. https://github.com/JabRef/jabref/issues/3367
- We fixed an issue where composite author names were sorted incorrectly. https://github.com/JabRef/jabref/issues/2828
- We fixed an issue where commands followed by `-` didn't work. [#3805](https://github.com/JabRef/jabref/issues/3805)
- We fixed an issue where a non-existing aux file in a group made it impossible to open the library. [#4735](https://github.com/JabRef/jabref/issues/4735)
- We fixed an issue where some journal names were wrongly marked as abbreviated. [#4115](https://github.com/JabRef/jabref/issues/4115)
- We fixed an issue where the custom file column were sorted incorrectly. https://github.com/JabRef/jabref/issues/3119
- We improved the parsing of author names whose infix is abbreviated without a dot. [#4864](https://github.com/JabRef/jabref/issues/4864)
- We fixed an issues where the entry losses focus when a field is edited and at the same time used for sorting. https://github.com/JabRef/jabref/issues/3373
- We fixed an issue where the menu on Mac OS was not displayed in the usual Mac-specific way. https://github.com/JabRef/jabref/issues/3146
- We improved the integrity check for page numbers. [#4113](https://github.com/JabRef/jabref/issues/4113) and [feature request in the forum](http://discourse.jabref.org/t/pages-field-allow-use-of-en-dash/1199)
- We fixed an issue where the order of fields in customized entry types was not saved correctly. [#4033](http://github.com/JabRef/jabref/issues/4033)
- We fixed an issue where renaming a group did not change the group name in the interface. [#3189](https://github.com/JabRef/jabref/issues/3189)
- We fixed an issue where the groups tree of the last database was still shown even after the database was already closed.
- We fixed an issue where the "Open file dialog" may disappear behind other windows. https://github.com/JabRef/jabref/issues/3410
- We fixed an issue where the number of entries matched was not updated correctly upon adding or removing an entry. [#3537](https://github.com/JabRef/jabref/issues/3537)
- We fixed an issue where the default icon of a group was not colored correctly.
- We fixed an issue where the first field in entry editor was not focused when adding a new entry. [#4024](https://github.com/JabRef/jabref/issues/4024)
- We reworked the "Edit file" dialog to make it resizeable and improved the workflow for adding and editing files https://github.com/JabRef/jabref/issues/2970
- We fixed an issue where custom name formatters were no longer found correctly. [#3531](https://github.com/JabRef/jabref/issues/3531)
- We fixed an issue where the month was not shown in the preview https://github.com/JabRef/jabref/issues/3239.
- Rewritten logic to detect a second jabref instance. [#4023](https://github.com/JabRef/jabref/issues/4023)
- We fixed an issue where the "Convert to BibTeX-Cleanup" moved the content of the `file` field to the `pdf` field [#4120](https://github.com/JabRef/jabref/issues/4120)
- We fixed an issue where the preview pane in entry preview in preferences wasn't showing the citation style selected [#3849](https://github.com/JabRef/jabref/issues/3849)
- We fixed an issue where the default entry preview style still contained the field `review`. The field `review` in the style is now replaced with comment to be consistent with the entry editor [#4098](https://github.com/JabRef/jabref/issues/4098)
- We fixed an issue where users were vulnerable to XXE attacks during parsing [#4229](https://github.com/JabRef/jabref/issues/4229)
- We fixed an issue where files added via the "Attach file" contextmenu of an entry were not made relative. [#4201](https://github.com/JabRef/jabref/issues/4201) and [#4241](https://github.com/JabRef/jabref/issues/4241)
- We fixed an issue where author list parser can't generate bibtex for Chinese author. [#4169](https://github.com/JabRef/jabref/issues/4169)
- We fixed an issue where the list of XMP Exclusion fields in the preferences was not be saved [#4072](https://github.com/JabRef/jabref/issues/4072)
- We fixed an issue where the ArXiv Fetcher did not support HTTP URLs [koppor#328](https://github.com/koppor/jabref/issues/328)
- We fixed an issue where only one PDF file could be imported [#4422](https://github.com/JabRef/jabref/issues/4422)
- We fixed an issue where "Move to group" would always move the first entry in the library and not the selected [#4414](https://github.com/JabRef/jabref/issues/4414)
- We fixed an issue where an older dialog appears when downloading full texts from the quality menu. [#4489](https://github.com/JabRef/jabref/issues/4489)
- We fixed an issue where right clicking on any entry in any database and selecting "Open folder" results in the NullPointer exception. [#4763](https://github.com/JabRef/jabref/issues/4763)
- We fixed an issue where option 'open terminal here' with custom command was passing wrong argument. [#4802](https://github.com/JabRef/jabref/issues/4802)
- We fixed an issue where ranking an entry would generate an IllegalArgumentException. [#4754](https://github.com/JabRef/jabref/issues/4754)
- We fixed an issue where special characters where removed from non label key generation pattern parts [#4767](https://github.com/JabRef/jabref/issues/4767)
- We fixed an issue where the RIS import would overwite the article date with the value of the acessed date [#4816](https://github.com/JabRef/jabref/issues/4816)
- We fixed an issue where an NullPointer exception was thrown when a referenced entry in an Open/Libre Office document was no longer present in the library. Now an error message with the reference marker of the missing entry is shown. [#4932](https://github.com/JabRef/jabref/issues/4932)
- We fixed an issue where a database exception related to a missing timezone was too big. [#4827](https://github.com/JabRef/jabref/issues/4827)
- We fixed an issue where the command line help text had several errors, and arguments and descriptions have been rewritten to simplify and detail them better. [#4932](https://github.com/JabRef/jabref/issues/2016)
- We fixed an issue where the same menu for changing entry type had two different sizes and weights. [#4977](https://github.com/JabRef/jabref/issues/4977)


### Removed
- The feature to "mark entries" was removed and merged with the groups functionality.  For migration, a group is created for every value of the `__markedentry` field and the entry is added to this group.
- The number column was removed.
- We removed the global search feature.
- We removed the coloring of cells in the maintable according to whether the field is optional/required.
- We removed the feature to find and resolve duplicate BibTeX keys (as this use case is already covered by the integrity check).
- We removed a few commands from the right-click menu that are not needed often and thus don't need to be placed that prominently:
  - Print entry preview: available through entry preview
  - All commands related to marking: marking is not yet reimplemented
  - Set/clear/append/rename fields: available through Edit menu
  - Manage keywords: available through Edit menu
  - Copy linked files to folder: available through File menu
  - Add/move/remove from group: removed completely (functionality still available through group interface)
- We removed the option to change the column widths in the preferences dialog. [#4546](https://github.com/JabRef/jabref/issues/4546)





























## Older versions

The changelog of JabRef 4.x is available at the [v4.x branch](https://github.com/JabRef/jabref/blob/v4.x/CHANGELOG.md).
The changelog of JabRef 3.x is available at the [v3.8.2 tag](https://github.com/JabRef/jabref/blob/v3.8.2/CHANGELOG.md).
The changelog of JabRef 2.11 and all previous versions is available as [text file in the v2.11.1 tag](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG).

[Unreleased]: https://github.com/JabRef/jabref/compare/v4.3...HEAD
[4.3]: https://github.com/JabRef/jabref/compare/v4.2...v4.3
[4.2]: https://github.com/JabRef/jabref/compare/v4.1...v4.2
[4.1]: https://github.com/JabRef/jabref/compare/v4.0...v4.1
[4.0]: https://github.com/JabRef/jabref/compare/v4.0-beta3...v4.0
[4.0-beta3]: https://github.com/JabRef/jabref/compare/v4.0-beta2...v4.0-beta3
[4.0-beta2]: https://github.com/JabRef/jabref/compare/v4.0-beta...v4.0-beta2
[4.0-beta]: https://github.com/JabRef/jabref/compare/v3.8.2...v4.0-beta
[2.11.1]: https://github.com/JabRef/jabref/compare/v2.11...v2.11.1
[JavaFX]: https://en.wikipedia.org/wiki/JavaFX

