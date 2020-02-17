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
- we changed the open office panel to show buttons in rows of three instead of going straight down to save space as the button expanded out to take up unnecessary horizontal space [#5479] (https://github.com/JabRef/jabref/issues/5479)

- We cleaned up the group add/edit dialog. [#5826](https://github.com/JabRef/jabref/pull/5826)
- We reintroduced the index column. [#5844](https://github.com/JabRef/jabref/pull/5844)
- Filenames of external files can no longer contain curly braces


### Fixed

- We fixed and issue where pdf files will not open under some KDE linux distributions when using okular. [#5253](https://github.com/JabRef/jabref/issues/5253)
- We fixed an issue where the Medline fetcher was only working when JabRef was running from source. [#5645](https://github.com/JabRef/jabref/issues/5645)
- We fixed some visual issues in the dark theme. [#5764](https://github.com/JabRef/jabref/pull/5764) [#5753](https://github.com/JabRef/jabref/issues/5753)
- We fixed an issue where non-default previews didn't handle unicode characters. [#5779](https://github.com/JabRef/jabref/issues/5779)
- We improved the performance, especially changing field values in the entry should feel smoother now.
- We fixed an issue where the ampersand character wasn't rendering correctly on previews. [#3840](https://github.com/JabRef/jabref/issues/3840)
- We fixed an issue where an erroneous "The library has been modified by another program" message was shown when saving. [#4877](https://github.com/JabRef/jabref/issues/4877)
- We fixed an issue where the file extension was missing after downloading a file (we now fall-back to pdf). [#5816](https://github.com/JabRef/jabref/issues/5816)
- We fixed an issue where cleaning up entries broke web URLs, if "Make paths of linked files relative (if possible)" was enabled, which resulted in various other issues subsequently. [#5861](https://github.com/JabRef/jabref/issues/5861)
- We fixed an issue where the tab "Required fields" of the entry editor did not show all required fields, if at least two of the defined required fields are linked with a logical or. [#5859](https://github.com/JabRef/jabref/issues/5859)
- We fixed several issues concerning managing external file types: Now everything is usable and fully functional. Previously, there were problems with the radio buttons, with saving the settings and with loading an input field value. Furthermore, different behavior for Windows and other operating systems was given, which was unified as well. [#5846](https://github.com/JabRef/jabref/issues/5846)
- We fixed an issue where entries containing Unicode charaters were not parsed correctly [#5899](https://github.com/JabRef/jabref/issues/5899)
- We fixed an issue where an entry containing an external filename with curly braces could not be saved. Curly braces are now longer allowed in filenames. [#5899](https://github.com/JabRef/jabref/issues/5899)
- We fixed an issue where changing the type of an entry did not update the main table [#5906](https://github.com/JabRef/jabref/issues/5906)
- We fixed an issue where opening a library from the recent libraries menu was not possible [#5939](https://github.com/JabRef/jabref/issues/5939)


### Removed
- Ampersands are no longer escaped by default in the `bib` file. If you want to keep the current behaviour, you can use the new "Escape Ampersands" formatter as a save action.


## [5.0-beta] – 2019-12-15

### Changed

- We added a short DOI field formatter which shortens DOI to more human-readable form. [koppor#343](https://github.com/koppor/jabref/issues/343)
- We improved the display of group memberships by adding multiple colored bars if the entry belongs to more than one group. [#4574](https://github.com/JabRef/jabref/issues/4574)
- We added an option to show the preview as an extra tab in the entry editor (instead of in a split view). [#5244](https://github.com/JabRef/jabref/issues/5244)
- A custom Open/LibreOffice jstyle file now requires a layout line for the entry type `default` [#5452](https://github.com/JabRef/jabref/issues/5452)
- The entry editor is now open by default when JabRef starts up. [#5460](https://github.com/JabRef/jabref/issues/5460)
- Customized entry types are now serialized in alphabetical order in the bib file.
- We added a new ADS fetcher to use the new ADS API. [#4949](https://github.com/JabRef/jabref/issues/4949)
- We added support of the [X11 primary selection](https://unix.stackexchange.com/a/139193/18033) [#2389](https://github.com/JabRef/jabref/issues/2389)
- We added support to switch between biblatex and bibtex library types. [#5550](https://github.com/JabRef/jabref/issues/5550)
- We changed the save action buttons to be easier to understand. [#5565](https://github.com/JabRef/jabref/issues/5565)
- We made the columns for groups, files and uri in the main table reorderable and merged the clickable icon columns for uri, url, doi and eprint. [#5544](https://github.com/JabRef/jabref/pull/5544)
- We reduced the number of write actions performed when autosave is enabled [#5679](https://github.com/JabRef/jabref/issues/5679)
- We made the column sort order in the main table persistent [#5730](https://github.com/JabRef/jabref/pull/5730)
- When an entry is modified on disk, the change dialog now shows the merge dialog to highlight the changes [#5688](https://github.com/JabRef/jabref/pull/5688)

### Fixed

- Inherit fields from cross-referenced entries as specified by biblatex. [#5045](https://github.com/JabRef/jabref/issues/5045)
- We fixed an issue where it was no longer possible to connect to LibreOffice. [#5261](https://github.com/JabRef/jabref/issues/5261)
- The "All entries group" is no longer shown when no library is open.
- We fixed an exception which occurred when closing JabRef. [#5348](https://github.com/JabRef/jabref/issues/5348)
- We fixed an issue where JabRef reports incorrectly about customized entry types. [#5332](https://github.com/JabRef/jabref/issues/5332)
- We fixed a few problems that prevented JabFox to communicate with JabRef. [#4737](https://github.com/JabRef/jabref/issues/4737) [#4303](https://github.com/JabRef/jabref/issues/4303)
- We fixed an error where the groups containing an entry loose their highlight color when scrolling. [#5022](https://github.com/JabRef/jabref/issues/5022)
- We fixed an error where scrollbars were not shown. [#5374](https://github.com/JabRef/jabref/issues/5374)
- We fixed an error where an exception was thrown when merging entries. [#5169](https://github.com/JabRef/jabref/issues/5169)
- We fixed an error where certain metadata items were not serialized alphabetically.
- After assigning an entry to a group, the item count is now properly colored to reflect the new membership of the entry. [#3112](https://github.com/JabRef/jabref/issues/3112)
- The group panel is now properly updated when switching between libraries (or when closing/opening one). [#3142](https://github.com/JabRef/jabref/issues/3142)
- We fixed an error where the number of matched entries shown in the group pane was not updated correctly. [#4441](https://github.com/JabRef/jabref/issues/4441)
- We fixed an error where the wrong file is renamed and linked when using the "Copy, rename and link" action. [#5653](https://github.com/JabRef/jabref/issues/5653)
- We fixed a "null" error when writing XMP metadata. [#5449](https://github.com/JabRef/jabref/issues/5449)
- We fixed an issue where empty keywords lead to a strange display of automatic keyword groups. [#5333](https://github.com/JabRef/jabref/issues/5333)
- We fixed an error where the default color of a new group was white instead of dark gray. [#4868](https://github.com/JabRef/jabref/issues/4868)
- We fixed an issue where the first field in the entry editor got the focus while performing a different action (like searching). [#5084](https://github.com/JabRef/jabref/issues/5084)
- We fixed an issue where multiple entries were highlighted in the web search result after scrolling. [#5035](https://github.com/JabRef/jabref/issues/5035)
- We fixed an issue where the hover indication in the web search pane was not working. [#5277](https://github.com/JabRef/jabref/issues/5277)
- We fixed an error mentioning "javafx.controls/com.sun.javafx.scene.control" that was thrown when interacting with the toolbar.
- We fixed an error where a cleared search was restored after switching libraries. [#4846](https://github.com/JabRef/jabref/issues/4846)
- We fixed an exception which occurred when trying to open a non-existing file from the "Recent files"-menu [#5334](https://github.com/JabRef/jabref/issues/5334)
- We fixed an issues where the search highlight in the entry preview did not worked. [#5069](https://github.com/JabRef/jabref/issues/5069)
- The context menu for fields in the entry editor is back. [#5254](https://github.com/JabRef/jabref/issues/5254)
- We fixed an exception which occurred when trying to open a non-existing file from the "Recent files"-menu [#5334](https://github.com/JabRef/jabref/issues/5334)
- We fixed a problem where the "editor" information has been duplicated during saving a .bib-Database. [#5359](https://github.com/JabRef/jabref/issues/5359)
- We re-introduced the feature to switch between different preview styles. [#5221](https://github.com/JabRef/jabref/issues/5221)
- We fixed various issues (including [#5263](https://github.com/JabRef/jabref/issues/5263)) related to copying entries to the clipboard
- We fixed some display errors in the preferences dialog and replaced some of the controls [#5033](https://github.com/JabRef/jabref/pull/5033) [#5047](https://github.com/JabRef/jabref/pull/5047) [#5062](https://github.com/JabRef/jabref/pull/5062) [#5141](https://github.com/JabRef/jabref/pull/5141) [#5185](https://github.com/JabRef/jabref/pull/5185) [#5265](https://github.com/JabRef/jabref/pull/5265) [#5315](https://github.com/JabRef/jabref/pull/5315) [#5360](https://github.com/JabRef/jabref/pull/5360)
- We fixed an exception which occurred when trying to import entries without an open library. [#5447](https://github.com/JabRef/jabref/issues/5447)
- The "Automatically set file links" feature now follows symbolic links. [#5664](https://github.com/JabRef/jabref/issues/5664)
- After successful import of one or multiple bib entries the main table scrolls to the first imported entry [#5383](https://github.com/JabRef/jabref/issues/5383)
- We fixed an exception which occurred when an invalid jstyle was loaded. [#5452](https://github.com/JabRef/jabref/issues/5452)
- We fixed an issue where the command line arguments `importBibtex` and `importToOpen` did not import into the currently open library, but opened a new one. [#5537](https://github.com/JabRef/jabref/issues/5537)
- We fixed an error where the preview theme did not adapt to the "Dark" mode [#5463](https://github.com/JabRef/jabref/issues/5463)
- We fixed an issue where multiple entries were allowed in the "crossref" field [#5284](https://github.com/JabRef/jabref/issues/5284)
- We fixed an issue where the merge dialog showed the wrong text colour in "Dark" mode [#5516](https://github.com/JabRef/jabref/issues/5516)
- We fixed visibility issues with the scrollbar and group selection highlight in "Dark" mode, and enabled "Dark" mode for the OpenOffice preview in the style selection window. [#5522](https://github.com/JabRef/jabref/issues/5522)
- We fixed an issue where the author field was not correctly parsed during bibtex key-generation. [#5551](https://github.com/JabRef/jabref/issues/5551)
- We fixed an issue where notifications where shown during autosave. [#5555](https://github.com/JabRef/jabref/issues/5555)
- We fixed an issue where the side pane was not remembering its position. [#5615](https://github.com/JabRef/jabref/issues/5615)
- We fixed an issue where JabRef could not interact with [Oracle XE](https://www.oracle.com/de/database/technologies/appdev/xe.html) in the [shared SQL database setup](https://docs.jabref.org/collaborative-work/sqldatabase).
- We fixed an issue where the toolbar icons were hidden on smaller screens.
- We fixed an issue where renaming referenced files for bib entries with long titles was not possible. [#5603](https://github.com/JabRef/jabref/issues/5603)
- We fixed an issue where a window which is on an external screen gets unreachable when external screen is removed. [#5037](https://github.com/JabRef/jabref/issues/5037)
- We fixed a bug where the selection of groups was lost after drag and drop. [#2868](https://github.com/JabRef/jabref/issues/2868)
- We fixed an issue where the custom entry types didn't show the correct display name [#5651](https://github.com/JabRef/jabref/issues/5651)

### Removed

- We removed some obsolete notifications. [#5555](https://github.com/JabRef/jabref/issues/5555)
- We removed an internal step in the [ISBN-to-BibTeX fetcher](https://docs.jabref.org/import-using-publication-identifiers/isbntobibtex): The [ISBN to BibTeX Converter](https://manas.tungare.name/software/isbn-to-bibtex) by [@manastungare](https://github.com/manastungare) is not used anymore, because it is offline: "people using this tool have not been generating enough sales for Amazon."
- We removed the option to control the default drag and drop behaviour. You can use the modifier keys (like CtrL or Alt) instead.


## [5.0-alpha] – 2019-08-25

### Changed
- We added eventitle, eventdate and venue fields to @unpublished entry type.
- We added @software and @dataSet entry type to biblatex.
- All fields are now properly sorted alphabetically (in the subgroups of required/optional fields) when the entry is written to the bib file.
- We fixed an issue where some importers used the field `pubstatus` instead of the standard BibTeX field `pubstate`.
- We changed the latex command removal for docbook exporter. [#3838](https://github.com/JabRef/jabref/issues/3838)
- We changed the location of some fields in the entry editor (you might need to reset your preferences for these changes to come into effect)
  - Journal/Year/Month in biblatex mode -> Deprecated (if filled)
  - DOI/URL: General -> Optional
  - Internal fields like ranking, read status and priority: Other -> General
  - Moreover, empty deprecated fields are no longer shown
- Added server timezone parameter when connecting to a shared database.
- We updated the dialog for setting up general fields.
- URL field formatting is updated. All whitespace chars, located at the beginning/ending of the URL, are trimmed automatically
- We changed the behavior of the field formatting dialog such that the `bibtexkey` is not changed when formatting all fields or all text fields.
- We added a "Move file to file directory and rename file" option for simultaneously moving and renaming of document file. [#4166](https://github.com/JabRef/jabref/issues/4166)
- Use integrated graphics card instead of discrete on macOS [#4070](https://github.com/JabRef/jabref/issues/4070)
- We added a cleanup operation that detects an arXiv identifier in the note, journal or URL field and moves it to the `eprint` field.
  Because of this change, the last-used cleanup operations were reset.
- We changed the minimum required version of Java to 1.8.0_171, as this is the latest release for which the automatic Java update works.  [#4093](https://github.com/JabRef/jabref/issues/4093)
- The special fields like `Printed` and `Read status` now show gray icons when the row is hovered.
- We added a button in the tab header which allows you to close the database with one click. https://github.com/JabRef/jabref/issues/494
- Sorting in the main table now takes information from cross-referenced entries into account. https://github.com/JabRef/jabref/issues/2808
- If a group has a color specified, then entries matched by this group have a small colored bar in front of them in the main table.
- Change default icon for groups to a circle because a colored version of the old icon was hard to distinguish from its black counterpart.
- In the main table, the context menu appears now when you press the "context menu" button on the keyboard. [feature request in the forum](http://discourse.jabref.org/t/how-to-enable-keyboard-context-key-windows)
- We added icons to the group side panel to quickly switch between `union` and `intersection` group view mode https://github.com/JabRef/jabref/issues/3269.
- We use `https` for [fetching from most online bibliographic database](https://docs.jabref.org/import-using-online-bibliographic-database).
- We changed the default keyboard shortcuts for moving between entries when the entry editor is active to ̀<kbd>alt</kbd> + <kbd>up/down</kbd>.
- Opening a new file now prompts the directory of the currently selected file, instead of the directory of the last opened file.
- Window state is saved on close and restored on start.
- We made the MathSciNet fetcher more reliable.
- We added the ISBN fetcher to the list of fetcher available under "Update with bibliographic information from the web" in the entry editor toolbar.
- Files without a defined external file type are now directly opened with the default application of the operating system
- We streamlined the process to rename and move files by removing the confirmation dialogs.
- We removed the redundant new lines of markings and wrapped the summary in the File annotation tab. [#3823](https://github.com/JabRef/jabref/issues/3823)
- We add auto URL formatting when user paste link to URL field in entry editor. [koppor#254](https://github.com/koppor/jabref/issues/254)
- We added a minimum height for the entry editor so that it can no longer be hidden by accident. [#4279](https://github.com/JabRef/jabref/issues/4279)
- We added a new keyboard shortcut so that the entry editor could be closed by <kbd>Ctrl</kbd> + <kbd>E</kbd>. [#4222] (https://github.com/JabRef/jabref/issues/4222)
- We added an option in the preference dialog box, that allows user to pick the dark or light theme option. [#4130] (https://github.com/JabRef/jabref/issues/4130)
- We updated the Related Articles tab to accept JSON from the new version of the Mr. DLib service
- We added an option in the preference dialog box that allows user to choose behavior after dragging and dropping files in Entry Editor. [#4356](https://github.com/JabRef/jabref/issues/4356)
- We added the ability to have an export preference where previously "File"-->"Export"/"Export selected entries" would not save the user's preference[#4495](https://github.com/JabRef/jabref/issues/4495)
- We optimized the code responsible for connecting to an external database, which should lead to huge improvements in performance.
- For automatically created groups, added ability to filter groups by entry type. [#4539](https://github.com/JabRef/jabref/issues/4539)
- We added the ability to add field names from the Preferences Dialog [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added the ability to change the column widths directly in the main
. [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added a description of how recommendations were chosen and better error handling to Related Articles tab
- We added the ability to execute default action in dialog by using with <kbd>Ctrl</kbd> + <kbd>Enter</kbd> combination [#4496](https://github.com/JabRef/jabref/issues/4496)
- We grouped and reordered the Main Menu (File, Edit, Library, Quality, Tools, and View tabs & icons). [#4666](https://github.com/JabRef/jabref/issues/4666) [#4667](https://github.com/JabRef/jabref/issues/4667) [#4668](https://github.com/JabRef/jabref/issues/4668) [#4669](https://github.com/JabRef/jabref/issues/4669) [#4670](https://github.com/JabRef/jabref/issues/4670) [#4671](https://github.com/JabRef/jabref/issues/4671) [#4672](https://github.com/JabRef/jabref/issues/4672) [#4673](https://github.com/JabRef/jabref/issues/4673)
- We added additional modifiers (capitalize, titlecase and sentencecase) to the Bibtex key generator. [#1506](https://github.com/JabRef/jabref/issues/1506)
- We have migrated from the mysql jdbc connector to the mariadb one for better authentication scheme support. [#4746](https://github.com/JabRef/jabref/issues/4745)
- We grouped the toolbar icons and changed the Open Library and Copy icons. [#4584](https://github.com/JabRef/jabref/issues/4584)
- We added a browse button next to the path text field for aux-based groups. [#4586](https://github.com/JabRef/jabref/issues/4586)
- We changed the title of Group Dialog to "Add subgroup" from "Edit group" when we select Add subgroup option.
- We enable import button only if entries are selected. [#4755](https://github.com/JabRef/jabref/issues/4755)
- We made modifications to improve the contrast of UI elements. [#4583](https://github.com/JabRef/jabref/issues/4583)
- We added a warning for empty BibTeX keys in the entry editor. [#4440](https://github.com/JabRef/jabref/issues/4440)
- We added an option in the settings to set the default action in JabRef when right clicking on any entry in any database and selecting "Open folder". [#4763](https://github.com/JabRef/jabref/issues/4763)
- The Medline fetcher now normalizes the author names according to the BibTeX-Standard [#4345](https://github.com/JabRef/jabref/issues/4345)
- We added an option on the Linked File Viewer to rename the attached file of an entry directly on the JabRef. [#4844](https://github.com/JabRef/jabref/issues/4844)
- We added an option in the preference dialog box that allows user to enable helpful tooltips.[#3599](https://github.com/JabRef/jabref/issues/3599)
- We reworked the functionality for extracting BibTeX entries from plain text, because our used service [freecite shut down](https://library.brown.edu/libweb/freecite_notice.php). [#5206](https://github.com/JabRef/jabref/pull/5206)
- We moved the dropdown menu for selecting the push-application from the toolbar into the external application preferences. [#674](https://github.com/JabRef/jabref/issues/674)
- We removed the alphabetical ordering of the custom tabs and updated the error message when trying to create a general field with a name containing an illegal character. [#5019](https://github.com/JabRef/jabref/issues/5019)
- We added a context menu to the bib(la)tex-source-editor to copy'n'paste. [#5007](https://github.com/JabRef/jabref/pull/5007)
- We added a tool that allows searching for citations in LaTeX files. It scans directories and shows which entries are used, how many times and where.
- We added a 'LaTeX citations' tab to the entry editor, to search for citations to the active entry in the LaTeX file directory. It can be disabled in the preferences dialog.
- We added an option in preferences to allow for integers in field "edition" when running database in bibtex mode. [#4680](https://github.com/JabRef/jabref/issues/4680)
- We added the ability to use negation in export filter layouts. [#5138](https://github.com/JabRef/jabref/pull/5138)


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
- We fixed an issue where selecting a group messed up the focus of the main table and the entry editor. https://github.com/JabRef/jabref/issues/3367
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
- We fixed an issue where option 'open terminal here' with custom command was passing the wrong argument. [#4802](https://github.com/JabRef/jabref/issues/4802)
- We fixed an issue where ranking an entry would generate an IllegalArgumentException. [#4754](https://github.com/JabRef/jabref/issues/4754)
- We fixed an issue where special characters where removed from non-label key generation pattern parts [#4767](https://github.com/JabRef/jabref/issues/4767)
- We fixed an issue where the RIS import would overwite the article date with the value of the acessed date [#4816](https://github.com/JabRef/jabref/issues/4816)
- We fixed an issue where an NullPointer exception was thrown when a referenced entry in an Open/Libre Office document was no longer present in the library. Now an error message with the reference marker of the missing entry is shown. [#4932](https://github.com/JabRef/jabref/issues/4932)
- We fixed an issue where a database exception related to a missing timezone was too big. [#4827](https://github.com/JabRef/jabref/issues/4827)
- We fixed an issue where the IEEE fetcher returned an error if no keywords were present in the result from the IEEE website [#4997](https://github.com/JabRef/jabref/issues/4997)
- We fixed an issue where the command line help text had several errors, and arguments and descriptions have been rewritten to simplify and detail them better. [#4932](https://github.com/JabRef/jabref/issues/2016)
- We fixed an issue where the same menu for changing entry type had two different sizes and weights. [#4977](https://github.com/JabRef/jabref/issues/4977)
- We fixed an issue where the "Attach file" dialog, in the right-click menu for an entry, started on the working directory instead of the user's main directory. [#4995](https://github.com/JabRef/jabref/issues/4995)
- We fixed an issue where the JabRef Icon in the macOS launchpad was not displayed correctly [#5003](https://github.com/JabRef/jabref/issues/5003)
- We fixed an issue where the "Search for unlinked local files" would throw an exception when parsing the content of a PDF-file with missing "series" information [#5128](https://github.com/JabRef/jabref/issues/5128)

### Removed

- The feature to "mark entries" was removed and merged with the groups functionality.  For migration, a group is created for every value of the `__markedentry` field and the entry is added to this group.
- The number column was removed.
- We removed the global search feature.
- We removed the coloring of cells in the main table according to whether the field is optional/required.
- We removed the feature to find and resolve duplicate BibTeX keys (as this use case is already covered by the integrity check).
- We removed a few commands from the right-click menu that are not needed often and thus don't need to be placed that prominently:
  - Print entry preview: available through entry preview
  - All commands related to marking: marking is not yet reimplemented
  - Set/clear/append/rename fields: available through Edit menu
  - Manage keywords: available through the Edit menu
  - Copy linked files to folder: available through File menu
  - Add/move/remove from group: removed completely (functionality still available through group interface)
- We removed the option to change the column widths in the preferences dialog. [#4546](https://github.com/JabRef/jabref/issues/4546)

## Older versions

The changelog of JabRef 4.x is available at the [v4.x branch](https://github.com/JabRef/jabref/blob/v4.x/CHANGELOG.md).
The changelog of JabRef 3.x is available at the [v3.8.2 tag](https://github.com/JabRef/jabref/blob/v3.8.2/CHANGELOG.md).
The changelog of JabRef 2.11 and all previous versions is available as [text file in the v2.11.1 tag](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG).

[Unreleased]: https://github.com/JabRef/jabref/compare/v5.0-beta...HEAD
[5.0-beta]: https://github.com/JabRef/jabref/compare/v5.0-alpha...v5.0-beta
[5.0-alpha]: https://github.com/JabRef/jabref/compare/v4.3...v5.0-alpha
