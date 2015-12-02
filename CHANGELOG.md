# Changelog
All notable changes to this project will be documented in this file.
This project **does not** adhere to [Semantic Versioning](http://semver.org/).
This file tries to follow the conventions proposed by [keepachangelog.com](http://keepachangelog.com/).
Here, the categories "Changed" for added and changed functionality,
"Fixed" for fixed functionality, and
"Removed" for removed functionality is used.

We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`,
to [sourceforge bugs](https://sourceforge.net/p/jabref/bugs/) by using `bug NUM`, and
to [sourceforge feature requests](https://sourceforge.net/p/jabref/features/) by using `feature NUM`.

## [Unreleased]

### Changed
- Add ability to format/cleanup the date field
- Add support for proxy authentication via VM args and GUI settings, this implements Feature Request #388
- Move Bibtex and Biblatex mode switcher to File menu
- Display active edit mode (BibTeX or Biblatex) at window title

### Fixed
- Fixed #420: Reenable preference changes
- Fixed #414: Rework BibLatex entry types with correct required and optional fields
- Fixed #413: Help links in released jar version are not working
- Fixes #412: Biblatex preserves capital letters, checking whether letters may be converted to lowercase within the Integrity Check action is obsolete.
- Fixed #437: The toolbar after the search field is now correctly wrapped when using a small window size for JabRef
- Fixed #438: Cut, Copy and Paste are now translated correctly in the menu

### Removed
- Removed file history size preference (never available from the UI)




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
   - Search options are available via a drop-down list, this implements Feature Request #853
   - "Clear search" button also clears search field, this implements Feature Request #601
   - Every search is done automatically (live) as soon as the search text is changed
   - Search is local by default. To do a global search, one has to do a local search and then this search can be done globally as well, opening a new window. 
   - The local search results can be shown in a new window. 
 - Feature: Merge information from a DOI generated BibTex entry to an entry
 - Added more characters to HTML/Unicode converter
 - Feature: Push citations to Texmaker ([bug 318](https://sourceforge.net/p/jabref/bugs/318/), [bug 582](https://sourceforge.net/p/jabref/bugs/582/))
 - Case changers improved to honor words (not yet more than single words) within {}
 - Feature: Added converters from HTML and Unicode to LaTeX on right click in text fields (#191)
 - Feature: Add an option to the FileList context menu to delete an associated file from the file system
 - Feature: Field names "Doi", "Ee", and "Url" are now written as "DOI", "EE", and "URL"
 - The default language is now automatically set to the system's locale.
 - Use correct encoding names (#155) and replace old encoding names in bibtex files. This changes the file header.
 - No longer write JabRef version to BibTex file header.
 - No longer add blank lines inside a bibtex entry
 - Feature: When pasting a Google search URL, meta data will be automatically stripped before insertion.
 - Feature: PDF auto download from ACS, arXiv, ScienceDirect, SpringerLink, and Google Scholar
 - List of authors is now auto generated `scripts/generate-authors.sh` and inserted into L10N About.html
 - Streamline logging API: Replace usages of java.util.logging with commons.logging
 - Remove support for custom icon themes. The user has to use the default one.
 - Solved feature request #767: New subdatabase based on AUX file (biblatex)
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
 - Fixed #325: Deactivating AutoCompletion crashes EntryEditor
 - Fixed bug when having added and then removed a personal journal list, an exception is always shown on startup
 - Fixed a bug in the IEEEXploreFetcher
 - Fixed [bug 1282](https://sourceforge.net/p/jabref/bugs/1282/) related to backslashes duplication.
 - Fixed [bug 1285](https://sourceforge.net/p/jabref/bugs/1285/): Editing position is not lost on saving
 - Fixed [bug 1297](https://sourceforge.net/p/jabref/bugs/1297/): No console message on closing
 - Fixed #194: JabRef starts again on Win XP and Win Vista
 - Fixed: Tooltips are now shown for the #-field when the bibtex entry is incomplete.
 - Fixed #173: Personal journal abbreviation list is not loaded twice
 - Bugfix: Preview of external journal abbreviation list now displays the correct list
 - Fixed #223: Window is displayed in visible area even when having multiple screens
 - Localization tweaks: "can not" -> "cannot" and "file name" -> "filename"
 - Fixed: When reconfiguring the BibTeX key generator, changes are applied instantly without requiring a restart of JabRef
 - Fixed #250: No hard line breaks after 70 chars in serialized JabRef meta data
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

## [dev_2.11] - unreleased

### Fixed
 - Fix #345: Remove history from help file

## [2.11.1] - 2015-11-16

### Fixed
 - Backports from 2.80: Fixed #250: No hard line breaks after 70 chars in serialized JabRef meta data
 - Backports from 2.80: Fixed #325: Deactivating AutoCompletion crashes EntryEditor

## 2.11 - 2015-11-11

The changelog of 2.11 and versions before is maintained as [text file](https://github.com/JabRef/jabref/blob/dev_2.11/CHANGELOG) in the [dev_2.11 branch](https://github.com/JabRef/jabref/tree/dev_2.11).

[Unreleased]: https://github.com/JabRef/jabref/compare/v3.0...HEAD
[3.0]: https://github.com/JabRef/jabref/compare/v2.11.1...v3.0
[dev_2.11]: https://github.com/JabRef/jabref/compare/v2.11.1...dev_2.11
[2.11.1]: https://github.com/JabRef/jabref/compare/v2.11...v2.11.1
