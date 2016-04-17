# Changelog
All notable changes to this project will be documented in this file.
This project **does not** adhere to [Semantic Versioning](http://semver.org/).
This file tries to follow the conventions proposed by [keepachangelog.com](http://keepachangelog.com/).
Here, the categories "Changed" for added and changed functionality,
"Fixed" for fixed functionality, and
"Removed" for removed functionality is used.

We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`,
to [sourceforge bugs](https://sourceforge.net/p/jabref/bugs/) by using `bug NUM`, and
to [sourceforge feature requests](https://sourceforge.net/p/jabref/features/) by using `feature request NUM`.

## [3.3] - 2016-04-17

### Changed
- Migrated JabRef help to markdown at https://github.com/JabRef/help.jabref.org
- Add possibility to lookup DOI from BibTeX entry contents inside the DOI field
- PDFs can be automatically fetched from IEEE (given that you have access without logging in)
- The OpenOffice/LibreOffice style file handling is changed to have only a single list of available style and you need to add your custom styles again
- OpenOffice/LibreOffice style files are now always read and written with the same default encoding as for the database (found in the preferences)
- The user journal abbreviation list is now always read and written with the same default encoding as for the database (found in the preferences)
- The mass edit function "Set/clear/rename fields" is now in the Edit menu
- Implemented [#455](https://github.com/JabRef/jabref/issues/455): Add button in preference dialog to reset preferences
- Add ability to run arbitrary formatters as cleanup actions (some old cleanup jobs are replaced by this functionality)
- Add "Move linked files to default file directory" as cleanup procedure
- Implemented [#756](https://github.com/JabRef/jabref/issues/756): Add possibility to reformat all entries on save (under Preferences, File)
- All fields in a bib entry are written without any leading and trailing whitespace 
- Comments and preamble are serialized with capitalized first letter, i.e. `@Comment` instead of `@comment` and `@Preamble` instead of `@PREAMBLE`.
- Global sorting options and preferences are removed. Databases can still be sorted on save, but this is configured locally and stored in the file
- OvidImporter now also imports fields: doi, issn, language and keywords
- Implemented [#647](https://github.com/JabRef/jabref/issues/647): The preview can now be copied
- [#459](https://github.com/JabRef/jabref/issues/459) Open default directory when trying to add files to an entry
- Implemented [#668](https://github.com/JabRef/jabref/issues/668): Replace clear with icon to reduce search bar width
- Improved layout for OSX: Toolbar buttons and search field
- BibTeX and BibLaTeX mode is now file based and can be switched at runtime. The information is stored in the .bib file, and if it is not there detected by the entry types.
- Moved all quality-related database actions inside a new quality menu
- [#684](https://github.com/JabRef/jabref/issues/684): ISBNtoBibTex Error Message is now more clear
- Moved default bibliography mode to general preferences tab
- Add dialog to show all preferences in their raw form plus some filtering
- Added Ordinal formatter (1 -> 1st etc)
- [#492](https://github.com/JabRef/jabref/issues/492): If no text is marked, the whole field is copied. Preview of pasted text in tool tip
- [#454](https://github.com/JabRef/jabref/issues/454) Add a tab that shows all remaining entry fields that are not displayed in any other tab
- The LaTeX to Unicode/HTML functionality is much improved by covering many more cases
- Ability to convert from LaTeX to Unicode in right-click field menu
- Regex-based search is know only applied entirely and not split up to different regexes on whitespaces
- [#492](https://github.com/JabRef/jabref/issues/492): If no text is marked, the whole field is copied. Preview of pasted text in tool tip
- Integrity check now also checks broken file links, abbreviations in `journal` and `booktitle`, and incorrect use of proceedings with page numbers
- PdfContentImporter does not write the content of the first page into the review field any more
- Implemented [#462](https://github.com/JabRef/jabref/issues/462): Add new action to open console where opened database file is located. New button, menu entry and shortcut (CTRL+SHIFT+J) for this action have also been added.
- [#957](https://github.com/JabRef/jabref/issues/957) Improved usability of Export save order selection in Preferences and Database Properties
- [#958](https://github.com/JabRef/jabref/issues/958) Adjusted size and changed layout of database dialog
- [#1023](https://github.com/JabRef/jabref/issues/492) ArXiv fetcher now also fetches based on eprint id
- Moved "Get BibTeX data from DOI" from main table context menu to DOI field in entry editor
- Added open buttons to DOI and URL field
- Move Look & Feel settings from advanced to appearance tab in preferences
- JabRef installer now automatically determines the user rights and installs to home directory/program dir when user is restricted/admin
- Move PDF file directory configuration from external tab to file tab in preferences
- Implemented [#672](https://github.com/JabRef/jabref/issues/672): FileList now distributes its space dependent on the width of its columns
- Added missing German translations
- Swedish is added as a language option (still not a complete translation)

### Fixed
- Fixed [#318](https://github.com/JabRef/jabref/issues/318): Improve normalization of author names
- Fixed [#598](https://github.com/JabRef/jabref/issues/598) and [#402](https://github.com/JabRef/jabref/issues/402): No more issues with invalid icons for ExternalFileTypes in global search or after editing the settings
- Fixed [#883](https://github.com/JabRef/jabref/issues/883): No NPE during cleanup
- Fixed [#845](https://github.com/JabRef/jabref/issues/845): Add checkboxes for highlighting in groups menu, fixes other toggle highlighting as well for all toggle buttons
- Fixed [#890](https://github.com/JabRef/jabref/issues/890): No NPE when renaming file
- Fixed [#466](https://github.com/JabRef/jabref/issues/466): Rename PDF cleanup now also changes case of file name
- Fixed [#621](https://github.com/JabRef/jabref/issues/621) and [#669](https://github.com/JabRef/jabref/issues/669): Encoding and preamble now end with newline.
- Make BibTex parser more robust against missing newlines
- Fix bug that prevented the import of BibTex entries having only a key as content
- Fixed [#666](https://github.com/JabRef/jabref/issues/666): MS Office 2007 export is working again
- Fixed [#670](https://github.com/JabRef/jabref/issues/670): Expressions using enclosed quotes (`keywords="one two"`) did not work.
- Fixed [#667](https://github.com/JabRef/jabref/issues/667): URL field is not sanitized anymore upon opening in browser.
- Fixed [#687](https://github.com/JabRef/jabref/issues/687): Fixed NPE when closing JabRef with new unsaved database.
- Fixed [#680](https://github.com/JabRef/jabref/issues/680): Synchronize Files key binding works again.
- Fixed [#212](https://github.com/JabRef/jabref/issues/212): Added command line option `-g` for autogenerating bibtex keys
- Fixed [#213](https://github.com/JabRef/jabref/issues/212): Added command line option `-asfl` for autosetting file links
- Fixed [#671](https://github.com/JabRef/jabref/issues/671): Remember working directory of last import
- IEEEXplore fetcher replaces keyword separator with the preferred
- Fixed [#710](https://github.com/JabRef/jabref/issues/710): Fixed quit behavior under OSX
- "Merge from DOI" now honors removed fields
- Fixed [#778](https://github.com/JabRef/jabref/issues/778): Fixed NPE when exporting to `.sql` File
- Fixed [#824](https://github.com/JabRef/jabref/issues/824): MimeTypeDetector can now also handle local file links
- Fixed [#803](https://github.com/JabRef/jabref/issues/803): Fixed dynamically group, free-form search
- Fixed [#743](https://github.com/JabRef/jabref/issues/743): Logger not configured when JAR is started
- Fixed [#822](https://github.com/JabRef/jabref/issues/822): OSX - Exception when adding the icon to the dock
- Fixed [#609](https://github.com/JabRef/jabref/issues/609): Sort Arrows are shown in the main table if table is sorted
- Fixed [#685](https://github.com/JabRef/jabref/issues/685): Fixed MySQL exporting for more than one entry
- Fixed [#815](https://github.com/JabRef/jabref/issues/815): Curly Braces no longer ignored in OpenOffice/LibreOffice citation
- Fixed [#855](https://github.com/JabRef/jabref/issues/856): Fixed OpenOffice Manual connect - Clicking on browse does now work correctly
- Fixed [#649](https://github.com/JabRef/jabref/issues/649): Key bindings are now working in the preview panel
- Fixed [#410](https://github.com/JabRef/jabref/issues/410): Find unlinked files no longer freezes when extracting entry from PDF content
- Fixed [#936](https://github.com/JabRef/jabref/issues/936): Preview panel is now updated when an entry is cut/deleted
- Fixed [#1001](https://github.com/JabRef/jabref/issues/1001): No NPE when exporting a complete database
- Fixed [#991](https://github.com/JabRef/jabref/issues/991): Entry is now correctly removed from the BibDatabase
- Fixed [#1062](https://github.com/JabRef/jabref/issues/1062): Merge entry with DOI information now also applies changes to entry type
- Fixed [#535](https://github.com/JabRef/jabref/issues/535): Add merge action to right click menu
- Fixed [#1115](https://github.com/JabRef/jabref/issues/1115): Wrong warning message when importing duplicate entries
- Fixed [#935](https://github.com/JabRef/jabref/issues/935): PDFs, which are readable, but carry a protection for editing, are treated by the XMP parser and the importer generating a BibTeX entry based on the content.
- Fixed: Showing the preview panel with a single-click at startup

### Removed
- Removed JabRef offline help files which are replaced by the new online documentation at https://github.com/JabRef/help.jabref.org
- Fixed [#627](https://github.com/JabRef/jabref/issues/627): The `pdf` field is removed from the export formats, use the `file` field
- Removed configuration option to use database file directory as base directory for attached files and make it default instead
- Removed save session functionality as it just saved the last opened tabs which is done by default
- Removed CLI option `-l` to load a session
- Removed PDF preview functionality
- Removed Sixpackimporter it is not used in the wild anymore
- Removed double click listener from `doi` and `url` fields


## [3.2] - 2016-01-10

### Changed
- All import/open database warnings are now shown in a scrolling text area
- Add an integrity check to ensure that a url has a correct protocol, implements [#358](https://github.com/JabRef/jabref/issues/358)

### Fixed
- Changes in customized entry types are now directly reflected in the table when clicking "Apply" or "OK"
- Fixed [#608](https://github.com/JabRef/jabref/issues/608): Export works again
- Fixed [#417](https://github.com/JabRef/jabref/issues/417): Table now updates when switching groups
- Fixed [#534](https://github.com/JabRef/jabref/issues/534): No OpenOffice setup panel in preferences
- Fixed [#545](https://github.com/JabRef/jabref/issues/545): ACM fetcher works again
- Fixed [#593](https://github.com/JabRef/jabref/issues/593): Reference list generation works for OpenOffice/LibreOffice again
- Fixed [#598](https://github.com/JabRef/jabref/issues/598): Use default file icon for custom external file types
- Fixed [#607](https://github.com/JabRef/jabref/issues/607): OpenOffice/LibreOffice works on OSX again

### Removed
- OpenOffice/LibreOffice is removed from the push-to-application button and only accessed through the side panel


## [3.1] - 2015-12-24

### Changed
- Added new DoiResolution fetcher that tries to download full text PDF from DOI link
- Add options to close other/all databases in tab right-click menu
- Implements [#470](https://github.com/JabRef/jabref/issues/470): Show editor (as an alternative to author) and booktitle (as an alternative to journal) in the main table by default
- Restore focus to last focused tab on start
- Add ability to format/cleanup the date field
- Add support for proxy authentication via VM args and GUI settings, this implements [feature request 388](https://sourceforge.net/p/jabref/feature-requests/388/)
- Move Bibtex and Biblatex mode switcher to File menu
- Display active edit mode (BibTeX or Biblatex) at window title
- Implements [#444](https://github.com/JabRef/jabref/issues/444): The search is cleared by either clicking the clear-button or by pressing ESC with having focus in the search field.
- Icons are shown as Header for icon columns in the entry table ([#315](https://github.com/JabRef/jabref/issues/315))
- Tooltips are shown for header columns and contents which are too wide to be displayed in the entry table ([#384](https://github.com/JabRef/jabref/issues/384))
- Default order in entry table:  # | all file based icons (file, URL/DOI, ...) | all bibtex field based icons (bibtexkey, entrytype, author, title, ...) | all activated special field icons (ranking, quality, ...)
- Write all field keys in lower case. No more camel casing of field names. E.g., `title` is written instead of `Title`, `howpublished` instead of `HowPublished`, and `doi` instead of `DOI`. The configuration option `Use camel case for field names (e.g., "HowPublished" instead of "howpublished")` is gone.
- All field saving options are removed. There is no more customization of field sorting. '=' is now appended to the field key instead of its value. The intendation is aligned for an entry and not for the entire database. Entry names are written in title case format.
- Entries are only reformatted if they were changed during a session. There is no more mandatory reformatting of the entire database on save.
- Implements [#565](https://github.com/JabRef/jabref/issues/565): Highlighting matches works now also for regular expressions in preview panel and entry editor
- IEEEXplore search now downloads the full Bibtex record instead of parsing the fields from the HTML webpage result (fixes [bug 1146](https://sourceforge.net/p/jabref/bugs/1146/) and [bug 1267](https://sourceforge.net/p/jabref/bugs/1267/))
- Christmas color theme (red and green)
- Implements #444: The search is cleared by either clicking the clear-button or by pressing ESC with having focus in the search field. 
- Added command line switch --debug to show more detailed logging messages

### Fixed
- Fixed [bug 482](https://sourceforge.net/p/jabref/bugs/482/) partly: escaped brackets are now parsed properly when opening a bib file
- Fixed [#479](https://github.com/JabRef/jabref/issues/479): Import works again
- Fixed [#434](https://github.com/JabRef/jabref/issues/434): Revert to old 'JabRef' installation folder name instead of 'jabref'
- Fixed [#435](https://github.com/JabRef/jabref/issues/435): Retrieve non open access ScienceDirect PDFs via HTTP DOM
- Fixed: Cleanup process aborts if linked file does not exists
- Fixed [#420](https://github.com/JabRef/jabref/issues/420): Reenable preference changes
- Fixed [#414](https://github.com/JabRef/jabref/issues/414): Rework BibLatex entry types with correct required and optional fields
- Fixed [#413](https://github.com/JabRef/jabref/issues/413): Help links in released jar version are not working
- Fixes [#412](https://github.com/JabRef/jabref/issues/412): Biblatex preserves capital letters, checking whether letters may be converted to lowercase within the Integrity Check action is obsolete.
- Fixed [#437](https://github.com/JabRef/jabref/issues/437): The toolbar after the search field is now correctly wrapped when using a small window size for JabRef
- Fixed [#438](https://github.com/JabRef/jabref/issues/438): Cut, Copy and Paste are now translated correctly in the menu
- Fixed [#443](https://github.com/JabRef/jabref/issues/443)/[#445](https://github.com/JabRef/jabref/issues/445): Fixed sorting and moving special field columns
- Fixed [#498](https://github.com/JabRef/jabref/issues/498): non-working legacy PDF/PS column removed
- Fixed [#473](https://github.com/JabRef/jabref/issues/473): Import/export to external database works again
- Fixed [#526](https://github.com/JabRef/jabref/issues/526): OpenOffice/LibreOffice connection works again on Linux/OSX
- Fixed [#533](https://github.com/JabRef/jabref/issues/533): Preview parsed incorrectly when regular expression was enabled
- Fixed: MedlinePlain Importer made more resistant for malformed entries
- Fixed [#564](https://github.com/JabRef/jabref/issues/564): Cite command changes are immediately reflected in the push-to-application actions, and not only after restart

### Removed
- Removed BioMail format importer
- Removed file history size preference (never available from the UI)
- Removed jstorImporter because it's hardly ever used, even Jstor.org doesn't support/export said format anymore
- Removed ScifinderImporter because it's hardly ever used, and we could not get resource files to test against
- Removed option "Show one letter heading for icon columns" which is obsolete with the fix of [#315](https://github.com/JabRef/jabref/issues/315)/[#384](https://github.com/JabRef/jabref/issues/384)
- Removed table column "PDF/PS" which refers to legacy fields "ps" resp. "pdf" which are no longer supported (see also fix [#498](https://github.com/JabRef/jabref/issues/498))
- Removed the ability to export references on the CLI interface based on year ranges


## [3.0] - 2015-11-29

### Changed
 - Updated to support OpenOffice 4 and LibreOffice 5
 - Add toolbar icon for deleting an entry, and move menu item for this action to BibTeX
 - Better support for IEEEtranBSTCTL entries
 - Quick selection of month in entry editor
 - Unknown entry types will be converted to 'Misc' (was 'Other' before).
 - EntryTypes are now clustered per group on the 'new entry' GUI screen.
 - Tab shows the minimal unique folder name substring if multiple database files share the same name
 - Added a page numbers integrity checker
 - Position and size of certain dialogs are stored and restored.
 - Feature: Search Springer
 - Feature: Search DOAJ, Directory of Open Access Journals
 - Changes the old integrity check by improving the code base (+tests) and converting it to a simple issues table
 - Added combo box in MassSetFieldAction to simplify selecting the correct field name
 - Feature: Merge information from both entries on duplication detection
 - Always use import inspection dialog on import from file
 - All duplicate whitespaces / tabs / newlines are now removed from non-multiline fields
 - Improvements to search:
   - Search bar is now at the top
   - A summary of the search result is shown in textual form in the search bar
   - The search text field changes its color based on the search result (red if nothing is found, green if at least one entry is found)
   - Autocompletion suggestions are shown in a popup
   - Search options are available via a drop-down list, this implements [feature request 853](https://sourceforge.net/p/jabref/feature-requests/853/)
   - "Clear search" button also clears search field, this implements [feature request 601](https://sourceforge.net/p/jabref/feature-requests/601/)
   - Every search is done automatically (live) as soon as the search text is changed
   - Search is local by default. To do a global search, one has to do a local search and then this search can be done globally as well, opening a new window. 
   - The local search results can be shown in a new window. 
 - Feature: Merge information from a DOI generated BibTex entry to an entry
 - Added more characters to HTML/Unicode converter
 - Feature: Push citations to Texmaker ([bug 318](https://sourceforge.net/p/jabref/bugs/318/), [bug 582](https://sourceforge.net/p/jabref/bugs/582/))
 - Case changers improved to honor words (not yet more than single words) within {}
 - Feature: Added converters from HTML and Unicode to LaTeX on right click in text fields ([#191](https://github.com/JabRef/jabref/issues/191))
 - Feature: Add an option to the FileList context menu to delete an associated file from the file system
 - Feature: Field names "Doi", "Ee", and "Url" are now written as "DOI", "EE", and "URL"
 - The default language is now automatically set to the system's locale.
 - Use correct encoding names ([#155](https://github.com/JabRef/jabref/issues/155)) and replace old encoding names in bibtex files. This changes the file header.
 - No longer write JabRef version to BibTex file header.
 - No longer add blank lines inside a bibtex entry
 - Feature: When pasting a Google search URL, meta data will be automatically stripped before insertion.
 - Feature: PDF auto download from ACS, arXiv, ScienceDirect, SpringerLink, and Google Scholar
 - List of authors is now auto generated `scripts/generate-authors.sh` and inserted into L10N About.html
 - Streamline logging API: Replace usages of java.util.logging with commons.logging
 - Remove support for custom icon themes. The user has to use the default one.
 - Solved [feature request 767](https://sourceforge.net/p/jabref/feature-requests/767/): New subdatabase based on AUX file (biblatex)
 - Feature: DOItoBibTeX fetcher now also handles HTTP URLs
 - Feature: "Normalize to BibTeX name format" also removes newlines
 - Tweak of preference defaults
   - Autolink requires that the filename starts with the given BibTeX key and the default filename patterns is key followed by title
   - Default sorting changed
   - Default label pattern changed from `[auth][year]` to `[authors3][year]`
 - Feature: case changers now leave protected areas (enclosed with curly brakets) alone
 - BREAKING: The BibTeX key generator settings from previous versions are lost
 - BREAKING: LabelPatterns `[auth.etal]`, `[authEtAl]`, `[authors]`, `[authorsN]`, `[authorLast]` and more to omit spaces and commas (and work as described at http://jabref.sourceforge.net/help/LabelPatterns.php)
 - BREAKING: `[keywordN]` returns the Nth keyword (as described in the help) and not the first N keywords
 - BREAKING: If field consists of blanks only or an emtpy string, it is not written at all
 - Feature: new LabelPattern `[authFirstFull]` returning the last name of the first author and also a "van" or "von" if it exists
 - Feature: all new lines when writing an entry are obeying the globally configured new line (File -> newline separator). Affects fields: abstract and review
 - Feature: `[veryShortTitle]` and `[shortTitle]` also skip words like "in", "among", "before", ...
 - Feature: New LabelPattern `[keywordsN]`, where N is optional. Returns the first N keywords. If no N is specified ("`[keywords]`"), all keywords are returned. Spaces are removed.
 - Update supported LookAndFeels
 - Show replaced journal abbreviations on console
 - Integrated [GVK-Plugin](http://www.gbv.de/wikis/cls/Jabref-GVK-Plugin)
 - The three options to manage file references are moved to their own separated group in the Tools menu. 
 - Default preferences: Remote server (port 6050) always started on first JabRef instance. This prevents JabRef loaded twice when opening a bib file.

### Fixed
 - Fixed the bug that the file encoding was not correctly determined from the first (or second) line
 - Fixed [#325](https://github.com/JabRef/jabref/issues/325): Deactivating AutoCompletion crashes EntryEditor
 - Fixed bug when having added and then removed a personal journal list, an exception is always shown on startup
 - Fixed a bug in the IEEEXploreFetcher
 - Fixed [bug 1282](https://sourceforge.net/p/jabref/bugs/1282/) related to backslashes duplication.
 - Fixed [bug 1285](https://sourceforge.net/p/jabref/bugs/1285/): Editing position is not lost on saving
 - Fixed [bug 1297](https://sourceforge.net/p/jabref/bugs/1297/): No console message on closing
 - Fixed [#194](https://github.com/JabRef/jabref/issues/194): JabRef starts again on Win XP and Win Vista
 - Fixed: Tooltips are now shown for the #-field when the bibtex entry is incomplete.
 - Fixed [#173](https://github.com/JabRef/jabref/issues/173): Personal journal abbreviation list is not loaded twice
 - Bugfix: Preview of external journal abbreviation list now displays the correct list
 - Fixed [#223](https://github.com/JabRef/jabref/issues/223): Window is displayed in visible area even when having multiple screens
 - Localization tweaks: "can not" -> "cannot" and "file name" -> "filename"
 - Fixed: When reconfiguring the BibTeX key generator, changes are applied instantly without requiring a restart of JabRef
 - Fixed [#250](https://github.com/JabRef/jabref/issues/250): No hard line breaks after 70 chars in serialized JabRef meta data
 - Fixed [bug 1296](https://sourceforge.net/p/jabref/bugs/1296/): External links in the help open in the standard browser
 - Fixed behavior of opening files: If an existing database is opened, it is focused now instead of opening it twice.

### Removed
 - Entry type 'Other' is not selectable anymore as it is no real entry type. Will be converted to 'Misc'.
 - BREAKING: Remove plugin functionality.
 - The key bindings for searching specific databases are removed
 - Remove option to toggle native file dialog on mac by making JabRef always use native file dialogs on mac
 - Remove options to set PDF and PS directories per .bib database as the general options have also been deleted.
 - Remove option to disable renaming in FileChooser dialogs.
 - Remove option to hide the BibTeX Code tab in the entry editor.
 - Remove option to set a custom icon for the external file types. This is not possible anymore with the new icon font.
 - Remove legacy options to sync files in the "pdf" or "ps" field
 - Remove button to merge entries and keep the old ones.
 - Remove non-compact rank symbols in favor of compact rank
 - Remove Mr.DLib support as MR.DLib will be shut down in 2015
 - Remove support for key bindings per external application by allowing only the key binding "push to application" for the currently selected external application.
 - Remove "edit preamble" from toolbar
 - Remove support to the move-to-SysTray action
 - Remove incremental search
 - Remove option to disable autocompleters for search and make this always one
 - Remove option to highlight matches and make this always one when not using regex or grammar-based search
 - Remove non-working web searches: JSTOR and Sciencedirect (planned to be fixed for the next release)
 - Remove option Tools -> Open PDF or PS which is replaced by Tools -> Open File

## 2.80 - never released

Version 2.80 was intended as intermediate step to JabRef 3.0.
Since much functionality has changed during development, a release of this version was skipped.

## 2.11

The changelog of 2.11 and versions before is maintained as [text file](https://github.com/JabRef/jabref/blob/dev_2.11/CHANGELOG) in the [dev_2.11 branch](https://github.com/JabRef/jabref/tree/dev_2.11).

[Unreleased]: https://github.com/JabRef/jabref/compare/v3.3...HEAD
[3.3]: https://github.com/JabRef/jabref/compare/v3.2...v3.3
[3.2]: https://github.com/JabRef/jabref/compare/v3.1...v3.2
[3.1]: https://github.com/JabRef/jabref/compare/v3.0...v3.1
[3.0]: https://github.com/JabRef/jabref/compare/v2.11.1...v3.0
[dev_2.11]: https://github.com/JabRef/jabref/compare/v2.11.1...dev_2.11
[2.11.1]: https://github.com/JabRef/jabref/compare/v2.11...v2.11.1
