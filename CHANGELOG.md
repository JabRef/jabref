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
- JabRef has a new logo! The logo was designed by "[AikTheOne](https://99designs.de/profiles/theonestudio)" - who was the winner of a design contest at 99designs.com
- Partly switched to a new UI technology ([JavaFX]).
  - Redesigned group panel.
    - Number of matched entries is always shown.
    - The background color of the hit counter signals whether the group contains all/any of the entries selected in the main table.
    - Added a possibility to filter the groups panel [#1904](https://github.com/JabRef/jabref/issues/1904)
    - Removed edit mode.
    - Removed the following commands in the right-click menu:
      - Expand/collapse subtree
      - Move up/down/left/right
    - Remove option to "highlight overlapping groups"
    - Moved the option to "Gray out non-hits" / "Hide non-hits" to the preferences
    - Removed the following options from the group preferences:
      - Show icons (icons can now be customized)
      - Show dynamic groups in italics (dynamic groups are not treated specially now)
      - Initially show groups tree expanded (always true now)
    - Expansion status of groups are saved across sessions. [#1428](https://github.com/JabRef/jabref/issues/1428)
  - Redesigned about dialog.
  - Redesigned key bindings dialog.
  - Redesigned journal abbreviations dialog.
  - New error console.
  - All file dialogs now use the native file selector of the OS. [#1711](https://github.com/JabRef/jabref/issues/1711)
- We added a few properties to a group:
    - Icon (with customizable color) that is shown in the groups panel (implements a [feature request in the forum](http://discourse.jabref.org/t/assign-colors-to-groups/321)).
    - Description text that is shown on mouse hover (implements old feature requests [489](https://sourceforge.net/p/jabref/feature-requests/489/) and [818](https://sourceforge.net/p/jabref/feature-requests/818/)
- We introduced "automatic groups" that automatically create subgroups based on a certain criteria (e.g., a subgroup for every author or keyword) and supports hierarchies. Implements [91](https://sourceforge.net/p/jabref/feature-requests/91/), [398](https://sourceforge.net/p/jabref/feature-requests/398/) and [#1173](https://github.com/JabRef/jabref/issues/1173) and [#628](https://github.com/JabRef/jabref/issues/628).
- We added a document viewer which allows you to have a glance at your PDF documents directly from within JabRef.
- Using "Look up document identifier" in the quality menu, it is possible to look up DOIs, ArXiv ids and other identifiers for multiple entries.
- Comments in PDF files can now be displayed inside JabRef in a separate tab
- We separated the `Move file` and `Rename Pdfs` logic and context menu entries in the `General`-Tab for the Field `file` to improve the semantics
- We integrated support for the [paper recommender system Mr.DLib](http://help.jabref.org/en/EntryEditor#related-articles-tab) in a new tab in the entry editor.
- We renamed "database" to "library" to have a real distinction to SQL and NoSQL databases. [#2095](https://github.com/JabRef/jabref/issues/2095)
- We improved the UI customization possibilities:
    - It is now possible to customize the colors and the size of the icons (implements a [feature request in the forum](http://discourse.jabref.org/t/menu-and-buttons-with-a-dark-theme/405)).
    - Resizing the menu and label sizes has been improved.
    - Font sizes can now be increased <kbd>Ctrl</kbd> + <kbd>Plus</kbd>, decreased <kbd>Ctrl</kbd> + <kbd>Minus</kbd>, and reset to default <kbd>CTRL</kbd> + <kbd>0</kbd>.
- <kbd>F4</kbd> opens selected file in current JTable context not just from selected entry inside the main table [#2355](https://github.com/JabRef/jabref/issues/2355)
- We are happy to welcome [CrossRef](https://www.crossref.org/) as a new member of our fetcher family. [#2455](https://github.com/JabRef/jabref/issues/2455)
- We added MathSciNet as a ID-based fetcher in the `BibTeX -> New entry` dialog (implements a [feature request in the forum](http://discourse.jabref.org/t/allow-to-search-by-mr-number-mathscinet))
- Add tab which shows the MathSciNet review website if the `MRNumber` field is present.
- A scrollbar was added to the cleanup panel, as a result of issue [#2501](https://github.com/JabRef/jabref/issues/2501)
- Several scrollbars were added to the preference dialog which show up when content is too large [#2559](https://github.com/JabRef/jabref/issues/2559)
- We fixed and improved the auto detection of the []OpenOffice and LibreOffice connection](http://help.jabref.org/en/OpenOfficeIntegration)
- We added an option to copy the title of BibTeX entries to the clipboard through `Edit -> Copy title` (implements [#210](https://github.com/koppor/jabref/issues/210))
- The `Move linked files to default file directory`-Cleanup operation respects the `File directory pattern` setting
- We removed the ordinals-to-superscript formatter from the recommendations for biblatex save actions [#2596](https://github.com/JabRef/jabref/issues/2596)
- Improved MS-Office Import/Export
  - Improved author handling
  - The `day` part of the biblatex `date` field is now exported to the corresponding `day` field. [#2691](https://github.com/JabRef/jabref/issues/2691)
  - Entries with a single corporate author are now correctly exported to the corresponding `corporate` author field. [#1497](https://github.com/JabRef/jabref/issues/1497)
  - Now exports the field `volumes` and `pubstate`.
- The integrity checker reports now if a journal is not found in the abbreviation list
- JabRef will now no longer delete meta data it does not know, but keeps such entries and tries to keep their formatting as far as possible.
- Switch to the [latex2unicode library](https://github.com/tomtung/latex2unicode) for converting LaTeX to unicode
- Single underscores are not converted during the LaTeX to unicode conversion, which does not follow the rules of LaTeX, but is what users require. [#2664](https://github.com/JabRef/jabref/issues/2664)
- The bibtexkey field is not converted to unicode 

### Fixed
 - ArXiV fetcher now checks similarity of entry when using DOI retrieval to avoid false positives [#2575](https://github.com/JabRef/jabref/issues/2575)
 - We fixed an issue of duplicate keys after using a fetcher, e.g., DOI or ISBN [#2867](https://github.com/JabRef/jabref/issues/2687)
 - We fixed an issue that prevented multiple parallel JabRef instances from terminating gracefully. [#2698](https://github.com/JabRef/jabref/issues/2698)
 - We fixed an issue where authors with multiple surnames were not presented correctly in the main table. [#2534](https://github.com/JabRef/jabref/issues/2534)
 - Repairs the handling of apostrophes in the LaTeX to unicode conversion. [#2500](https://github.com/JabRef/jabref/issues/2500)
 - Fix import of journal title in RIS format. [#2506](https://github.com/JabRef/jabref/issues/2506)
 - We fixed the export of the `number` field in MS-Office XML export. [#2509](https://github.com/JabRef/jabref/issues/2509)
 - The field `issue` is now always exported to the corresponding `issue` field in MS-Office XML.
 - We fixed the import of MS-Office XML files, when the `month` field contained an invalid value.
 - We fixed an issue with repeated escaping of the %-sign when running the LaTeXCleanup more than once. [#2451](https://github.com/JabRef/jabref/issues/2451)
 - Sciencedirect/Elsevier fetcher is now able to scrape new HTML structure [#2576](https://github.com/JabRef/jabref/issues/2576)
 - Fixed the synchronization logic of keywords and special fields and vice versa [#2580](https://github.com/JabRef/jabref/issues/2580)
 - We fixed an exception that prevented JabRef from starting in rare cases [bug report in the forum](http://discourse.jabref.org/t/jabref-not-opening/476).
 - We fixed an unhandled exception when saving an entry containing unbalanced braces [#2571](https://github.com/JabRef/jabref/issues/2571) 
 - Fixed a display issue when removing a group with a long name [#1407](https://github.com/JabRef/jabref/issues/1407)
 - We fixed an issue where the "find unlinked files" functionality threw an error when only one PDF was imported but not assigned to an entry [#2577](https://github.com/JabRef/jabref/issues/2577)
 - We fixed issue where escaped braces were incorrectly counted when calculating brace balance in a field [#2561](https://github.com/JabRef/jabref/issues/2561)
 - We fixed an issue introduced with Version 3.8.2 where executing the `Rename PDFs`-cleanup operation moved the files to the file directory. [#2526](https://github.com/JabRef/jabref/issues/2526)
 - We improved the performance when opening a big library that still used the old groups format. Fixes an [issue raised in the forum](http://discourse.jabref.org/t/v3-8-2-x64-windows-problem-saving-large-bib-libraries/456).
 - We fixed an issue where the `Move linked files to default file directory`- cleanup operation did not move the files to the location of the bib-file. [#2454](https://github.com/JabRef/jabref/issues/2454)
 - We fixed an issue where executing `Move file` on a selected file in the `general`-tab could overwrite an existing file. [#2385](https://github.com/JabRef/jabref/issues/2358)
 - We fixed an issue with importing groups and subgroups [#2600](https://github.com/JabRef/jabref/issues/2600)
 - Fixed an issue where title-related key patterns did not correspond to the documentation. [#2604](https://github.com/JabRef/jabref/issues/2604) [#2589](https://github.com/JabRef/jabref/issues/2589)
 - We fixed an issue which prohibited the citation export to external programs on MacOS. [#2613](https://github.com/JabRef/jabref/issues/2613)
 - We fixed an issue where the file folder could not be changed when running `Get fulltext` in the `general`-tab. [#2572](https://github.com/JabRef/jabref/issues/2572)
 - Newly created libraries no longer have the executable bit set under POSIX/Linux systems. The file permissions are now set to `664 (-rw-rw-r--)`. [#2635](https://github.com/JabRef/jabref/issues/#2635)
 - <kbd>Ctrl</kbd> + <kbd>A</kbd> now correctly selects all entries again. [#2615](https://github.com/JabRef/jabref/issues/#2615)
 - We fixed an issue where the dialog for selecting the main file directory in the preferences opened the wrong folder
 - OpenOffice text formatting now handles nested tags properly [#2483](https://github.com/JabRef/jabref/issues/#2483)
 - The group selection is no longer lost when switching tabs [#1104](https://github.com/JabRef/jabref/issues/1104)
 
### Removed













































## [3.8.2] – 2017-01-29

### Changed
- Added the option to update bibliographic information from DOI to the sidebar of the entryeditor. Implements [#2432](https://github.com/JabRef/jabref/issues/2432).
- The default shortcut for "Cleanup entries" is now <kbd>Alt</kbd> + <kbd>F8</kbd> since <kbd>F8</kbd> alone did not work.
  Please [reset your key bindings](http://help.jabref.org/en/CustomKeyBindings) to get <kbd>Alt</kbd> + <kbd>F8</kbd> as default.
  Fixes [#2251](https://github.com/JabRef/jabref/issues/2251).

### Fixed
- The formatter for normalizing pages now also can treat ACM pages such as `2:1--2:33`.
- Backslashes in content selectors are now correctly escaped. Fixes [#2426](https://github.com/JabRef/jabref/issues/2426).
- Non-ISO timestamp settings prevented the opening of the entry editor. Fixes [#2447](https://github.com/JabRef/jabref/issues/2447).
- When pressing <kbd>Ctrl</kbd> + <kbd>F</kbd> and the searchbar is already focused, the text will be selected.
- LaTeX symbols are now displayed as Unicode for the author column in the main table. `'n` and `\'{n}` are parsed correctly. Fixes [#2458](https://github.com/JabRef/jabref/issues/2458).
- If one deleted the current query it was not saved (every basepanel can have its own query). Fixes [#2468](https://github.com/JabRef/jabref/issues/2468).
- The [ACM fetcher](https://help.jabref.org/en/ACMPortal) does no longer add HTML code to the bib-file. Fixes [#2472](https://github.com/JabRef/jabref/issues/2472).
- When [finding unlinked files](https://help.jabref.org/en/FindUnlinkedFiles), JabRef does not freeze any more. Fixes [#2309]()https://github.com/JabRef/jabref/issues/2309).
- Collapse and expand all buttons in the group assignment dialog no longer lead to a crash of JabRef.
- The aux export command line function does no longer add duplicates of references that were resolved via `crossref`. Fixes [#2475](https://github.com/JabRef/jabref/issues/2475).
- When the database is changed externally, JabRef is no longer prevented from an orderly shutdown. Fixes [#2486](https://github.com/JabRef/jabref/issues/2486).
- Parsing of damaged metadata is now more robust and reports a more detailed error message. Fixes [#2477](https://github.com/JabRef/jabref/issues/2477).
- Dynamic groups with regular expression can be edited again. Fixes [#2481](https://github.com/JabRef/jabref/issues/2481).


## [3.8.1] – 2016-12-24

### Changed
- When [adding a new entry](https://help.jabref.org/en/BaseFrame#adding-a-new-entry), one can select "title" to create a full BibTeX entry based on a title.
- When [editing](https://help.jabref.org/en/EntryEditor) an article, the tab "Optional fields" now shows "ISSN".
- When editing a book, the tab "Optional fields" now shows "ISBN".
- When using "Copy citation (HTML)" and pasting into a text editor, plain text is always pasted.
- When using the "Download from URL" functionality, one is not limited to http(s) URLs, but can, for instance, enter ftp URLs.
- When using the "Look up full text documents" functionality, JabRef warns more explicitly about multiple requests.
- The entry received from DOI does no longer contain the DOI as URL. Implements [#2417](https://github.com/JabRef/jabref/issues/2417).
- We use following parameters for the JVM on Windows and OSX: `-XX:+UseG1GC -XX:+UseStringDeduplication -XX:StringTableSize=1000003`.

### Fixed
- Clicking on "Get Fulltext" button sets links correctly for the entry being edited. Fixes [#2391](https://github.com/JabRef/jabref/issues/2391).
- The [integrity check](https://help.jabref.org/en/CheckIntegrity) now determines the set of biblatex-only fields differently. Fixes [#2390](https://github.com/JabRef/jabref/issues/2390).
- The integrity check filter works again. Fixes [#2406](https://github.com/JabRef/jabref/issues/2406).
- The [ArXiv fetcher](http://help.jabref.org/en/arXiv) also accepts identifiers that include the "arXiv:" prefix. Fixes [#2427](https://github.com/JabRef/jabref/issues/2427).
- We fixed an issue where groups containing brackets were not working properly. Fixes [#2394](https://github.com/JabRef/jabref/issues/2394).
- Closing of subtrees in the groups panel using "close subtree" is working again. Fixes [#2319](https://github.com/JabRef/jabref/issues/2319).
- We fixed issues with the [timestamp](http://help.jabref.org/en/TimeStamp) field. However, clearing with the clear button is not possible if timestamp format does not match the current settings. Fixes [#2403](https://github.com/JabRef/jabref/issues/2403).
- The proxy settings are now also applied to HTTPS connections. Fixes [#2249](https://github.com/JabRef/jabref/issues/2249).


## [3.8] – 2016-12-16

### Changed
- Bibliographic information from web resources can now be used to complete existing entries.
  This functionality can be accessed via a new button in the entry editor.
- URLs can now be passed as arguments to the `-import` and `-importToOpen` command line options.
  The referenced file is downloaded and then imported as usual.
- We added integrity check to detect all bibtex keys which deviate from their generation pattern [#2206](https://github.com/JabRef/jabref/issues/2206)
- We added an integrity check that detects invalid DOIs [#1445](https://github.com/JabRef/jabref/issues/1445)
- We enhanced the integrity checks testing for biblatex-only fields to be aware of more fields (e.g., `location`).
- ISBNs not available at [ebook.de](https://www.ebook.de) are now resolved using <https://bibtex.chimbori.com/>. [#684](https://github.com/JabRef/jabref/issues/684)
- When using the ISBN fetcher, the names are now correctly rendered in BibTeX. [#2343](https://github.com/JabRef/jabref/issues/2343)
- We display both the field name `journaltitle` and `journal` in biblatex mode as `journaltitle` only was causing headaches. [#2209](https://github.com/JabRef/jabref/issues/2209)
- We changed the order of the cleanup operations so that the generated file name corresponds to the cleaned-up fields. [#1441](https://github.com/JabRef/jabref/issues/1441)
- Files can now be moved to subfolders named by a custom format pattern, e.g., based on `entrytype`.
  The pattern can be specified in the settings like the filename pattern. [#1092](https://github.com/JabRef/jabref/issues/1092)
- [#2375](https://github.com/JabRef/jabref/issues/2375) 'LaTeXCleanup' action does now escape % signs inside BibTeX fields
- Add the possibility to copy citations of multiple entries to the clipboard
- Custom EntryTypes are now stored independently for BibTeX and biblatex mode.
  - Upon the first start of JabRef 3.8 old entry type customizations will be converted to custom types for the set default database mode (BibTeX if not changed to biblatex)
- Upon opening a file with customized entry types it is now possible to choose which customizations should be stored in local preferences.
- The default emacs executable name on linux changed from `gnuclient` to `emacsclient`.
  [feature-request 433](https://sourceforge.net/p/jabref/feature-requests/433/)
- Replaces manual thread management with cached thread pool
- Windows and OSX binaries are now signed with a certificate.

### Fixed
- We fixed various problems with customized entry types:
  - Resetting the preferences now also resets custom entry types. [#2261](https://github.com/JabRef/jabref/issues/2261)
  - Importing preferences does no longer duplicate custom entry types. [#772](https://github.com/JabRef/jabref/issues/772)
  - Potenial problems upon resetting to defaults should be fixed. [#772](https://github.com/JabRef/jabref/issues/772)
  - Customized standard types (such as `@article`) are no longer listed as "custom" type in "New Entry" dialog.
  - Applying changes in the "Custom Entry Types" dialog is now faster. [#2318](https://github.com/JabRef/jabref/issues/2318)
- We fixed a few groups related issues:
  - "Remove entries from group" no longer removes entries from groups with similar names. [#2334](https://github.com/JabRef/jabref/issues/2334)
  - If an entry's group field contains 'a b' it is no longer considered a member the groups 'a', 'b', and 'a b'. [1873](https://github.com/JabRef/jabref/issues/1873)
  - Reading and writing now works for groups that contain special escaped characters in their names. [1681](https://github.com/JabRef/jabref/issues/1681)
- Fixed [#2221](https://github.com/JabRef/jabref/issues/2221): Customizable field content selectors due to popular demand. Content selectors now avoid duplicate words.
- We fixed an issue which prevented JabRef from closing using the "Quit" menu command. [#2336](https://github.com/JabRef/jabref/issues/2336)
- We fixed an issue where the file permissions of the .bib-file were changed upon saving [#2279](https://github.com/JabRef/jabref/issues/2279).
- We fixed an issue which prevented that a database was saved successfully if JabRef failed to generate new BibTeX-keys [#2285](https://github.com/JabRef/jabref/issues/2285).
- Update check now correctly notifies about new release if development version is used. [#2298](https://github.com/JabRef/jabref/issues/2298)
- Fixed [#2311](https://github.com/JabRef/jabref/issues/2311): The DBLP fetcher has been rewritten and is working again.
- Fixed [#2273](https://github.com/JabRef/jabref/issues/2273): Export via commandline in no-gui mode is now working again.
- We fixed an issue when JabRef restores its session and a shared database was used: The error message "No suitable driver found" will not appear.
- We fixed an issue which caused a metadata loss on reconnection to shared database. [#2219](https://github.com/JabRef/jabref/issues/2219)
- We fixed an issue which caused an internal error when leaving the file path field empty and connecting to a shared database.
- We fixed an issue where the biblatex Cleanup did not move the contents of the fields `year` and `month` to the field `date`. [#2335](https://github.com/JabRef/jabref/issues/2335)
- Fixed [#2378](https://github.com/JabRef/jabref/issues/2378): Saving of the Backup-Option in the Preferences does now work.
- We fixed an issue which prevented the preference dialog to open on systems with Java 9.


## [3.7] – 2016-11-14

### Changed
- Implementation of eventbased autosave and backup functionality and file synchronization for shared DBs. Related to [#344](https://github.com/JabRef/jabref/issues/344)
- Source tab in the entry editor displays "biblatex Source" when using biblatex mode
- [koppor#171](https://github.com/koppor/jabref/issues/171): Add Shortcuts to context menu
- Add session restoring functionality for shared database. Related to [#1703](https://github.com/JabRef/jabref/issues/1703)
- Implementation of LiveUpdate for PostgreSQL & Oracle systems. Related to [#970](https://github.com/JabRef/jabref/issues/970).
- [koppor#31](https://github.com/koppor/jabref/issues/31): Number column in the main table is always Left aligned
- Added support for [1.0.1 CitationStyles](http://citationstyles.org/)
- You can set and cycle between different preview styles (including CitationStyles)
- Added fetcher for [MathSciNet](http://www.ams.org/mathscinet), [zbMATH](https://www.zbmath.org/) and [Astrophysics Data System](http://www.adsabs.harvard.edu/)
- Improved search:
  - Search queries consisting of a normal query and a field-based query are now supported (for example, `JabRef AND author == you`)
  - Implemented [#825](https://github.com/JabRef/jabref/issues/825): Search Bar across all bib files instead each having its own
  - Implemented [#573](https://github.com/JabRef/jabref/issues/573): Add key shortcut for global search (<kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>F</kbd>, if the searchfield is empty it will be focused instead)
  - The search result Window will now show which entry belongs to which bib file
  - The search result Window will now remember its location
  - The search result Window won't stay on top anymore if the main Window is focused and will be present in the taskbar
  - The user can jump from the searchbar to the maintable  with <kbd>Ctrl</kbd> + <kbd>Enter</kbd>
  - Implemented [#573 (comment)](https://github.com/JabRef/jabref/issues/573#issuecomment-232284156): Added shortcut: closing the search result window with <kbd>Ctrl</kbd> + <kbd>W</kbd>
- Added integrity check for fields with BibTeX keys, e.g., `crossref` and `related`, to check that the key exists
- Fields linking to other entries (e.g., `crossref` and `related`) have now specialized editors in the entry editor. Check the tabs "Other fields" and "General".
- [#1496](https://github.com/JabRef/jabref/issues/1496) Keep track of which entry a downloaded file belongs to
- Made it possible to download multiple entries in one action
- [#1506](https://github.com/JabRef/jabref/issues/1506) It is possible to apply two new key modifier `title_case` for Title Case, `capitalize` for Capitalized first character of each word (difference is that title case will leave prepositions etc in lower case), and `sentence_case` for normal sentence case (first word capitalized). In addition `lower_case` and `upper_case` can be used instead of `lower` and `upper`. 
- Added two new pseudo-fields for search: `anykeyword` to search for a specific keyword and `anyfield` to search in all fields (useful in combination with search in specific fields)
- [#1813](https://github.com/JabRef/jabref/issues/1813) Import/Export preferences dialog default directory set to working directory
- [#1897](https://github.com/JabRef/jabref/issues/1897) Implemented integrity check for `year` field: Last four nonpunctuation characters should be numerals
- Address in MS-Office 2007 xml format is now imported as `location`
- [#1912](https://github.com/JabRef/jabref/issues/1912) Implemented integrity check for `edition` field: Should have the first letter capitalized (BibTeX), Should contain an integer or a literal (biblatex)
- The dialog for choosing new entries additionally supports ID-based entry generation. For instance, when searching for a DOI or ISBN, you have to press <kbd>Ctrl</kbd> + <kbd>N</kbd> instead of using the web search (<kbd>Alt</kbd> + <kbd>4</kbd>).
- `number` field is now exported as `number` field in MS-Office 2007 xml format, if no `issue` field is present and the entry type is not `patent`
- `note` field is now exported as `comments` field in MS-Office 2007 xml format
- `comments` field in MS-Office 2007 xml format is now imported as `note` field
- [#463](https://github.com/JabRef/jabref/issues/463): Disable menu-item and toolbar-buttons while no database is open
- Implemented integrity check for `note` and `howpublished` field: Should have the first letter capitalized (BibTeX)
- <kbd>Pos1</kbd> / <kbd>Home</kbd> now select the first/last entry in the main table and the search result frame.
- <kbd>Up</kbd> / <kbd>Down</kbd> / <kbd>Tab</kbd> / <kbd>Shift</kbd> + <kbd>Tab</kbd> in the search result frame have now the same functionality as in the main  table.
- Importer for MODS format added
- [#2012](https://github.com/JabRef/jabref/issues/2012) Implemented integrity check for `month` field: Should be an integer or normalized (biblatex), Should be normalized (BibTeX)
- [#1779](https://github.com/JabRef/jabref/issues/1779) Implemented integrity check for `bibtexkey` field: Empty BibTeX key
- Prohibit more than one connections to the same shared database.
- Implemented integrity check for `journaltitle` field: biblatex field only (BibTeX)
- [#463](https://github.com/JabRef/jabref/issues/463): Disable certain menu items, toolbar buttons and context menu items while multiple entries are selected
- [#490](https://github.com/JabRef/jabref/issues/490) Added right click menu to main table and entry editor to allow copying doi url
- [#549](https://github.com/JabRef/jabref/issues/549) Added new shortcut to copy the BibTeX key as a hyperlink to its url to the clipboard
- Complete vietnam language translation in menu
- Generalize German translation of database into "Datenbank"
- Improve language quality of the German translation of shared database
- Change "Recent files" to "Recent databases" to keep the file menu consistent
- Customized importer files need to be slightly changed since the class `ImportFormat` was renamed to `Importer`
- [koppor/#97] (https://github.com/koppor/jabref/issues/97): When importing preferences, the explorer will start where the preferences are last exported
- [koppor#5](https://github.com/koppor/jabref/issues/5) When entries are found while dropping a pdf with xmp meta data the found entries will be displayed in the import dialog
- [koppor#61](https://github.com/koppor/jabref/issues/61) Display gray background text in "Author" and "Editor" field to assist newcomers
- Updated Vietnamese translation
- Added greyed-out suggestion for `year`/`date`/`url` fields
- [#1908](https://github.com/JabRef/jabref/issues/1908) Add a shortcut for check integrity <kbd>Ctrl</kbd> + <kbd>F8</kbd>
- When creatig an entry based on an ISBN, but the ISBN is not available on ebook.de, the error message is now more clear.

### Fixed
- Fixed problem where closing brackets could not be used as texts in layout arguments
- Fixed NullPointerException when opening search result window for an untitled database
- Fixed selecting an entry out of multiple duplicates
- Entries in the SearchResultPanel will be shown correctly (Latex to Unicode)
- Suggestions in the autocomplete will be shown correctly (Latex to Unicode)
- Selecting an entry in the search result Window will now select the correct entry in the bib file
- Suggestions in the autocomplete (search) are now in Unicode
- Entries in the SearchResultDialog are now converted to Unicode
- Fixed NullPointerException when opening search result window for an untitled database
- Fixed entry table traversal with Tab (no column traversal thus no double jump)
- Fixed: When searching the first match will be selected if the current selection is no match
- Fixed [koppor#160](https://github.com/koppor/jabref/issues/160): Tooltips now working in the main table
- Fixed [koppor/#128](https://github.com/koppor/jabref/issues/128): Sensible default settings for "Enable save actions" and "Cleanup"
- Fixed loop when pulling changes (shared database) when current selected field has changed
- Fixed field `key` field is not exported to MS-Office 2008 xml format
- Fixed field `location` containing only city is not exported correctly to MS-Office 2007 xml format
- Fixed close action of entry editor not working after parsing error corrected
- Fixed RTFChars would only use "?" for characters with unicode over the value of 127, now it uses the base character (é -> e instead of ?)
- Fixed download files failed silently when an invalid directory is selected
- Fixed InvalidBackgroundColor flickering with <kbd>Ctrl</kbd> + <kbd>S</kbd> and File > Save database
- Fixed file menu displays wrong hotkey in the German translation
- Fixed [#617](https://github.com/JabRef/jabref/issues/617): `Enter` in global search opens the selected entry & `Enter` in search dialog window opens the selected entry
- Fixed [#1181](https://github.com/JabRef/jabref/issues/1181) and [#1504](https://github.com/JabRef/jabref/issues/1504): Improved "Normalize to BibTeX name format": Support separated names with commas and colons. Considered name affixes such as "Jr".
- Fixed [#1235](https://github.com/JabRef/jabref/issues/1235): Modified Key bindings do not work correctly
- Fixed [#1542](https://github.com/JabRef/jabref/issues/1542): Improved error messages when using fetcher
- Fixed [#1663](https://github.com/JabRef/jabref/issues/1663): Better multi-monitor support
- Fixed [#1757](https://github.com/JabRef/jabref/issues/1757): Crash after saving illegal argument in entry editor
- Fixed [#1808](https://github.com/JabRef/jabref/issues/1808): Font preference dialog now keeps changes
- Fixed [#1882](https://github.com/JabRef/jabref/issues/1882): Crash after saving illegal bibtexkey in entry editor
- Fixed [#1937](https://github.com/JabRef/jabref/issues/1937): If no help page for the current chosen language exists, the english help page will be shown
- Fixed [#1949](https://github.com/JabRef/jabref/issues/1949): Error message directs to the wrong preference tab
- Fixed [#1958](https://github.com/JabRef/jabref/issues/1958): Verbatim fields are no longer checked for HTML encoded characters by integrity checks
- Fixed [#1993](https://github.com/JabRef/jabref/issues/1993): Various optimizations regarding search performance
- Fixed [#2021](https://github.com/JabRef/jabref/issues/2021): All filetypes can be selected on MacOS again
- Fixed [#2054](https://github.com/JabRef/jabref/issues/2054): Ignoring a new version now works as expected
- Fixed [#2060](https://github.com/JabRef/jabref/issues/2060): Medline fetcher now imports data in UTF-8 encoding
- Fixed [#2064](https://github.com/JabRef/jabref/issues/2064): Not all `other fields` are shown on entry change of same type
- Fixed [#2089](https://github.com/JabRef/jabref/issues/2089): Fixed faulty cite key generation
- Fixed [#2090](https://github.com/JabRef/jabref/issues/#2090): If special fields were not selected, two menu item separator were shown
- Fixed [#2092](https://github.com/JabRef/jabref/issues/2092): "None"-button in date picker clears the date field
- Fixed [#2104](https://github.com/JabRef/jabref/issues/#2104): Crash after saving BibTeX source with parsing error
- Fixed [#2109](https://github.com/JabRef/jabref/issues/#2109): <kbd>Ctrl</kbd> + <kbd>S</kbd> doesn't trigger parsing error message
- Fixed [#2200](https://github.com/JabRef/jabref/issues/#2200): Sorting now uses the same unicode representation that is also used for showing the content in the maintable
- Fixed [#2201](https://github.com/JabRef/jabref/issues/#2201) and [#1825](https://github.com/JabRef/jabref/issues/#1825): Status of the Group panel is saved and reused for next startup of JabRef
- Fixed [#2228](https://github.com/JabRef/jabref/issues/2228): Fixed Medline fetcher no longer working. The fetcher now uses `https` for fetching

### Removed
- Removed 2nd preview style
- The non-supported feature of being able to define file directories for any extension is removed. Still, it should work for older databases using the legacy `ps` and `pdf` fields, although we strongly encourage using the `file` field.
- Automatic migration for the `evastar_pdf` field is removed.
- We removed the customizable "content selectors" since they are replaced by the auto-completion feature
- Removed optional fields from `other fields` (BibTeX), Removed deprecated fields from `other fields` (biblatex)


## [3.6] – 2016-08-26

### Changed
- [#462](https://github.com/JabRef/jabref/issues/462) Extend the OpenConsoleFeature by offering a selection between default terminal emulator and configurable command execution.
- [#970](https://github.com/JabRef/jabref/issues/970): Implementation of shared database support (full system) with event based synchronization for MySQL, PostgreSQL and Oracle database systems.
- [#1026](https://github.com/JabRef/jabref/issues/1026) JabRef does no longer delete user comments outside of BibTeX entries and strings
- [#1225](https://github.com/JabRef/jabref/issues/1225): Hotkeys are now consistent
- [#1249](https://github.com/JabRef/jabref/issues/1249) Date layout formatter added
- [#1345](https://github.com/JabRef/jabref/issues/1345) Cleanup ISSN
- [#1516](https://github.com/JabRef/jabref/issues/1516) Selected field names are written in uppercase in the entry editor
- [#1751](https://github.com/JabRef/jabref/issues/1751) Added tooltip to web search button
- [#1758](https://github.com/JabRef/jabref/issues/1758) Added a button to open Database Properties dialog help
- [#1841](https://github.com/JabRef/jabref/issues/1841) The "etal"-string in the Authors layout formatter can now be empty
- Added EntryTypeFormatter to add camel casing to entry type in layouts, e.g., InProceedings
- Added print entry preview to the right click menu
- Added links to JabRef internet resources
- Added integrity check to avoid non-ASCII characters in BibTeX files
- Added ISBN integrity checker
- Added filter to not show selected integrity checks
- Automatically generated group names are now converted from LaTeX to Unicode
- Enhance the entry customization dialog to give better visual feedback
- Externally fetched information can be merged for entries with an ISBN
- Externally fetched information can be merged for entries with an ArXiv eprint
- File open dialogs now use default extensions as primary file filter
- For developers: Moved the bst package into logic. This requires the regeneration of antlr sources, execute: `gradlew generateSource`
- It is now possible to generate a new BIB database from the citations in an OpenOffice/LibreOffice document
- It is now possible to add your own lists of protected terms, see Options -> Manage protected terms
- Improve focus of the maintable after a sidepane gets closed (Before it would focus the toolbar or it would focus the wrong entry)
- Table row height is adjusted on Windows which is useful for high resolution displays
- The field name in the layout files for entry type is changed from `bibtextype` to `entrytype`. Please update your existing files as support for `bibtextype` will be removed eventually.
- The contents of `crossref` and `related` will be automatically updated if a linked entry changes key
- The information shown in the main table now resolves crossrefs and strings and it can be shown which fields are resolved in this way (Preferences -> Appearance -> Color codes for resolved fields)
- The formatting of the main table is based on the actual field shown when using e.g. `title/author`
- The arXiv fetcher now also supports free-text search queries
- Undo/redo are enabled/disabled and show the action in the tool tip
- Unified dialogs for opening/saving files

### Fixed
- Fixed [#636](https://github.com/JabRef/jabref/issues/636): DOI in export filters
- Fixed [#1257](https://github.com/JabRef/jabref/issues/1324): Preferences for the BibTeX key generator set in a version prior to 3.2 are now migrated automatically to the new version
- Fixed [#1264](https://github.com/JabRef/jabref/issues/1264): S with caron does not render correctly
- Fixed [#1288](https://github.com/JabRef/jabref/issues/1288): Newly opened bib-file is not focused
- Fixed [#1321](https://github.com/JabRef/jabref/issues/1321): LaTeX commands in fields not displayed in the list of references
- Fixed [#1324](https://github.com/JabRef/jabref/issues/1324): Save-Dialog for Lookup fulltext document now opens in the specified working directory
- Fixed [#1499](https://github.com/JabRef/jabref/issues/1499): {} braces are now treated correctly in in author/editor
- Fixed [#1527](https://github.com/JabRef/jabref/issues/1527): 'Get BibTeX data from DOI' Removes Marking
- Fixed [#1519](https://github.com/JabRef/jabref/issues/1519): The word "Seiten" is automatically removed when fetching info from ISBN
- Fixed [#1531](https://github.com/JabRef/jabref/issues/1531): `\relax` can be used for abbreviation of author names
- Fixed [#1554](https://github.com/JabRef/jabref/issues/1554): Import dialog is no longer hidden behind main window
- Fixed [#1592](https://github.com/JabRef/jabref/issues/1592): LibreOffice: wrong numbers in citation labels
- Fixed [#1609](https://github.com/JabRef/jabref/issues/1324): Adding a file to an entry opened dialog in the parent folder of the working directory
- Fixed [#1632](https://github.com/JabRef/jabref/issues/1632): User comments (`@Comment`) with or without brackets are now kept
- Fixed [#1639](https://github.com/JabRef/jabref/issues/1639): Google Scholar fetching works again.
- Fixed [#1643](https://github.com/JabRef/jabref/issues/1643): Searching with double quotes in a specific field ignores the last character
- Fixed [#1669](https://github.com/JabRef/jabref/issues/1669): Dialog for manual connection to OpenOffice/LibreOffice works again on Linux
- Fixed [#1682](https://github.com/JabRef/jabref/issues/1682): An entry now must have a BibTeX key to be cited in OpenOffice/LibreOffice
- Fixed [#1687](https://github.com/JabRef/jabref/issues/1687): "month" field ascending/descending sorting swapped
- Fixed [#1716](https://github.com/JabRef/jabref/issues/1716): `@`-Symbols stored in BibTeX fields no longer break the database
- Fixed [#1750](https://github.com/JabRef/jabref/issues/1750): biblatex `date` field is now correctly exported as `year` in MS-Office 2007 xml format
- Fixed [#1760](https://github.com/JabRef/jabref/issues/1760): Preview updated correctly when selecting a single entry after selecting multiple entries
- Fixed [#1771](https://github.com/JabRef/jabref/issues/1771): Show all supported import types as default
- Fixed [#1804](https://github.com/JabRef/jabref/issues/1804): Integrity check no longer removes URL field by mistake
- Fixed: LaTeX characters in author names are now converted to Unicode before export in MS-Office 2007 xml format
- Fixed: `volume`, `journaltitle`, `issue` and `number`(for patents) fields are now exported correctly in MS-Office 2007 xml format
- Fixed NullPointerException when clicking OK without specifying a field name in set/clear/rename fields
- Fixed IndexOutOfBoundsException when trying to download a full text document without selecting an entry
- Fixed NullPointerException when trying to set a special field or mark an entry through the menu without having an open database
- Fixed NullPointerException when trying to synchronize file field with an entry without BibTeX key
- Fixed NullPointerException when importing PDFs and pressing cancel when selecting entry type
- Fixed a number of issues related to accessing the GUI from outside the EDT
- Fixed NullPointerException when using BibTeX key pattern `authFirstFull` and the author does not have a "von"-part
- Fixed NullPointerException when opening Customize entry type dialog without an open database
- LaTeX to Unicode converter now handles combining accents
- Fixed NullPointerException when clicking Browse in Journal abbreviations with empty text field
- Fixed NullPointerException when opening file in Plain text import
- Fixed NullPointerException when appending database
- Fixed NullPointerException when loading a style file that has not got a default style
- Date fields in the biblatex standard are now always formatted in the correct way, independent of the preferences
- The merge entry dialog showed wrong heading after merging from DOI
- Manage content selectors now saves edited existing lists again and only marks database as changed when the content selectors are changed
- When inserting a duplicate the right entry will be selected
- Preview panel height is now saved immediately, thus is shown correctly if the panel height is changed, closed and opened again

### Removed
- [#1610](https://github.com/JabRef/jabref/issues/1610) Removed the possibility to auto show or hide the groups interface
- It is not longer possible to choose to convert HTML sub- and superscripts to equations
- Removed option to open right-click menu with ctrl + left-click as it was not working
- Removed option to disable entry editor when multiple entries are selected as it was not working
- Removed option to show warning for empty key as it was not working
- Removed option to show warning for duplicate key as it was not working
- Removed preview toolbar (since long disabled)


## [3.5] – 2016-07-13

### Changed
- Implemented [#1356](https://github.com/JabRef/jabref/issues/1356): Added a formatter for converting HTML to Unicode
- Implemented [#661](https://github.com/JabRef/jabref/issues/661): Introducing a "check for updates" mechnism (manually/automatic at startup)
- Implemented [#1338](https://github.com/JabRef/jabref/issues/1338): clicking on a crossref in the main table selects the parent entry and added a button in the entry editor to select the parent entry.
- Implemented [#1485](https://github.com/JabRef/jabref/issues/1485): Biblatex field shorttitle is now exported/imported as standard field ShortTitle to Word bibliography
- Implemented [#1431](https://github.com/JabRef/jabref/issues/1431): Import dialog shows file extensions and filters the view
- When resolving duplicate BibTeX-keys there is now an "Ignore" button. "Cancel" and close key now quits the resolving.
- The [online forum](http://discourse.jabref.org/) is now directly accessible via the "Help" menu
- Updated German translation

### Fixed
- Fixed [#1530](https://github.com/JabRef/jabref/issues/1530): Unescaped hashes in the url field are ignored by the integrity checker
- Fixed [#405](https://github.com/JabRef/jabref/issues/405): Added more {} around capital letters in Unicode/HTML to LaTeX conversion to preserve them
- Fixed [#1476](https://github.com/JabRef/jabref/issues/1476): NPE when importing from SQL DB because of missing DatabaseMode
- Fixed [#1481](https://github.com/JabRef/jabref/issues/1481): Mac OS X binary seems broken for JabRef 3.4 release
- Fixed [#1430](https://github.com/JabRef/jabref/issues/1430): "review changes" did misinterpret changes
- Fixed [#1434](https://github.com/JabRef/jabref/issues/1434): Static groups are now longer displayed as dynamic ones
- Fixed [#1482](https://github.com/JabRef/jabref/issues/1482): Correct number of matched entries is displayed for refining subgroups
- Fixed [#1444](https://github.com/JabRef/jabref/issues/1444): Implement getExtension and getDescription for importers.
- Fixed [#1507](https://github.com/JabRef/jabref/issues/1507): Keywords are now separated by the delimiter specified in the preferences
- Fixed [#1484](https://github.com/JabRef/jabref/issues/1484): HTML export handles some UTF characters wrong
- Fixed [#1534](https://github.com/JabRef/jabref/issues/1534): "Mark entries imported into database" does not work correctly
- Fixed [#1500](https://github.com/JabRef/jabref/issues/1500): Renaming of explicit groups now changes entries accordingly
- Fixed issue where field changes were not undoable if the time stamp was updated on editing
- Springer fetcher now fetches the requested number of entries (not one less as before)
- Alleviate multiuser concurrency issue when near simultaneous saves occur to a shared database file


## [3.4] – 2016-06-02

### Changed
- Implemented [#629](https://github.com/JabRef/jabref/issues/629): Explicit groups are now written in the "groups" field of the entry instead of at the end of the bib file
- Main table now accepts pasted DOIs and tries to retrieve the entry
- Added support for several Biblatex-fields through drop-down lists with valid alternatives
- Added integrity checker for an odd number of unescaped '#'
- Implemented [feature request 384](https://sourceforge.net/p/jabref/features/384): The merge entries dialog now show all text and colored differences between the fields
- Implemented [#1233](https://github.com/JabRef/jabref/issues/1233): Group side pane now takes up all the remaining space
- Added integrity check detecting HTML-encoded characters
- Added missing help files
- Implemented [feature request #1294](https://github.com/JabRef/jabref/issues/1294): Added possibility to filter for `*.jstyle` files in OpenOffice/LibreOffice style selection dialog. Open style selection dialog in directory of last selected file
- Added integrity check for ISSN
- Add LaTeX to Unicode converter as cleanup operation
- Added an option in the about dialog to easily copy the version information of JabRef
- Integrity check table can be sorted by clicking on column headings
- Added \SOFTWARE\Jabref 'Path' registry entry for installation path inside the installer
- Added an additional icon to distinguish DOI and URL links ([feature request #696](https://github.com/JabRef/jabref/issues/696))
- Added nbib fields to Medlineplain importer and to MedlineImporter
- Implemented [#1342](https://github.com/JabRef/jabref/issues/1342): show description of case converters as tooltip 
- Updated German translation

### Fixed
- Fixed [#473](https://github.com/JabRef/jabref/issues/473): Values in an entry containing symbols like ' are now properly escaped for exporting to the database
- Fixed [#1270](https://github.com/JabRef/jabref/issues/1270): Auto save is now working again as expected (without leaving a bunch of temporary files behind)
- Fixed [#1234](https://github.com/JabRef/jabref/issues/1234): NPE when getting information from retrieved DOI
- Fixed [#1245](https://github.com/JabRef/jabref/issues/1245): Empty jstyle properties can now be specified as ""
- Fixed [#1259](https://github.com/JabRef/jabref/issues/1259): NPE when sorting tabs
- Fixed display bug in the cleanup dialog: field formatters are now correctly displayed using their name 
- Fixed [#1271](https://github.com/JabRef/jabref/issues/1271): Authors with compound first names are displayed properly 
- Fixed: Selecting invalid jstyle causes NPE and prevents opening of style selection dialog
- Fixed: Move linked files to default directory works again
- Fixed [#1327](https://github.com/JabRef/jabref/issues/1327): PDF cleanup changes order of linked pdfs
- Fixed [#1313](https://github.com/JabRef/jabref/issues/1313): Remove UI for a configuration option which was no longer available
- Fixed [#1340](https://github.com/JabRef/jabref/issues/1340): Edit -> Mark Specific Color Dysfunctional on OSX
- Fixed [#1245](https://github.com/JabRef/jabref/issues/1245): Empty jstyle properties can now be specified as ""
- Fixed [#1364](https://github.com/JabRef/jabref/issues/1364): Windows: install to LOCALAPPDATA directory for non-admin users
- Fixed [#1365](https://github.com/JabRef/jabref/issues/1365): Default label pattern back to `[auth][year]`
- Fixed [#796](https://github.com/JabRef/jabref/issues/796): Undoing more than one entry at the same time is now working
- Fixed [#1122](https://github.com/JabRef/jabref/issues/1122): Group view is immediately updated after adding an entry to a group
- Fixed [#171](https://github.com/JabRef/jabref/issues/171): Dragging an entry to a group preserves scrolling
- Fixed [#1353](https://github.com/JabRef/jabref/issues/1353): Fetch-Preview did not display updated BibTeX-Key after clicking on `Generate Now`
- Fixed [#1381](https://github.com/JabRef/jabref/issues/1381): File links containing blanks are broken if non-default viewer is set
- Fixed sourceforge bug 1000: shorttitleINI can generate the initials of the shorttitle
- Fixed [#1394](https://github.com/JabRef/jabref/issues/1394): Personal journal abbrevations could not be saved
- Fixed [#1400](https://github.com/JabRef/jabref/issues/1400): Detect path constructs wrong path for Windows
- Fixed [#973](https://github.com/JabRef/jabref/issues/973): Add additional DOI field for English version of MS Office 2007 XML
- Fixed [#1412](https://github.com/JabRef/jabref/issues/1412): Save action *protect terms* protects terms within words unecessarily
- Fixed [#1420](https://github.com/JabRef/jabref/issues/1420): Auto downloader should respect file pattern and propose correct filename
- Fixed [#651](https://github.com/JabRef/jabref/issues/651): Improve parsing of author names containing braces
- Fixed [#1421](https://github.com/JabRef/jabref/issues/1421): Auto downloader should try to retrieve DOI if not present and fetch afterwards
- Fixed [#1457](https://github.com/JabRef/jabref/issues/1457): Support multiple words inside LaTeX commands to RTF export
- Entries retain their groupmembership when undoing their cut/deletion
- Fixed [#1450](https://github.com/JabRef/jabref/issues/1450): EntryEditor is restored in the correct size after preference changes
- Fixed [#421](https://github.com/JabRef/jabref/issues/421): Remove LaTeX commands from all BibTeX fields when exporting to Word Bibliography

### Removed
- Removed possibility to export entries/databases to an `.sql` file, as the logic cannot easily use the correct escape logic
- Removed support of old groups format, which was used prior to JabRef version 1.6. If you happen to have a 10 years old .bib file, then JabRef 3.3 can be used to convert it to the current format.
- Removed possibility to automatically add braces via Option - Preferences - File - Store the following fields with braces around capital letters. Please use save actions instead for adding braces automatically.
- Removed button to refresh groups view. This button shouldn't be needed anymore. Please report any cases where the groups view is not updated automatically.
- Medline and GVK importer no longer try to expand author initials (i.e.  `EH Wissler -> E. H. Wissler`).
- Removed not-working option "Select Matches" under Groups -> Settings.


## [3.3] – 2016-04-17

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
- BibTeX and biblatex mode is now file based and can be switched at runtime. The information is stored in the .bib file, and if it is not there detected by the entry types.
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
- [#969](https://github.com/JabRef/jabref/issues/969) Adding and replacing old event system mechanisms with Google Guava EventBus.

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


## [3.2] – 2016-01-10

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


## [3.1] – 2015-12-24

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
- Fixed [#414](https://github.com/JabRef/jabref/issues/414): Rework biblatex entry types with correct required and optional fields
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


## [3.0] – 2015-11-29

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
 - Feature: case changers now leave protected areas (enclosed with curly brackets) alone
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

## 2.80 – never released

Version 2.80 was intended as intermediate step to JabRef 3.0.
Since much functionality has changed during development, a release of this version was skipped.

## 2.11

The changelog of 2.11 and versions before is maintained as [text file](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG) in the [v2.11.1 tag](https://github.com/JabRef/jabref/tree/v2.11.1).

[unreleased]: https://github.com/JabRef/jabref/compare/v3.8.2...HEAD
[3.8.2]: https://github.com/JabRef/jabref/compare/v3.8.1...v3.8.2
[3.8.1]: https://github.com/JabRef/jabref/compare/v3.8...v3.8.1
[3.8]: https://github.com/JabRef/jabref/compare/v3.7...v3.8
[3.7]: https://github.com/JabRef/jabref/compare/v3.6...v3.7
[3.6]: https://github.com/JabRef/jabref/compare/v3.5...v3.6
[3.5]: https://github.com/JabRef/jabref/compare/v3.4...v3.5
[3.4]: https://github.com/JabRef/jabref/compare/v3.3...v3.4
[3.3]: https://github.com/JabRef/jabref/compare/v3.2...v3.3
[3.2]: https://github.com/JabRef/jabref/compare/v3.1...v3.2
[3.1]: https://github.com/JabRef/jabref/compare/v3.0...v3.1
[3.0]: https://github.com/JabRef/jabref/compare/v2.11.1...v3.0
[dev_2.11]: https://github.com/JabRef/jabref/compare/v2.11.1...dev_2.11
[2.11.1]: https://github.com/JabRef/jabref/compare/v2.11...v2.11.1
[JavaFX]: https://en.wikipedia.org/wiki/JavaFX
