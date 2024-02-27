# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`.
In case, there is no issue present, the pull request implementing the feature is linked.

Note that this project **does not** adhere to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- We added a "refresh" button for the LaTeX citations tab in the entry editor. [#10584](https://github.com/JabRef/jabref/issues/10584)
- We added the possibility to show the BibTeX source in the [web search](https://docs.jabref.org/collect/import-using-online-bibliographic-database) import screen. [#560](https://github.com/koppor/jabref/issues/560)
- We added a fetcher for [ISIDORE](https://isidore.science/), simply paste in the link into the text field or the last 6 digits in the link that identify that paper. [#10423](https://github.com/JabRef/jabref/issues/10423)
- When importing entries form the "Citation relations" tab, the field [cites](https://docs.jabref.org/advanced/entryeditor/entrylinks) is now filled according to the relationship between the entries. [#10572](https://github.com/JabRef/jabref/pull/10752)
- We added a new group icon column to the main table showing the icons of the entry's groups. [#10801](https://github.com/JabRef/jabref/pull/10801)
- When deleting an entry, the files linked to the entry are now optionally deleted as well. [#10509](https://github.com/JabRef/jabref/issues/10509)
- We added support to move the file to the system trash (instead of deleting it). [#10591](https://github.com/JabRef/jabref/pull/10591)
- We added ability to jump to an entry in the command line using `-j CITATIONKEY`. [koppor#540](https://github.com/koppor/jabref/issues/540)
- We added a new boolean to the style files for Openoffice/Libreoffice integration to switch between ZERO_WIDTH_SPACE (default) and no space. [#10843](https://github.com/JabRef/jabref/pull/10843)
- When pasting HTML into the abstract or a comment field, the hypertext is automatically converted to Markdown. [#10558](https://github.com/JabRef/jabref/issues/10558)
- We added the possibility to redownload files that had been present but are no longer in the specified location. [#10848](https://github.com/JabRef/jabref/issues/10848)
- We added the citation key pattern `[camelN]`. Equivalent to the first N words of the `[camel]` pattern.

### Changed

- The "Automatically open folders of attached files" preference default status has been changed to enabled on Windows. [koppor#56](https://github.com/koppor/jabref/issues/56)
- The Custom export format now uses the custom DOI base URI in the preferences for the `DOICheck`, if activated [forum#4084](https://discourse.jabref.org/t/export-html-disregards-custom-doi-base-uri/4084)
- The index directories for full text search have now more readable names to increase debugging possibilities using Apache Lucense's Lurk. [#10193](https://github.com/JabRef/jabref/issues/10193)
- The fulltext search also indexes files ending with .pdf (but do not having an explicit file type set). [#10193](https://github.com/JabRef/jabref/issues/10193)
- We changed the arrangement of the lists in the "Citation relations" tab. `Cites` are now on the left and `Cited by` on the right [#10572](https://github.com/JabRef/jabref/pull/10752)
- Sub libraries based on `aux` file can now also be generated if some citations are not found library. [#10775](https://github.com/JabRef/jabref/pull/10775)
- We rearranged the tab order in the entry editor and renamed the "Scite Tab" to "Citation information". [#10821](https://github.com/JabRef/jabref/issues/10821)
- We changed the duplicate handling in the Import entries dialog. Potential duplicate entries are marked with an icon and importing will now trigger the merge dialog [#10914](https://github.com/JabRef/jabref/pull/10914)
- We made the command "Push to TexShop" more robust to allow cite commands with a character before the first slash. [forum#2699](https://discourse.jabref.org/t/push-to-texshop-mac/2699/17?u=siedlerchr)
- We only show the notification "Saving library..." if the library contains more than 2000 entries. [#9803](https://github.com/JabRef/jabref/issues/9803)
- We enhanced the dialog for adding new fields in the content selector with a selection box containing a list of standard fields. [#10912](https://github.com/JabRef/jabref/pull/10912)

### Fixed

- We fixed an issue where the fulltext search button in entry editor used to disappear on click till the search is completed. [#10425](https://github.com/JabRef/jabref/issues/10425)
- We fixed an issue where attempting to cancel the importing/generation of an entry from id is ignored. [#10508](https://github.com/JabRef/jabref/issues/10508)
- We fixed an issue where the preview panel showing the wrong entry (an entry that is not selected in the entry table). [#9172](https://github.com/JabRef/jabref/issues/9172)
- We fixed an issue where HTML-reserved characters like '&' and '<', in addition to HTML entities like '&amp;' were not rendered correctly in entry preview. [#10677](https://github.com/JabRef/jabref/issues/10677)
- The last page of a PDF is now indexed by the full text search. [#10193](https://github.com/JabRef/jabref/issues/10193)
- We fixed an issue where the duplicate check did not take umlauts or other LaTeX-encoded characters into account. [#10744](https://github.com/JabRef/jabref/pull/10744)
- We fixed the colors of the icon on hover for unset special fields. [#10431](https://github.com/JabRef/jabref/issues/10431)
- We fixed an issue where the CrossRef field did not work if autocompletion was disabled [#8145](https://github.com/JabRef/jabref/issues/8145)
- We fixed an issue where exporting`@electronic` and `@online` entry types to the Office XMl would duplicate the field `title`  [#10807](https://github.com/JabRef/jabref/issues/10807)

### Removed

## [5.12] – 2023-12-24

### Added

- We added a scite.ai tab in the entry editor that retrieves 'Smart Citation' tallies for citations that have a DOI. [koppor#375](https://github.com/koppor/jabref/issues/375)  
- We added a dropdown menu to let users change the reference library during AUX file import. [#10472](https://github.com/JabRef/jabref/issues/10472)
- We added a button to let users reset the cite command to the default value. [#10569](https://github.com/JabRef/jabref/issues/10569)
- We added the option to use System Preference for Light/Dark Theme [#8729](https://github.com/JabRef/jabref/issues/8729).
- We added [scholar.archive.org](https://scholar.archive.org/) as a new fetcher. [#10498](https://github.com/JabRef/jabref/issues/10498)
- We integrated predatory journal checking as part of the Integrity Checker based on the [check-bib-for-predatory](https://github.com/CfKu/check-bib-for-predatory). [koppor#348](https://github.com/koppor/jabref/issues/348)
- We added a 'More options' section in the main table right click menu opening the preferences dialog. [#9432](https://github.com/JabRef/jabref/issues/9432)
- When creating a new group, it inherits the icon of the parent group. [#10521](https://github.com/JabRef/jabref/pull/10521)

### Changed

- We moved the location of the 'Open only one instance of JabRef' preference option from "Network" to "General". [#9306](https://github.com/JabRef/jabref/issues/9306)
- The two previews in the change resolver dialog now have their scrollbars synchronized. [#9576](https://github.com/JabRef/jabref/issues/9576).
- We changed the setting of the keyword separator to accept a single character only. [#177](https://github.com/koppor/jabref/issues/177)
- We replaced "SearchAll" in Web Search by "Search Selected". [#10556](https://github.com/JabRef/jabref/issues/10556)
- Short DOI formatter now checks, if the value is already formatted. If so, it returns the value instead of calling the ShortDOIService again. [#10589](https://github.com/JabRef/jabref/issues/10589)
- We upgraded to JavaFX 21.0.1. As a consequence JabRef requires now macOS 11 or later and GTK 3.8 or later on Linux [10627](https://github.com/JabRef/jabref/pull/10627).
- A user-specific comment fields is not enabled by default, but can be enabled using the "Add" button. [#10424](https://github.com/JabRef/jabref/issues/10424)
- We upgraded to Lucene 9.9 for the fulltext search. The search index will be rebuild. [#10686](https://github.com/JabRef/jabref/pull/10686)
- When using "Copy..." -> "Copy citation key", the delimiter configured at "Push applications" is respected. [#10707](https://github.com/JabRef/jabref/pull/10707)

### Fixed

- We fixed an issue where the added protected term has unwanted leading and trailing whitespaces, where the formatted text has unwanted empty brackets and where the word at the cursor in the textbox can be added to the list. [#10415](https://github.com/JabRef/jabref/issues/10415)
- We fixed an issue where in the merge dialog the file field of entries was not correctly merged when the first and second entry both contained values inside the file field. [#10572](https://github.com/JabRef/jabref/issues/10572)
- We fixed some small inconsistencies in the user interface. [#10507](https://github.com/JabRef/jabref/issues/10507) [#10458](https://github.com/JabRef/jabref/issues/10458) [#10660](https://github.com/JabRef/jabref/issues/10660)
- We fixed the issue where the Hayagriva YAML exporter would not include a parent field for the publisher/series. [#10596](https://github.com/JabRef/jabref/issues/10596)
- We fixed issues in the external file type dialog w.r.t. duplicate entries in the case of a language switch. [#10271](https://github.com/JabRef/jabref/issues/10271)
- We fixed an issue where the right-click action "Copy cite..." did not respect the configured citation command under "External Programs" -> "[Push Applications](https://docs.jabref.org/cite/pushtoapplications)" [#10615](https://github.com/JabRef/jabref/issues/10615)

### Removed

- We removed duplicate filtering and sorting operations in the MainTable when editing BibEntries. [#10619](https://github.com/JabRef/jabref/pull/10619)

## [5.11] – 2023-10-22

### Added

- We added the ability to sort subgroups in Z-A order, as well as by ascending and descending number of subgroups. [#10249](https://github.com/JabRef/jabref/issues/10249)
- We added the possibility to find (and add) papers that cite or are cited by a given paper. [#6187](https://github.com/JabRef/jabref/issues/6187)
- We added an error-specific message for when a download from a URL fails. [#9826](https://github.com/JabRef/jabref/issues/9826)
- We added support for customizing the citation command (e.g., `[@key1,@key2]`) when [pushing to external applications](https://docs.jabref.org/cite/pushtoapplications). [#10133](https://github.com/JabRef/jabref/issues/10133)
- We added an integrity check for more special characters. [#8712](https://github.com/JabRef/jabref/issues/8712)
- We added protected terms described as "Computer science". [#10222](https://github.com/JabRef/jabref/pull/10222)
- We added a link "Get more themes..." in the preferences to that points to [themes.jabref.org](https://themes.jabref.org) allowing the user to download new themes. [#10243](https://github.com/JabRef/jabref/issues/10243)
- We added a fetcher for [LOBID](https://lobid.org/resources/api) resources. [koppor#386](https://github.com/koppor/jabref/issues/386)
- When in `biblatex` mode, the [integrity check](https://docs.jabref.org/finding-sorting-and-cleaning-entries/checkintegrity) for journal titles now also checks the field `journal`.
- We added support for exporting to Hayagriva YAML format. [#10382](https://github.com/JabRef/jabref/issues/10382)
- We added support for pushing citations to [TeXShop](https://pages.uoregon.edu/koch/texshop/) on macOS [forum#2699](https://discourse.jabref.org/t/push-to-texshop-mac/2699).
- We added the 'Bachelor's thesis' type for Biblatex's 'Thesis' EntryType [#10029](https://github.com/JabRef/jabref/issues/10029).

### Changed

- The export formats `listrefs`, `tablerefs`, `tablerefsabsbib`, now use the ISO date format in the footer [#10383](https://github.com/JabRef/jabref/pull/10383).
- When searching for an identifier in the "Web search", the title of the search window is now "Identifier-based Web Search". [#10391](https://github.com/JabRef/jabref/pull/10391)
- The ampersand checker now skips verbatim fields (`file`, `url`, ...). [#10419](https://github.com/JabRef/jabref/pull/10419)
- If no existing document is selected for exporting "XMP annotated pdf" JabRef will now create a new PDF file with a sample text and the metadata. [#10102](https://github.com/JabRef/jabref/issues/10102)
- We modified the DOI cleanup to infer the DOI from an ArXiV ID if it's present. [#10426](https://github.com/JabRef/jabref/issues/10426)
- The ISI importer uses the field `comment` for notes (instead of `review). [#10478](https://github.com/JabRef/jabref/pull/10478)
- If no existing document is selected for exporting "Embedded BibTeX pdf" JabRef will now create a new PDF file with a sample text and the metadata. [#10101](https://github.com/JabRef/jabref/issues/10101)
- Translated titles format no longer raise a warning. [#10459](https://github.com/JabRef/jabref/issues/10459)
- We re-added the empty grey containers in the groups panel to keep an indicator for the current selected group, if displaying of group item count is turned off [#9972](https://github.com/JabRef/jabref/issues/9972)

### Fixed

- We fixed an issue where "Move URL in note field to url field" in the cleanup dialog caused an exception if no note field was present [forum#3999](https://discourse.jabref.org/t/cleanup-entries-cant-get-it-to-work/3999)
- It is possible again to use "current table sort order" for the order of entries when saving. [#9869](https://github.com/JabRef/jabref/issues/9869)
- Passwords can be stored in GNOME key ring. [#10274](https://github.com/JabRef/jabref/issues/10274)
- We fixed an issue where groups based on an aux file could not be created due to an exception [#10350](https://github.com/JabRef/jabref/issues/10350)
- We fixed an issue where the JabRef browser extension could not communicate with JabRef under macOS due to missing files. You should use the `.pkg` for the first installation as it updates all necessary files for the extension [#10308](https://github.com/JabRef/jabref/issues/10308)
- We fixed an issue where the ISBN fetcher returned the entrytype `misc` for certain ISBN numbers [#10348](https://github.com/JabRef/jabref/issues/10348)
- We fixed a bug where an exception was raised when saving less than three export save orders in the preference. [#10157](https://github.com/JabRef/jabref/issues/10157)
- We fixed an issue where it was possible to create a group with no name or with a group separator inside the name [#9776](https://github.com/JabRef/jabref/issues/9776)
- Biblatex's `journaltitle` is now also respected for showing the journal information. [#10397](https://github.com/JabRef/jabref/issues/10397)
- JabRef does not hang anymore when exporting via CLI. [#10380](https://github.com/JabRef/jabref/issues/10380)
- We fixed an issue where it was not possible to save a library on a network share under macOS due to an exception when acquiring a file lock [#10452](https://github.com/JabRef/jabref/issues/10452)
- We fixed an issue where exporting "XMP annotated pdf" without selecting an existing document would produce an exception. [#10102](https://github.com/JabRef/jabref/issues/10102)
- We fixed an issue where the "Enabled" column in the "Protected terms files" tab in the preferences could not be resized [#10285](https://github.com/JabRef/jabref/issues/10285)
- We fixed an issue where after creation of a new library, the new library was not focused. [koppor#592](https://github.com/koppor/jabref/issues/592)
- We fixed an issue where double clicking on an url in the file field would trigger an exception instead of opening the browser [#10480](https://github.com/JabRef/jabref/pull/10480)
- We fixed an issue where scrolling was impossible on dragging a citation on the groups panel. [#9754](https://github.com/JabRef/jabref/issues/9754)
- We fixed an issue where exporting "Embedded BibTeX pdf" without selecting an existing document would produce an exception. [#10101](https://github.com/JabRef/jabref/issues/10101)
- We fixed an issue where there was a failure to access the url link for "eprint" for the ArXiv entry.[#10474](https://github.com/JabRef/jabref/issues/10474)
- We fixed an issue where it was not possible to connect to a shared database once a group with entries was added or other metadata modified [#10336](https://github.com/JabRef/jabref/issues/10336)
- We fixed an issue where middle-button paste in X not always worked [#7905](https://github.com/JabRef/jabref/issues/7905)

## [5.10] – 2023-09-02

### Added

- We added a field showing the BibTeX/biblatex source for added and deleted entries in the "External Changes Resolver" dialog. [#9509](https://github.com/JabRef/jabref/issues/9509)
- We added user-specific comment field so that multiple users can make separate comments. [#543](https://github.com/koppor/jabref/issues/543)
- We added a search history list in the search field's right click menu. [#7906](https://github.com/JabRef/jabref/issues/7906)
- We added a full text fetcher for IACR eprints. [#9651](https://github.com/JabRef/jabref/pull/9651)
- We added "Attach file from URL" to right-click context menu to download and store a file with the reference library. [#9646](https://github.com/JabRef/jabref/issues/9646)
- We enabled updating an existing entry with data from InspireHEP. [#9351](https://github.com/JabRef/jabref/issues/9351)
- We added a fetcher for the Bibliotheksverbund Bayern (experimental). [#9641](https://github.com/JabRef/jabref/pull/9641)
- We added support for more biblatex date formats for parsing dates. [#2753](https://github.com/JabRef/jabref/issues/2753)
- We added support for multiple languages for exporting to and importing references from MS Office. [#9699](https://github.com/JabRef/jabref/issues/9699)
- We enabled scrolling in the groups list when dragging a group on another group. [#2869](https://github.com/JabRef/jabref/pull/2869)
- We added the option to automatically download online files when a new entry is created from an existing ID (e.g., DOI). The option can be disabled in the preferences under "Import and Export". [#9756](https://github.com/JabRef/jabref/issues/9756)
- We added a new Integrity check for unescaped ampersands. [koppor#585](https://github.com/koppor/jabref/issues/585)
- We added support for parsing `$\backslash$` in file paths (as exported by Mendeley). [forum#3470](https://discourse.jabref.org/t/mendeley-bib-import-with-linked-files/3470)
- We added the possibility to automatically fetch entries when an ISBN is pasted on the main table. [#9864](https://github.com/JabRef/jabref/issues/9864)
- We added the option to disable the automatic linking of files in the entry editor [#5105](https://github.com/JabRef/jabref/issues/5105)
- We added the link icon for ISBNs in linked identifiers column. [#9819](https://github.com/JabRef/jabref/issues/9819)
- We added key binding to focus on groups <kbd>alt</kbd> + <kbd>s</kbd> [#9863](https://github.com/JabRef/jabref/issues/9863)
- We added the option to unprotect a text selection, which strips all pairs of curly braces away. [#9950](https://github.com/JabRef/jabref/issues/9950)
- We added drag and drop events for field 'Groups' in entry editor panel. [#569](https://github.com/koppor/jabref/issues/569)
- We added support for parsing MathML in the Medline importer. [#4273](https://github.com/JabRef/jabref/issues/4273)
- We added the ability to search for an identifier (DOI, ISBN, ArXiv ID) directly from 'Web Search'. [#7575](https://github.com/JabRef/jabref/issues/7575) [#9674](https://github.com/JabRef/jabref/issues/9674)
- We added a cleanup activity that identifies a URL or a last-visited-date in the `note` field and moves it to the `url` and `urldate` field respectively. [koppor#216](https://github.com/koppor/jabref/issues/216)
- We enabled the user to change the name of a field in a custom entry type by double-clicking on it. [#9840](https://github.com/JabRef/jabref/issues/9840)
- We added some preferences options to disable online activity. [#10064](https://github.com/JabRef/jabref/issues/10064)
- We integrated two mail actions ("As Email" and "To Kindle") under a new "Send" option in the right-click & Tools menus. The Kindle option creates an email targeted to the user's Kindle email, which can be set in preferences under "External programs" [#6186](https://github.com/JabRef/jabref/issues/6186)
- We added an option to clear recent libraries' history. [#10003](https://github.com/JabRef/jabref/issues/10003)
- We added an option to encrypt and remember the proxy password. [#8055](https://github.com/JabRef/jabref/issues/8055)[#10044](https://github.com/JabRef/jabref/issues/10044)
- We added support for showing journal information, via info buttons next to the `Journal` and `ISSN` fields in the entry editor. [#6189](https://github.com/JabRef/jabref/issues/6189)
- We added support for pushing citations to Sublime Text 3 [#10098](https://github.com/JabRef/jabref/issues/10098)
- We added support for the Finnish language. [#10183](https://github.com/JabRef/jabref/pull/10183)
- We added the option to automatically replaces illegal characters in the filename when adding a file to JabRef. [#10182](https://github.com/JabRef/jabref/issues/10182)
- We added a privacy policy. [#10064](https://github.com/JabRef/jabref/issues/10064)
- We added a tooltip to show the number of entries in a group [#10208](https://github.com/JabRef/jabref/issues/10208)
- We fixed an issue where it was no longer possible to add or remove selected entries to groups via context menu [#10404](https://github.com/JabRef/jabref/issues/10404), [#10317](https://github.com/JabRef/jabref/issues/10317) [#10374](https://github.com/JabRef/jabref/issues/10374)

### Changed

- We replaced "Close" by "Close library" and placed it after "Save all" in the File menu. [#10043](https://github.com/JabRef/jabref/pull/10043)
- We upgraded to Lucene 9.7 for the fulltext search. The search index will be rebuild. [#9584](https://github.com/JabRef/jabref/pull/10036)
- 'Get full text' now also checks the file url. [#568](https://github.com/koppor/jabref/issues/568)
- JabRef writes a new backup file only if there is a change. Before, JabRef created a backup upon start. [#9679](https://github.com/JabRef/jabref/pull/9679)
- We modified the `Add Group` dialog to use the most recently selected group hierarchical context. [#9141](https://github.com/JabRef/jabref/issues/9141)
- We refined the 'main directory not found' error message. [#9625](https://github.com/JabRef/jabref/pull/9625)
- JabRef writes a new backup file only if there is a change. Before, JabRef created a backup upon start. [#9679](https://github.com/JabRef/jabref/pull/9679)
- Backups of libraries are not stored per JabRef version, but collected together. [#9676](https://github.com/JabRef/jabref/pull/9676)
- We streamlined the paths for logs and backups: The parent path fragment is always `logs` or `backups`.
- `log.txt` now contains an entry if a BibTeX entry could not be parsed.
- `log.txt` now contains debug messages. Debugging needs to be enabled explicitly. [#9678](https://github.com/JabRef/jabref/pull/9678)
- `log.txt` does not contain entries for non-found files during PDF indexing. [#9678](https://github.com/JabRef/jabref/pull/9678)
- The hostname is now determined using environment variables (`COMPUTERNAME`/`HOSTNAME`) first. [#9910](https://github.com/JabRef/jabref/pull/9910)
- We improved the Medline importer to correctly import ISO dates for `revised`. [#9536](https://github.com/JabRef/jabref/issues/9536)
- To avoid cluttering of the directory, We always delete the `.sav` file upon successful write. [#9675](https://github.com/JabRef/jabref/pull/9675)
- We improved the unlinking/deletion of multiple linked files of an entry using the <kbd>Delete</kbd> key. [#9473](https://github.com/JabRef/jabref/issues/9473)
- The field names of customized entry types are now exchanged preserving the case. [#9993](https://github.com/JabRef/jabref/pull/9993)
- We moved the custom entry types dialog into the preferences dialog. [#9760](https://github.com/JabRef/jabref/pull/9760)
- We moved the manage content selectors dialog to the library properties. [#9768](https://github.com/JabRef/jabref/pull/9768)
- We moved the preferences menu command from the options menu to the file menu. [#9768](https://github.com/JabRef/jabref/pull/9768)
- We reworked the cross ref labels in the entry editor and added a right click menu. [#10046](https://github.com/JabRef/jabref/pull/10046)
- We reorganized the order of tabs and settings in the library properties. [#9836](https://github.com/JabRef/jabref/pull/9836)
- We changed the handling of an "overflow" of authors at `[authIniN]`: JabRef uses `+` to indicate an overflow. Example: `[authIni2]` produces `A+` (instead of `AB`) for `Aachen and Berlin and Chemnitz`. [#9703](https://github.com/JabRef/jabref/pull/9703)
- We moved the preferences option to open the last edited files on startup to the 'General' tab. [#9808](https://github.com/JabRef/jabref/pull/9808)
- We improved the recognition of DOIs when pasting a link containing a DOI on the maintable. [#9864](https://github.com/JabRef/jabref/issues/9864s)
- We reordered the preferences dialog. [#9839](https://github.com/JabRef/jabref/pull/9839)
- We split the 'Import and Export' tab into 'Web Search' and 'Export'. [#9839](https://github.com/JabRef/jabref/pull/9839)
- We moved the option to run JabRef in memory stick mode into the preferences dialog toolbar. [#9866](https://github.com/JabRef/jabref/pull/9866)
- In case the library contains empty entries, they are not written to disk. [#8645](https://github.com/JabRef/jabref/issues/8645)
- The formatter `remove_unicode_ligatures` is now called `replace_unicode_ligatures`. [#9890](https://github.com/JabRef/jabref/pull/9890)
- We improved the error message when no terminal was found. [#9607](https://github.com/JabRef/jabref/issues/9607)
- In the context of the "systematic literature functionality", we changed the name "database" to "catalog" to use a separate term for online catalogs in comparison to SQL databases. [#9951](https://github.com/JabRef/jabref/pull/9951)
- We now show more fields (including Special Fields) in the dropdown selection for "Save sort order" in the library properties and for "Export sort order" in the preferences. [#10010](https://github.com/JabRef/jabref/issues/10010)
- We now encrypt and store the custom API keys in the OS native credential store. [#10044](https://github.com/JabRef/jabref/issues/10044)
- We changed the behavior of group addition/edit, so that sorting by alphabetical order is not performed by default after the modification. [#10017](https://github.com/JabRef/jabref/issues/10017)
- We fixed an issue with spacing in the cleanup dialogue. [#10081](https://github.com/JabRef/jabref/issues/10081)
- The GVK fetcher now uses the new [K10plus](https://www.bszgbv.de/services/k10plus/) database. [#10189](https://github.com/JabRef/jabref/pull/10189)

### Fixed

- We fixed an issue where clicking the group expansion pane/arrow caused the node to be selected, when it should just expand/detract the node. [#10111](https://github.com/JabRef/jabref/pull/10111)
- We fixed an issue where the browser import would add ' characters before the BibTeX entry on Linux. [#9588](https://github.com/JabRef/jabref/issues/9588)
- We fixed an issue where searching for a specific term with the DOAB fetcher lead to an exception. [#9571](https://github.com/JabRef/jabref/issues/9571)
- We fixed an issue where the "Import" -> "Library to import to" did not show the correct library name if two opened libraries had the same suffix. [#9567](https://github.com/JabRef/jabref/issues/9567)
- We fixed an issue where the rpm-Version of JabRef could not be properly uninstalled and reinstalled. [#9558](https://github.com/JabRef/jabref/issues/9558), [#9603](https://github.com/JabRef/jabref/issues/9603)
- We fixed an issue where the command line export using `--exportMatches` flag does not create an output bib file. [#9581](https://github.com/JabRef/jabref/issues/9581)
- We fixed an issue where custom field in the custom entry types could not be set to mulitline. [#9609](https://github.com/JabRef/jabref/issues/9609)
- We fixed an issue where the Office XML exporter did not resolve BibTeX-Strings when exporting entries. [forum#3741](https://discourse.jabref.org/t/exporting-bibtex-constant-strings-to-ms-office-2007-xml/3741)
- We fixed an issue where the Merge Entries Toolbar configuration was not saved after hitting 'Merge Entries' button. [#9091](https://github.com/JabRef/jabref/issues/9091)
- We fixed an issue where the password is stored in clear text if the user wants to use a proxy with authentication. [#8055](https://github.com/JabRef/jabref/issues/8055)
- JabRef is now more relaxed when parsing field content: In case a field content ended with `\`, the combination `\}` was treated as plain `}`. [#9668](https://github.com/JabRef/jabref/issues/9668)
- We resolved an issue that cut off the number of group entries when it exceeded four digits. [#8797](https://github.com/JabRef/jabref/issues/8797)
- We fixed the issue where the size of the global search window was not retained after closing. [#9362](https://github.com/JabRef/jabref/issues/9362)
- We fixed an issue where the Global Search UI preview is still white in dark theme. [#9362](https://github.com/JabRef/jabref/issues/9362)
- We fixed the double paste issue when <kbd>Cmd</kbd> + <kbd>v</kbd> is pressed on 'New entry from plaintext' dialog. [#9367](https://github.com/JabRef/jabref/issues/9367)
- We fixed an issue where the pin button on the Global Search dialog was located at the bottom and not at the top. [#9362](https://github.com/JabRef/jabref/issues/9362)
- We fixed the log text color in the event log console when using dark mode. [#9732](https://github.com/JabRef/jabref/issues/9732)
- We fixed an issue where searching for unlinked files would include the current library's .bib file. [#9735](https://github.com/JabRef/jabref/issues/9735)
- We fixed an issue where it was no longer possible to connect to a shared mysql database due to an exception. [#9761](https://github.com/JabRef/jabref/issues/9761)
- We fixed an issue where an exception was thrown for the user after <kbd>Ctrl</kbd>+<kbd>Z</kbd> command. [#9737](https://github.com/JabRef/jabref/issues/9737)
- We fixed the citation key generation for [`[authors]`, `[authshort]`, `[authorsAlpha]`, `[authIniN]`, `[authEtAl]`, `[auth.etal]`](https://docs.jabref.org/setup/citationkeypatterns#special-field-markers) to handle `and others` properly. [koppor#626](https://github.com/koppor/jabref/issues/626)
- We fixed the Save/save as file type shows BIBTEX_DB instead of "Bibtex library". [#9372](https://github.com/JabRef/jabref/issues/9372)
- We fixed the default main file directory for non-English Linux users. [#8010](https://github.com/JabRef/jabref/issues/8010)
- We fixed an issue when overwriting the owner was disabled. [#9896](https://github.com/JabRef/jabref/pull/9896)
- We fixed an issue regarding recording redundant prefixes in search history. [#9685](https://github.com/JabRef/jabref/issues/9685)
- We fixed an issue where passing a URL containing a DOI led to a "No entry found" notification. [#9821](https://github.com/JabRef/jabref/issues/9821)
- We fixed some minor visual inconsistencies and issues in the preferences dialog. [#9866](https://github.com/JabRef/jabref/pull/9866)
- The order of save actions is now retained. [#9890](https://github.com/JabRef/jabref/pull/9890)
- We fixed an issue where the order of save actions was not retained in the bib file. [#9890](https://github.com/JabRef/jabref/pull/9890)
- We fixed an issue in the preferences 'External file types' tab ignoring a custom application path in the edit dialog. [#9895](https://github.com/JabRef/jabref/issues/9895)
- We fixed an issue in the preferences where custom columns could be added to the entry table with no qualifier. [#9913](https://github.com/JabRef/jabref/issues/9913)
- We fixed an issue where the encoding header in a bib file was not respected when the file contained a BOM (Byte Order Mark). [#9926](https://github.com/JabRef/jabref/issues/9926)
- We fixed an issue where cli help output for import and export format was inconsistent. [koppor#429](https://github.com/koppor/jabref/issues/429)
- We fixed an issue where the user could select multiple conflicting options for autocompletion at once. [#10181](https://github.com/JabRef/jabref/issues/10181)
- We fixed an issue where no preview could be generated for some entry types and led to an exception. [#9947](https://github.com/JabRef/jabref/issues/9947)
- We fixed an issue where the Linux terminal working directory argument was malformed and therefore ignored upon opening a terminal [#9953](https://github.com/JabRef/jabref/issues/9953)
- We fixed an issue under Linux where under some systems the file instead of the folder was opened. [#9607](https://github.com/JabRef/jabref/issues/9607)
- We fixed an issue where an Automatic Keyword Group could not be deleted in the UI. [#9778](https://github.com/JabRef/jabref/issues/9778)
- We fixed an issue where the citation key pattern `[edtrN_M]` returned the wrong editor. [#9946](https://github.com/JabRef/jabref/pull/9946)
- We fixed an issue where empty grey containers would remain in the groups panel, if displaying of group item count is turned off. [#9972](https://github.com/JabRef/jabref/issues/9972)
- We fixed an issue where fetching an ISBN could lead to application freezing when the fetcher did not return any results. [#9979](https://github.com/JabRef/jabref/issues/9979)
- We fixed an issue where closing a library containing groups and entries caused an exception [#9997](https://github.com/JabRef/jabref/issues/9997)
- We fixed a bug where the editor for strings in a bibliography file did not sort the entries by their keys [#10083](https://github.com/JabRef/jabref/pull/10083)
- We fixed an issues where clicking on the empty space of specific context menu entries would not trigger the associated action. [#8388](https://github.com/JabRef/jabref/issues/8388)
- We fixed an issue where JabRef would not remember whether the window was in fullscreen. [#4939](https://github.com/JabRef/jabref/issues/4939)
- We fixed an issue where the ACM Portal search sometimes would not return entries for some search queries when the article author had no given name. [#10107](https://github.com/JabRef/jabref/issues/10107)
- We fixed an issue that caused high CPU usage and a zombie process after quitting JabRef because of author names autocompletion. [#10159](https://github.com/JabRef/jabref/pull/10159)
- We fixed an issue where files with illegal characters in the filename could be added to JabRef. [#10182](https://github.com/JabRef/jabref/issues/10182)
- We fixed that checked-out radio buttons under "specified keywords" were not displayed as checked after closing and reopening the "edit group" window. [#10248](https://github.com/JabRef/jabref/issues/10248)
- We fixed that when editing groups, checked-out properties such as case sensitive and regular expression (under "Free search expression") were not displayed checked. [#10108](https://github.com/JabRef/jabref/issues/10108)

### Removed

- We removed the support of BibTeXML. [#9540](https://github.com/JabRef/jabref/issues/9540)
- We removed support for Markdown syntax for strikethrough and task lists in comment fields. [#9726](https://github.com/JabRef/jabref/pull/9726)
- We removed the options menu, because the two contents were moved to the File menu or the properties of the library. [#9768](https://github.com/JabRef/jabref/pull/9768)
- We removed the 'File' tab in the preferences and moved its contents to the 'Export' tab. [#9839](https://github.com/JabRef/jabref/pull/9839)
- We removed the "[Collection of Computer Science Bibliographies](https://en.wikipedia.org/wiki/Collection_of_Computer_Science_Bibliographies)" fetcher the websits is no longer available. [#6638](https://github.com/JabRef/jabref/issues/6638)

## [5.9] – 2023-01-06

### Added

- We added a dropdown menu to let users change the library they want to import into during import. [#6177](https://github.com/JabRef/jabref/issues/6177)
- We added the possibility to add/remove a preview style from the selected list using a double click. [#9490](https://github.com/JabRef/jabref/issues/9490)
- We added the option to define fields as "multine" directly in the custom entry types dialog. [#6448](https://github.com/JabRef/jabref/issues/6448)
- We changed the minWidth and the minHeight of the main window, so it won't have a width and/or a height with the value 0. [#9606](https://github.com/JabRef/jabref/issues/9606)

### Changed

- We changed database structure: in MySQL/MariaDB we renamed tables by adding a `JABREF_` prefix, and in PGSQL we moved tables in `jabref` schema. We added `VersionDBStructure` variable in `METADATA` table to indicate current version of structure, this variable is needed for automatic migration. [#9312](https://github.com/JabRef/jabref/issues/9312)
- We moved some preferences options to a new tab in the preferences dialog. [#9442](https://github.com/JabRef/jabref/pull/9308)
- We renamed "Medline abbreviation" to "dotless abbreviation". [#9504](https://github.com/JabRef/jabref/pull/9504)
- We now have more "dots" in the offered journal abbreviations. [#9504](https://github.com/JabRef/jabref/pull/9504)
- We now disable the button "Full text search" in the Searchbar by default [#9527](https://github.com/JabRef/jabref/pull/9527)


### Fixed

- The tab "deprecated fields" is shown in biblatex-mode only. [#7757](https://github.com/JabRef/jabref/issues/7757)
- In case a journal name of an IEEE journal is abbreviated, the "normal" abbreviation is used - and not the one of the IEEE BibTeX strings. [abbrv#91](https://github.com/JabRef/abbrv.jabref.org/issues/91)
- We fixed a performance issue when loading large lists of custom journal abbreviations. [#8928](https://github.com/JabRef/jabref/issues/8928)
- We fixed an issue where the last opened libraries were not remembered when a new unsaved library was open as well. [#9190](https://github.com/JabRef/jabref/issues/9190)
- We fixed an issue where no context menu for the group "All entries" was present. [forum#3682](https://discourse.jabref.org/t/how-sort-groups-a-z-not-subgroups/3682)
- We fixed an issue where extra curly braces in some fields would trigger an exception when selecting the entry or doing an integrity check. [#9475](https://github.com/JabRef/jabref/issues/9475), [#9503](https://github.com/JabRef/jabref/issues/9503)
- We fixed an issue where entering a date in the format "YYYY/MM" in the entry editor date field caused an exception. [#9492](https://github.com/JabRef/jabref/issues/9492)
- For portable versions, the `.deb` file now works on plain debian again. [#9472](https://github.com/JabRef/jabref/issues/9472)
- We fixed an issue where the download of linked online files failed after an import of entries for certain urls. [#9518](https://github.com/JabRef/jabref/issues/9518)
- We fixed an issue where an exception occurred when manually downloading a file from an URL in the entry editor. [#9521](https://github.com/JabRef/jabref/issues/9521)
- We fixed an issue with open office csv file formatting where commas in the abstract field where not escaped. [#9087](https://github.com/JabRef/jabref/issues/9087)
- We fixed an issue with deleting groups where subgroups different from the selected group were deleted. [#9281](https://github.com/JabRef/jabref/issues/9281)

## [5.8] – 2022-12-18

### Added

- We integrated a new three-way merge UI for merging entries in the Entries Merger Dialog, the Duplicate Resolver Dialog, the Entry Importer Dialog, and the External Changes Resolver Dialog. [#8945](https://github.com/JabRef/jabref/pull/8945)
- We added the ability to merge groups, keywords, comments and files when merging entries. [#9022](https://github.com/JabRef/jabref/pull/9022)
- We added a warning message next to the authors field in the merge dialog to warn users when the authors are the same but formatted differently. [#8745](https://github.com/JabRef/jabref/issues/8745)
- The default file directory of a library is used as default directory for [unlinked file lookup](https://docs.jabref.org/collect/findunlinkedfiles#link-the-pdfs-to-your-bib-library). [koppor#546](https://github.com/koppor/jabref/issues/546)
- The properties of an existing systematic literature review (SLR) can be edited. [koppor#604](https://github.com/koppor/jabref/issues/604)
- An systematic literature review (SLR) can now be started from the SLR itself. [#9131](https://github.com/JabRef/jabref/pull/9131), [koppor#601](https://github.com/koppor/jabref/issues/601)
- On startup, JabRef notifies the user if there were parsing errors during opening.
- We added support for the field `fjournal` (in `@article`) for abbreviation and unabbreviation functionalities. [#321](https://github.com/JabRef/jabref/pull/321)
- In case a backup is found, the filename of the backup is shown and one can navigate to the file. [#9311](https://github.com/JabRef/jabref/pull/9311)
- We added support for the Ukrainian and Arabic languages. [#9236](https://github.com/JabRef/jabref/pull/9236), [#9243](https://github.com/JabRef/jabref/pull/9243)

### Changed

- We improved the Citavi Importer to also import so called Knowledge-items into the field `comment` of the corresponding entry [#9025](https://github.com/JabRef/jabref/issues/9025)
- We modified the change case sub-menus and their corresponding tips (displayed when you stay long over the menu) to properly reflect exemplified cases. [#9339](https://github.com/Jabref/jabref/issues/9339)
- We call backup files `.bak` and temporary writing files now `.sav`.
- JabRef keeps 10 older versions of a `.bib` file in the [user data dir](https://github.com/harawata/appdirs#supported-directories) (instead of a single `.sav` (now: `.bak`) file in the directory of the `.bib` file)
- We improved the External Changes Resolver dialog to be more usaable. [#9021](https://github.com/JabRef/jabref/pull/9021)
- We simplified the actions to fast-resolve duplicates to 'Keep Left', 'Keep Right', 'Keep Both' and 'Keep Merged'. [#9056](https://github.com/JabRef/jabref/issues/9056)
- The fallback directory of the file folder now is the general file directory. In case there was a directory configured for a library and this directory was not found, JabRef placed the PDF next to the .bib file and not into the general file directory.
- The global default directory for storing PDFs is now the documents folder in the user's home.
- When adding or editing a subgroup it is placed w.r.t. to alphabetical ordering rather than at the end. [koppor#577](https://github.com/koppor/jabref/issues/577)
- Groups context menu now shows appropriate options depending on number of subgroups. [koppor#579](https://github.com/koppor/jabref/issues/579)
- We modified the "Delete file" dialog and added the full file path to the dialog text. The file path in the title was changed to file name only. [koppor#534](https://github.com/koppor/jabref/issues/534)
- Download from URL now automatically fills with URL from clipboard. [koppor#535](https://github.com/koppor/jabref/issues/535)
- We added HTML and Markdown files to Find Unlinked Files and removed BibTeX. [koppor#547](https://github.com/koppor/jabref/issues/547)
- ArXiv fetcher now retrieves additional data from related DOIs (both ArXiv and user-assigned). [#9170](https://github.com/JabRef/jabref/pull/9170)
- We modified the Directory of Open Access Books (DOAB) fetcher so that it will now also fetch the ISBN when possible. [#8708](https://github.com/JabRef/jabref/issues/8708)
- Genres are now mapped correctly to entry types when importing MODS files. [#9185](https://github.com/JabRef/jabref/issues/9185)
- We changed the button label from "Return to JabRef" to "Return to library" to better indicate the purpose of the action.
- We changed the color of found text from red to high-contrast colors (background: yellow; font color: purple). [koppor#552](https://github.com/koppor/jabref/issues/552)
- We fixed an issue where the wrong icon for a successful import of a bib entry was shown. [#9308](https://github.com/JabRef/jabref/pull/9308)
- We changed the messages after importing unlinked local files to past tense. [koppor#548](https://github.com/koppor/jabref/issues/548)
- We fixed an issue where the wrong icon for a successful import of a bib entry was shown [#9308](https://github.com/JabRef/jabref/pull/9308)
- In the context of the [Cleanup dialog](https://docs.jabref.org/finding-sorting-and-cleaning-entries/cleanupentries) we changed the text of the conversion of BibTeX to biblatex (and vice versa) to make it more clear. [koppor#545](https://github.com/koppor/jabref/issues/545)
- We removed wrapping of string constants when writing to a `.bib` file.
- In the context of a systematic literature review (SLR), a user can now add arbitrary data into `study.yml`. JabRef just ignores this data. [#9124](https://github.com/JabRef/jabref/pull/9124)
- In the context of a systematic literature review (SLR), we reworked the "Define study" parameters dialog. [#9123](https://github.com/JabRef/jabref/pull/9123)
- We upgraded to Lucene 9.4 for the fulltext search. The search index will be rebuild. [#9213](https://github.com/JabRef/jabref/pull/9213)
- We disabled the "change case" menu for empty fields. [#9214](https://github.com/JabRef/jabref/issues/9214)
- We disabled the conversion menu for empty fields. [#9200](https://github.com/JabRef/jabref/issues/9200)

### Fixed

- We fixed an issue where applied save actions on saving the library file would lead to the dialog "The library has been modified by another program" popping up. [#4877](https://github.com/JabRef/jabref/issues/4877)
- We fixed issues with save actions not correctly loaded when opening the library. [#9122](https://github.com/JabRef/jabref/pull/9122)
- We fixed the behavior of "Discard changes" when reopening a modified library. [#9361](https://github.com/JabRef/jabref/issues/9361)
- We fixed several bugs regarding the manual and the autosave of library files that could lead to exceptions. [#9067](https://github.com/JabRef/jabref/pull/9067), [#8484](https://github.com/JabRef/jabref/issues/8484), [#8746](https://github.com/JabRef/jabref/issues/8746), [#6684](https://github.com/JabRef/jabref/issues/6684), [#6644](https://github.com/JabRef/jabref/issues/6644), [#6102](https://github.com/JabRef/jabref/issues/6102), [#6000](https://github.com/JabRef/jabref/issues/6000)
- We fixed an issue where pdfs were re-indexed on each startup. [#9166](https://github.com/JabRef/jabref/pull/9166)
- We fixed an issue when using an unsafe character in the citation key, the auto-linking feature fails to link files. [#9267](https://github.com/JabRef/jabref/issues/9267)
- We fixed an issue where a message about changed metadata would occur on saving although nothing changed. [#9159](https://github.com/JabRef/jabref/issues/9159)
- We fixed an issue where the possibility to generate a subdatabase from an aux file was writing empty files when called from the commandline. [#9115](https://github.com/JabRef/jabref/issues/9115), [forum#3516](https://discourse.jabref.org/t/export-subdatabase-from-aux-file-on-macos-command-line/3516)
- We fixed an issue where author names with tilde accents (for example ñ) were marked as "Names are not in the standard BibTeX format". [#8071](https://github.com/JabRef/jabref/issues/8071)
- We fixed an issue where capitalize didn't capitalize words after hyphen characters. [#9157](https://github.com/JabRef/jabref/issues/9157)
- We fixed an issue where title case didn't capitalize words after en-dash characters and skip capitalization of conjunctions that comes after en-dash characters. [#9068](https://github.com/JabRef/jabref/pull/9068),[#9142](https://github.com/JabRef/jabref/pull/9142)
- We fixed an issue with the message that is displayed when fetcher returns an empty list of entries for given query. [#9195](https://github.com/JabRef/jabref/issues/9195)
- We fixed an issue where editing entry's "date" field in library mode "biblatex" causes an uncaught exception. [#8747](https://github.com/JabRef/jabref/issues/8747)
- We fixed an issue where importing from XMP would fail for certain PDFs. [#9383](https://github.com/JabRef/jabref/issues/9383)
- We fixed an issue that JabRef displayed the wrong group tree after loading. [koppor#637](https://github.com/koppor/jabref/issues/637)
- We fixed that sorting of entries in the maintable by special fields is updated immediately. [#9334](https://github.com/JabRef/jabref/issues/9334)
- We fixed the display of issue, number, eid and pages fields in the entry preview. [#8607](https://github.com/JabRef/jabref/pull/8607), [#8372](https://github.com/JabRef/jabref/issues/8372), [Koppor#514](https://github.com/koppor/jabref/issues/514), [forum#2390](https://discourse.jabref.org/t/unable-to-edit-my-bibtex-file-that-i-used-before-vers-5-1/2390), [forum#3462](https://discourse.jabref.org/t/jabref-5-6-need-help-with-export-from-jabref-to-microsoft-word-entry-preview-of-apa-7-not-rendering-correctly/3462)
- We fixed the page ranges checker to detect article numbers in the pages field (used at [Check Integrity](https://docs.jabref.org/finding-sorting-and-cleaning-entries/checkintegrity)). [#8607](https://github.com/JabRef/jabref/pull/8607)
- The [HtmlToLaTeXFormatter](https://docs.jabref.org/finding-sorting-and-cleaning-entries/saveactions#html-to-latex) keeps single `<` characters.
- We fixed a performance regression when opening large libraries. [#9041](https://github.com/JabRef/jabref/issues/9041)
- We fixed a bug where spaces are trimmed when highlighting differences in the Entries merge dialog. [koppor#371](https://github.com/koppor/jabref/issues/371)
- We fixed some visual glitches with the linked files editor field in the entry editor and increased its height. [#8823](https://github.com/JabRef/jabref/issues/8823)
- We fixed some visual inconsistencies (round corners of highlighted buttons). [#8806](https://github.com/JabRef/jabref/issues/8806)
- We fixed an issue where JabRef would not exit when a connection to a LibreOffice document was established previously and the document is still open. [#9075](https://github.com/JabRef/jabref/issues/9075)
- We fixed an issue about selecting the save order in the preferences. [#9147](https://github.com/JabRef/jabref/issues/9147)
- We fixed an issue where an exception when fetching a DOI was not logged correctly. [koppor#627](https://github.com/koppor/jabref/issues/627)
- We fixed an issue where a user could not open an attached file in a new unsaved library. [#9386](https://github.com/JabRef/jabref/issues/9386)
- We fixed a typo within a connection error message. [koppor#625](https://github.com/koppor/jabref/issues/625)
- We fixed an issue where journal abbreviations would not abbreviate journal titles with escaped ampersands (\\&). [#8948](https://github.com/JabRef/jabref/issues/8948)
- We fixed the readability of the file field in the dark theme. [#9340](https://github.com/JabRef/jabref/issues/9340)
- We fixed an issue where the 'close dialog' key binding was not closing the Preferences dialog. [#8888](https://github.com/jabref/jabref/issues/8888)
- We fixed an issue where a known journal's medline/dot-less abbreviation does not switch to the full name. [#9370](https://github.com/JabRef/jabref/issues/9370)
- We fixed an issue where hitting enter on the search field within the preferences dialog closed the dialog. [koppor#630](https://github.com/koppor/jabref/issues/630)
- We fixed the "Cleanup entries" dialog is partially visible. [#9223](https://github.com/JabRef/jabref/issues/9223)
- We fixed an issue where font size preferences did not apply correctly to preference dialog window and the menu bar. [#8386](https://github.com/JabRef/jabref/issues/8386) and [#9279](https://github.com/JabRef/jabref/issues/9279)
- We fixed the display of the "Customize Entry Types" dialog title. [#9198](https://github.com/JabRef/jabref/issues/9198)
- We fixed an issue where the CSS styles are missing in some dialogs. [#9150](https://github.com/JabRef/jabref/pull/9150)
- We fixed an issue where controls in the preferences dialog could outgrow the window. [#9017](https://github.com/JabRef/jabref/issues/9017)
- We fixed an issue where highlighted text color for entry merge dialogue was not clearly visible. [#9192](https://github.com/JabRef/jabref/issues/9192)

### Removed

- We removed "last-search-date" from the systematic literature review feature, because the last-search-date can be deducted from the git logs. [#9116](https://github.com/JabRef/jabref/pull/9116)
- We removed the [CiteseerX](https://docs.jabref.org/collect/import-using-online-bibliographic-database#citeseerx) fetcher, because the API used by JabRef is sundowned. [#9466](https://github.com/JabRef/jabref/pull/9466)

## [5.7] – 2022-08-05

### Added

- We added a fetcher for [Biodiversity Heritage Library](https://www.biodiversitylibrary.org/). [8539](https://github.com/JabRef/jabref/issues/8539)
- We added support for multiple messages in the snackbar. [#7340](https://github.com/JabRef/jabref/issues/7340)
- We added an extra option in the 'Find Unlinked Files' dialog view to ignore unnecessary files like Thumbs.db, DS_Store, etc. [koppor#373](https://github.com/koppor/jabref/issues/373)
- JabRef now writes log files. Linux: `$home/.cache/jabref/logs/version`, Windows: `%APPDATA%\..\Local\harawata\jabref\version\logs`, Mac: `Users/.../Library/Logs/jabref/version`
- We added an importer for Citavi backup files, support ".ctv5bak" and ".ctv6bak" file formats. [#8322](https://github.com/JabRef/jabref/issues/8322)
- We added a feature to drag selected entries and drop them to other opened inactive library tabs [koppor521](https://github.com/koppor/jabref/issues/521).
- We added support for the [biblatex-apa](https://github.com/plk/biblatex-apa) legal entry types `Legislation`, `Legadminmaterial`, `Jurisdiction`, `Constitution` and `Legal` [#8931](https://github.com/JabRef/jabref/issues/8931)

### Changed

- The file column in the main table now shows the corresponding defined icon for the linked file [8930](https://github.com/JabRef/jabref/issues/8930).
- We improved the color of the selected entries and the color of the summary in the Import Entries Dialog in the dark theme. [#7927](https://github.com/JabRef/jabref/issues/7927)
- We upgraded to Lucene 9.2 for the fulltext search.
  Thus, the now created search index cannot be read from older versions of JabRef anylonger.
  ⚠️ JabRef will recreate the index in a new folder for new files and this will take a long time for a huge library.
  Moreover, switching back and forth JabRef versions and meanwhile adding PDFs also requires rebuilding the index now and then.
  [#8868](https://github.com/JabRef/jabref/pull/8868)
- We improved the Latex2Unicode conversion [#8639](https://github.com/JabRef/jabref/pull/8639)
- Writing BibTeX data into a PDF (XMP) removes braces. [#8452](https://github.com/JabRef/jabref/issues/8452)
- Writing BibTeX data into a PDF (XMP) does not write the `file` field.
- Writing BibTeX data into a PDF (XMP) considers the configured keyword separator (and does not use "," as default any more)
- The Medline/Pubmed search now also supports the [default fields and operators for searching](https://docs.jabref.org/collect/import-using-online-bibliographic-database#search-syntax). [forum#3554](https://discourse.jabref.org/t/native-pubmed-search/3354)
- We improved group expansion arrow that prevent it from activating group when expanding or collapsing. [#7982](https://github.com/JabRef/jabref/issues/7982), [#3176](https://github.com/JabRef/jabref/issues/3176)
- When configured SSL certificates changed, JabRef warns the user to restart to apply the configuration.
- We improved the appearances and logic of the "Manage field names & content" dialog, and renamed it to "Automatic field editor". [#6536](https://github.com/JabRef/jabref/issues/6536)
- We improved the message explaining the options when modifying an automatic keyword group [#8911](https://github.com/JabRef/jabref/issues/8911)
- We moved the preferences option "Warn about duplicates on import" option from the tab "File" to the tab "Import and Export". [koppor#570](https://github.com/koppor/jabref/issues/570)
- When JabRef encounters `% Encoding: UTF-8` header, it is kept during writing (and not removed). [#8964](https://github.com/JabRef/jabref/pull/8964)
- We replace characters which cannot be decoded using the specified encoding by a (probably another) valid character. This happens if JabRef detects the wrong charset (e.g., UTF-8 instead of Windows 1252). One can use the [Integrity Check](https://docs.jabref.org/finding-sorting-and-cleaning-entries/checkintegrity) to find those characters.

### Fixed

- We fixed an issue where linked fails containing parts of the main file directory could not be opened. [#8991](https://github.com/JabRef/jabref/issues/8991)
- Linked files with an absolute path can be opened again. [#8991](https://github.com/JabRef/jabref/issues/8991)
- We fixed an issue where the user could not rate an entry in the main table when an entry was not yet ranked. [#5842](https://github.com/JabRef/jabref/issues/5842)
- We fixed an issue that caused JabRef to sometimes open multiple instances when "Remote Operation" is enabled. [#8653](https://github.com/JabRef/jabref/issues/8653)
- We fixed an issue where linked files with the filetype "application/pdf" in an entry were not shown with the correct PDF-Icon in the main table [8930](https://github.com/JabRef/jabref/issues/8930)
- We fixed an issue where "open folder" for linked files did not open the folder and did not select the file unter certain Linux desktop environments [#8679](https://github.com/JabRef/jabref/issues/8679), [#8849](https://github.com/JabRef/jabref/issues/8849)
- We fixed an issue where the content of a big shared database library is not shown [#8788](https://github.com/JabRef/jabref/issues/8788)
- We fixed the unnecessary horizontal scroll bar in group panel [#8467](https://github.com/JabRef/jabref/issues/8467)
- We fixed an issue where the notification bar message, icon and actions appeared to be invisible. [#8761](https://github.com/JabRef/jabref/issues/8761)
- We fixed an issue where deprecated fields tab is shown when the fields don't contain any values. [#8396](https://github.com/JabRef/jabref/issues/8396)
- We fixed an issue where an exception for DOI search occurred when the DOI contained urlencoded characters. [#8787](https://github.com/JabRef/jabref/issues/8787)
- We fixed an issue which allow us to select and open identifiers from a popup list in the maintable [#8758](https://github.com/JabRef/jabref/issues/8758), [8802](https://github.com/JabRef/jabref/issues/8802)
- We fixed an issue where the escape button had no functionality within the "Filter groups" textfield. [koppor#562](https://github.com/koppor/jabref/issues/562)
- We fixed an issue where the exception that there are invalid characters in filename. [#8786](https://github.com/JabRef/jabref/issues/8786)
- When the proxy configuration removed the proxy user/password, this change is applied immediately.
- We fixed an issue where removing several groups deletes only one of them. [#8390](https://github.com/JabRef/jabref/issues/8390)
- We fixed an issue where the Sidepane (groups, web search and open office) width is not remembered after restarting JabRef. [#8907](https://github.com/JabRef/jabref/issues/8907)
- We fixed a bug where switching between themes will cause an error/exception. [#8939](https://github.com/JabRef/jabref/pull/8939)
- We fixed a bug where files that were deleted in the source bibtex file were kept in the index. [#8962](https://github.com/JabRef/jabref/pull/8962)
- We fixed "Error while sending to JabRef" when the browser extension interacts with JabRef. [JabRef-Browser-Extension#479](https://github.com/JabRef/JabRef-Browser-Extension/issues/479)
- We fixed a bug where updating group view mode (intersection or union) requires re-selecting groups to take effect. [#6998](https://github.com/JabRef/jabref/issues/6998)
- We fixed a bug that prevented external group metadata changes from being merged. [#8873](https://github.com/JabRef/jabref/issues/8873)
- We fixed the shared database opening dialog to remember autosave folder and tick. [#7516](https://github.com/JabRef/jabref/issues/7516)
- We fixed an issue where name formatter could not be saved. [#9120](https://github.com/JabRef/jabref/issues/9120)
- We fixed a bug where after the export of Preferences, custom exports were duplicated. [#10176](https://github.com/JabRef/jabref/issues/10176)

### Removed

- We removed the social media buttons for our Twitter and Facebook pages. [#8774](https://github.com/JabRef/jabref/issues/8774)

## [5.6] – 2022-04-25

### Added

- We enabled the user to customize the API Key for some fetchers. [#6877](https://github.com/JabRef/jabref/issues/6877)
- We added an extra option when right-clicking an entry in the Entry List to copy either the DOI or the DOI url.
- We added a fetcher for [Directory of Open Access Books (DOAB)](https://doabooks.org/) [8576](https://github.com/JabRef/jabref/issues/8576)
- We added an extra option to ask the user whether they want to open to reveal the folder holding the saved file with the file selected. [#8195](https://github.com/JabRef/jabref/issues/8195)
- We added a new section to network preferences to allow using custom SSL certificates. [#8126](https://github.com/JabRef/jabref/issues/8126)
- We improved the version check to take also beta version into account and now redirect to the right changelog for the version.
- We added two new web and fulltext fetchers: SemanticScholar and ResearchGate.
- We added notifications on success and failure when writing metadata to a PDF-file. [#8276](https://github.com/JabRef/jabref/issues/8276)
- We added a cleanup action that escapes `$` (by adding a backslash in front). [#8673](https://github.com/JabRef/jabref/issues/8673)

### Changed

- We upgraded to Lucene 9.1 for the fulltext search.
  Thus, the now created search index cannot be read from older versions of JabRef any longer.
  ⚠️ JabRef will recreate the index in a new folder for new files and this will take a long time for a huge library.
  Moreover, switching back and forth JabRef versions and meanwhile adding PDFs also requires rebuilding the index now and then.
  [#8362](https://github.com/JabRef/jabref/pull/8362)
- We changed the list of CSL styles to those that support formatting bibliographies. [#8421](https://github.com/JabRef/jabref/issues/8421) [citeproc-java#116](https://github.com/michel-kraemer/citeproc-java/issues/116)
- The CSL preview styles now also support displaying data from cross references entries that are linked via the `crossref` field. [#7378](https://github.com/JabRef/jabref/issues/7378)
- We made the Search button in Web Search wider. We also skewed the panel titles to the left. [#8397](https://github.com/JabRef/jabref/issues/8397)
- We introduced a preference to disable fulltext indexing. [#8468](https://github.com/JabRef/jabref/issues/8468)
- When exporting entries, the encoding is always UTF-8.
- When embedding BibTeX data into a PDF, the encoding is always UTF-8.
- We replaced the [OttoBib](https://en.wikipedia.org/wiki/OttoBib) fetcher by a fetcher by [OpenLibrary](https://openlibrary.org/dev/docs/api/books). [#8652](https://github.com/JabRef/jabref/issues/8652)
- We first fetch ISBN data from OpenLibrary, if nothing found, ebook.de is tried.
- We now only show a warning when exiting for tasks that will not be recovered automatically upon relaunch of JabRef. [#8468](https://github.com/JabRef/jabref/issues/8468)

### Fixed

- We fixed an issue where right clicking multiple entries and pressing "Change entry type" would only change one entry. [#8654](https://github.com/JabRef/jabref/issues/8654)
- We fixed an issue where it was no longer possible to add or delete multiple files in the `file` field in the entry editor. [#8659](https://github.com/JabRef/jabref/issues/8659)
- We fixed an issue where the author's lastname was not used for the citation key generation if it started with a lowercase letter. [#8601](https://github.com/JabRef/jabref/issues/8601)
- We fixed an issue where custom "Protected terms" files were missing after a restart of JabRef. [#8608](https://github.com/JabRef/jabref/issues/8608)
- We fixed an issue where JabRef could not start due to a missing directory for the fulltex index. [#8579](https://github.com/JabRef/jabref/issues/8579)
- We fixed an issue where long article numbers in the `pages` field would cause an exception and preventing the citation style to display. [#8381](https://github.com/JabRef/jabref/issues/8381), [citeproc-java](https://github.com/michel-kraemer/citeproc-java/issues/114)
- We fixed an issue where online links in the file field were not detected correctly and could produce an exception. [#8510](https://github.com/JabRef/jabref/issues/8510)
- We fixed an issue where an exception could occur when saving the preferences [#7614](https://github.com/JabRef/jabref/issues/7614)
- We fixed an issue where "Copy DOI url" in the right-click menu of the Entry List would just copy the DOI and not the DOI url. [#8389](https://github.com/JabRef/jabref/issues/8389)
- We fixed an issue where opening the console from the drop-down menu would cause an exception. [#8466](https://github.com/JabRef/jabref/issues/8466)
- We fixed an issue when reading non-UTF-8 encoded. When no encoding header is present, the encoding is now detected from the file content (and the preference option is disregarded). [#8417](https://github.com/JabRef/jabref/issues/8417)
- We fixed an issue where pasting a URL was replacing `+` signs by spaces making the URL unreachable. [#8448](https://github.com/JabRef/jabref/issues/8448)
- We fixed an issue where creating subsidiary files from aux files created with some versions of biblatex would produce incorrect results. [#8513](https://github.com/JabRef/jabref/issues/8513)
- We fixed an issue where opening the changelog from withing JabRef led to a 404 error. [#8563](https://github.com/JabRef/jabref/issues/8563)
- We fixed an issue where not all found unlinked local files were imported correctly due to some race condition. [#8444](https://github.com/JabRef/jabref/issues/8444)
- We fixed an issue where Merge entries dialog exceeds screen boundaries.
- We fixed an issue where the app lags when selecting an entry after a fresh start. [#8446](https://github.com/JabRef/jabref/issues/8446)
- We fixed an issue where no citationkey was generated on import, pasting a doi or an entry on the main table. [8406](https://github.com/JabRef/jabref/issues/8406), [koppor#553](https://github.com/koppor/jabref/issues/553)
- We fixed an issue where accent search does not perform consistently. [#6815](https://github.com/JabRef/jabref/issues/6815)
- We fixed an issue where the incorrect entry was selected when "New Article" is pressed while search filters are active. [#8674](https://github.com/JabRef/jabref/issues/8674)
- We fixed an issue where "Write BibTeXEntry metadata to PDF" button remains enabled while writing to PDF is in-progress. [#8691](https://github.com/JabRef/jabref/issues/8691)

### Removed

- We removed the option to copy CSL Citation styles data as `XSL_FO`, `ASCIIDOC`, and `RTF` as these have not been working since a long time and are no longer supported in the external library used for processing the styles. [#7378](https://github.com/JabRef/jabref/issues/7378)
- We removed the option to configure the default encoding. The default encoding is now hard-coded to the modern UTF-8 encoding.

## [5.5] – 2022-01-17

### Changed

- We integrated the external file types dialog directly inside the preferences. [#8341](https://github.com/JabRef/jabref/pull/8341)
- We disabled the add group button color change after adding 10 new groups. [#8051](https://github.com/JabRef/jabref/issues/8051)
- We inverted the logic for resolving [BibTeX strings](https://docs.jabref.org/advanced/strings). This helps to keep `#` chars. By default String resolving is only activated for a couple of standard fields. The list of fields can be modified in the preferences. [#7010](https://github.com/JabRef/jabref/issues/7010), [#7012](https://github.com/JabRef/jabref/issues/7012), [#8303](https://github.com/JabRef/jabref/issues/8303)
- We moved the search box in preview preferences closer to the available citation styles list. [#8370](https://github.com/JabRef/jabref/pull/8370)
- Changing the preference to show the preview panel as a separate tab now has effect without restarting JabRef. [#8370](https://github.com/JabRef/jabref/pull/8370)
- We enabled switching themes in JabRef without the need to restart JabRef. [#7335](https://github.com/JabRef/jabref/pull/7335)
- We added support for the field `day`, `rights`, `coverage` and `language` when reading XMP data in Dublin Core format. [#8491](https://github.com/JabRef/jabref/issues/8491)

### Fixed

- We fixed an issue where the preferences for "Search and store files relative to library file location" where ignored when the "Main file directory" field was not empty [#8385](https://github.com/JabRef/jabref/issues/8385)
- We fixed an issue where `#`chars in certain fields would be interpreted as BibTeX strings [#7010](https://github.com/JabRef/jabref/issues/7010), [#7012](https://github.com/JabRef/jabref/issues/7012), [#8303](https://github.com/JabRef/jabref/issues/8303)
- We fixed an issue where the fulltext search on an empty library with no documents would lead to an exception [koppor#522](https://github.com/koppor/jabref/issues/522)
- We fixed an issue where clicking on "Accept changes" in the merge dialog would lead to an exception [forum#2418](https://discourse.jabref.org/t/the-library-has-been-modified-by-another-program/2418/8)
- We fixed an issue where clicking on headings in the entry preview could lead to an exception. [#8292](https://github.com/JabRef/jabref/issues/8292)
- We fixed an issue where IntegrityCheck used the system's character encoding instead of the one set by the library or in preferences [#8022](https://github.com/JabRef/jabref/issues/8022)
- We fixed an issue about empty metadata in library properties when called from the right click menu. [#8358](https://github.com/JabRef/jabref/issues/8358)
- We fixed an issue where someone could add a duplicate field in the customize entry type dialog. [#8194](https://github.com/JabRef/jabref/issues/8194)
- We fixed a typo in the library properties tab: "String constants". There, one can configure [BibTeX string constants](https://docs.jabref.org/advanced/strings).
- We fixed an issue when writing a non-UTF-8 encoded file: The header is written again. [#8417](https://github.com/JabRef/jabref/issues/8417)
- We fixed an issue where folder creation during systemic literature review failed due to an illegal fetcher name. [#8552](https://github.com/JabRef/jabref/pull/8552)

## [5.4] – 2021-12-20

### Added

- We added confirmation dialog when user wants to close a library where any empty entries are detected. [#8096](https://github.com/JabRef/jabref/issues/8096)
- We added import support for CFF files. [#7945](https://github.com/JabRef/jabref/issues/7945)
- We added the option to copy the DOI of an entry directly from the context menu copy submenu. [#7826](https://github.com/JabRef/jabref/issues/7826)
- We added a fulltext search feature. [#2838](https://github.com/JabRef/jabref/pull/2838)
- We improved the deduction of bib-entries from imported fulltext pdfs. [#7947](https://github.com/JabRef/jabref/pull/7947)
- We added unprotect_terms to the list of bracketed pattern modifiers [#7826](https://github.com/JabRef/jabref/pull/7960)
- We added a dialog that allows to parse metadata from linked pdfs. [#7929](https://github.com/JabRef/jabref/pull/7929)
- We added an icon picker in group edit dialog. [#6142](https://github.com/JabRef/jabref/issues/6142)
- We added a preference to Opt-In to JabRef's online metadata extraction service (Grobid) usage. [#8002](https://github.com/JabRef/jabref/pull/8002)
- We readded the possibility to display the search results of all databases ("Global Search"). It is shown in a separate window. [#4096](https://github.com/JabRef/jabref/issues/4096)
- We readded the possibility to keep the search string when switching tabs. It is implemented by a toggle button. [#4096](https://github.com/JabRef/jabref/issues/4096#issuecomment-575986882)
- We allowed the user to also preview the available citation styles in the preferences besides the selected ones [#8108](https://github.com/JabRef/jabref/issues/8108)
- We added an option to search the available citation styles by name in the preferences [#8108](https://github.com/JabRef/jabref/issues/8108)
- We added an option to generate bib-entries from ID through a popover in the toolbar. [#4183](https://github.com/JabRef/jabref/issues/4183)
- We added a menu option in the right click menu of the main table tabs to display the library properties. [#6527](https://github.com/JabRef/jabref/issues/6527)
- When a `.bib` file ("library") was saved successfully, a notification is shown

### Changed

- Local library settings may overwrite the setting "Search and store files relative to library file location" [#8179](https://github.com/JabRef/jabref/issues/8179)
- The option "Fit table horizontally on screen" in the "Entry table" preferences is now disabled by default [#8148](https://github.com/JabRef/jabref/pull/8148)
- We improved the preferences and descriptions in the "Linked files" preferences tab [#8148](https://github.com/JabRef/jabref/pull/8148)
- We slightly changed the layout of the Journal tab in the preferences for ui consistency. [#7937](https://github.com/JabRef/jabref/pull/7937)
- The JabRefHost on Windows now writes a temporary file and calls `-importToOpen` instead of passing the bibtex via `-importBibtex`. [#7374](https://github.com/JabRef/jabref/issues/7374), [JabRef Browser Ext #274](https://github.com/JabRef/JabRef-Browser-Extension/issues/274)
- We reordered some entries in the right-click menu of the main table. [#6099](https://github.com/JabRef/jabref/issues/6099)
- We merged the barely used ImportSettingsTab and the CustomizationTab in the preferences into one single tab and moved the option to allow Integers in Edition Fields in Bibtex-Mode to the EntryEditor tab. [#7849](https://github.com/JabRef/jabref/pull/7849)
- We moved the export order in the preferences from `File` to `Import and Export`. [#7935](https://github.com/JabRef/jabref/pull/7935)
- We reworked the export order in the preferences and the save order in the library preferences. You can now set more than three sort criteria in your library preferences. [#7935](https://github.com/JabRef/jabref/pull/7935)
- The metadata-to-pdf actions now also embeds the bibfile to the PDF. [#8037](https://github.com/JabRef/jabref/pull/8037)
- The snap was updated to use the core20 base and to use lzo compression for better startup performance [#8109](https://github.com/JabRef/jabref/pull/8109)
- We moved the union/intersection view button in the group sidepane to the left of the other controls. [#8202](https://github.com/JabRef/jabref/pull/8202)
- We improved the Drag and Drop behavior in the "Customize Entry Types" Dialog [#6338](https://github.com/JabRef/jabref/issues/6338)
- When determining the URL of an ArXiV eprint, the URL now points to the version [#8149](https://github.com/JabRef/jabref/pull/8149)
- We Included all standard fields with citation key when exporting to Old OpenOffice/LibreOffice Calc Format [#8176](https://github.com/JabRef/jabref/pull/8176)
- In case the database is encoded with `UTF8`, the `% Encoding` marker is not written anymore
- The written `.bib` file has the same line endings [#390](https://github.com/koppor/jabref/issues/390)
- The written `.bib` file always has a final line break
- The written `.bib` file keeps the newline separator of the loaded `.bib` file
- We present options to manually enter an article or return to the New Entry menu when the fetcher DOI fails to find an entry for an ID [#7870](https://github.com/JabRef/jabref/issues/7870)
- We trim white space and non-ASCII characters from DOI [#8127](https://github.com/JabRef/jabref/issues/8127)
- The duplicate checker now inspects other fields in case no difference in the required and optional fields are found.
- We reworked the library properties dialog and integrated the `Library > Preamble`, `Library > Citation key pattern` and `Library > String constants dialogs` [#8264](https://github.com/JabRef/jabref/pulls/8264)
- We improved the startup time of JabRef by switching from the logging library `log4j2` to `tinylog` [#8007](https://github.com/JabRef/jabref/issues/8007)

### Fixed

- We fixed an issue where an exception occurred when pasting an entry with a publication date-range of the form 1910/1917 [#7864](https://github.com/JabRef/jabref/issues/7864)
- We fixed an issue where an exception occurred when a preview style was edited and afterwards another preview style selected. [#8280](https://github.com/JabRef/jabref/issues/8280)
- We fixed an issue where the actions to move a file to a directory were incorrectly disabled. [#7908](https://github.com/JabRef/jabref/issues/7908)
- We fixed an issue where an exception occurred when a linked online file was edited in the entry editor [#8008](https://github.com/JabRef/jabref/issues/8008)
- We fixed an issue when checking for a new version when JabRef is used behind a corporate proxy. [#7884](https://github.com/JabRef/jabref/issues/7884)
- We fixed some icons that were drawn in the wrong color when JabRef used a custom theme. [#7853](https://github.com/JabRef/jabref/issues/7853)
- We fixed an issue where the `Aux file` on `Edit group` doesn't support relative sub-directories path to import. [#7719](https://github.com/JabRef/jabref/issues/7719).
- We fixed an issue where it was impossible to add or modify groups. [#7912](https://github.com/JabRef/jabref/pull/793://github.com/JabRef/jabref/pull/7921)
- We fixed an issue about the visible side pane components being out of sync with the view menu. [#8115](https://github.com/JabRef/jabref/issues/8115)
- We fixed an issue where the side pane would not close when all its components were closed. [#8082](https://github.com/JabRef/jabref/issues/8082)
- We fixed an issue where exported entries from a Citavi bib containing URLs could not be imported [#7882](https://github.com/JabRef/jabref/issues/7882)
- We fixed an issue where the icons in the search bar had the same color, toggled as well as untoggled. [#8014](https://github.com/JabRef/jabref/pull/8014)
- We fixed an issue where typing an invalid UNC path into the "Main file directory" text field caused an error. [#8107](https://github.com/JabRef/jabref/issues/8107)
- We fixed an issue where "Open Folder" didn't select the file on macOS in Finder [#8130](https://github.com/JabRef/jabref/issues/8130)
- We fixed an issue where importing PDFs resulted in an uncaught exception [#8143](https://github.com/JabRef/jabref/issues/8143)
- We fixed "The library has been modified by another program" showing up when line breaks change [#4877](https://github.com/JabRef/jabref/issues/4877)
- The default directory of the "LaTeX Citations" tab is now the directory of the currently opened database (and not the directory chosen at the last open file dialog or the last database save) [koppor#538](https://github.com/koppor/jabref/issues/538)
- When writing a bib file, the `NegativeArraySizeException` should not occur [#8231](https://github.com/JabRef/jabref/issues/8231) [#8265](https://github.com/JabRef/jabref/issues/8265)
- We fixed an issue where some menu entries were available without entries selected. [#4795](https://github.com/JabRef/jabref/issues/4795)
- We fixed an issue where right-clicking on a tab and selecting close will close the focused tab even if it is not the tab we right-clicked [#8193](https://github.com/JabRef/jabref/pull/8193)
- We fixed an issue where selecting a citation style in the preferences would sometimes produce an exception [#7860](https://github.com/JabRef/jabref/issues/7860)
- We fixed an issue where an exception would occur when clicking on a DOI link in the preview pane [#7706](https://github.com/JabRef/jabref/issues/7706)
- We fixed an issue where XMP and embedded BibTeX export would not work [#8278](https://github.com/JabRef/jabref/issues/8278)
- We fixed an issue where the XMP and embedded BibTeX import of a file containing multiple schemas failed [#8278](https://github.com/JabRef/jabref/issues/8278)
- We fixed an issue where writing embedded BibTeX import fails due to write protection or bibtex already being present [#8332](https://github.com/JabRef/jabref/pull/8332)
- We fixed an issue where pdf-paths and the pdf-indexer could get out of sync [#8182](https://github.com/JabRef/jabref/issues/8182)
- We fixed an issue where Status-Logger error messages appeared during the startup of JabRef [#5475](https://github.com/JabRef/jabref/issues/5475)

### Removed

- We removed two orphaned preferences options [#8164](https://github.com/JabRef/jabref/pull/8164)
- We removed the functionality of the `--debug` commandline options. Use the java command line switch `-Dtinylog.level=debug` for debug output instead. [#8226](https://github.com/JabRef/jabref/pull/8226)

## [5.3] – 2021-07-05

### Added

- We added a progress counter to the title bar in Possible Duplicates dialog window. [#7366](https://github.com/JabRef/jabref/issues/7366)
- We added new "Customization" tab to the preferences which includes option to choose a custom address for DOI access. [#7337](https://github.com/JabRef/jabref/issues/7337)
- We added zbmath to the public databases from which the bibliographic information of an existing entry can be updated. [#7437](https://github.com/JabRef/jabref/issues/7437)
- We showed to the find Unlinked Files Dialog the date of the files' most recent modification. [#4652](https://github.com/JabRef/jabref/issues/4652)
- We added to the find Unlinked Files function a filter to show only files based on date of last modification (Last Year, Last Month, Last Week, Last Day). [#4652](https://github.com/JabRef/jabref/issues/4652)
- We added to the find Unlinked Files function a filter that sorts the files based on the date of last modification(Sort by Newest, Sort by Oldest First). [#4652](https://github.com/JabRef/jabref/issues/4652)
- We added the possibility to add a new entry via its zbMath ID (zbMATH can be chosen as ID type in the "Select entry type" window). [#7202](https://github.com/JabRef/jabref/issues/7202)
- We added the extension support and the external application support (For Texshow, Texmaker and LyX) to the flatpak [#7248](https://github.com/JabRef/jabref/pull/7248)
- We added some symbols and keybindings to the context menu in the entry editor. [#7268](https://github.com/JabRef/jabref/pull/7268)
- We added keybindings for setting and clearing the read status. [#7264](https://github.com/JabRef/jabref/issues/7264)
- We added two new fields to track the creation and most recent modification date and time for each entry. [koppor#130](https://github.com/koppor/jabref/issues/130)
- We added a feature that allows the user to copy highlighted text in the preview window. [#6962](https://github.com/JabRef/jabref/issues/6962)
- We added a feature that allows you to create new BibEntry via paste arxivId [#2292](https://github.com/JabRef/jabref/issues/2292)
- We added support for conducting automated and systematic literature search across libraries and git support for persistence [#369](https://github.com/koppor/jabref/issues/369)
- We added a add group functionality at the bottom of the side pane. [#4682](https://github.com/JabRef/jabref/issues/4682)
- We added a feature that allows the user to choose whether to trust the target site when unable to find a valid certification path from the file download site. [#7616](https://github.com/JabRef/jabref/issues/7616)
- We added a feature that allows the user to open all linked files of multiple selected entries by "Open file" option. [#6966](https://github.com/JabRef/jabref/issues/6966)
- We added a keybinding preset for new entries. [#7705](https://github.com/JabRef/jabref/issues/7705)
- We added a select all button for the library import function. [#7786](https://github.com/JabRef/jabref/issues/7786)
- We added a search feature for journal abbreviations. [#7804](https://github.com/JabRef/jabref/pull/7804)
- We added auto-key-generation progress to the background task list. [#7267](https://github.com/JabRef/jabref/issues/7267)
- We added the option to write XMP metadata to pdfs from the CLI. [7814](https://github.com/JabRef/jabref/pull/7814)

### Changed

- The export to MS Office XML now exports the author field as `Inventor` if the bibtex entry type is `patent` [#7830](https://github.com/JabRef/jabref/issues/7830)
- We changed the EndNote importer to import the field `label` to the corresponding bibtex field `endnote-label` [forum#2734](https://discourse.jabref.org/t/importing-endnote-label-field-to-jabref-from-xml-file/2734)
- The keywords added via "Manage content selectors" are now displayed in alphabetical order. [#3791](https://github.com/JabRef/jabref/issues/3791)
- We improved the "Find unlinked files" dialog to show import results for each file. [#7209](https://github.com/JabRef/jabref/pull/7209)
- The content of the field `timestamp` is migrated to `creationdate`. In case one configured "udpate timestampe", it is migrated to `modificationdate`. [koppor#130](https://github.com/koppor/jabref/issues/130)
- The JabRef specific meta-data content in the main field such as priorities (prio1, prio2, ...) are migrated to their respective fields. They are removed from the keywords. [#6840](https://github.com/jabref/jabref/issues/6840)
- We fixed an issue where groups generated from authors' last names did not include all entries of the authors' [#5833](https://github.com/JabRef/jabref/issues/5833)
- The export to MS Office XML now uses the month name for the field `MonthAcessed` instead of the two digit number [#7354](https://github.com/JabRef/jabref/issues/7354)
- We included some standalone dialogs from the options menu in the main preference dialog and fixed some visual issues in the preferences dialog. [#7384](https://github.com/JabRef/jabref/pull/7384)
- We improved the linking of the `python3` interpreter via the shebang to dynamically use the systems default Python. Related to [JabRef-Browser-Extension #177](https://github.com/JabRef/JabRef-Browser-Extension/issues/177)
- Automatically found pdf files now have the linking button to the far left and uses a link icon with a plus instead of a briefcase. The file name also has lowered opacity(70%) until added. [#3607](https://github.com/JabRef/jabref/issues/3607)
- We simplified the select entry type form by splitting it into two parts ("Recommended" and "Others") based on internal usage data. [#6730](https://github.com/JabRef/jabref/issues/6730)
- We improved the submenu list by merging the'Remove group' having two options, with or without subgroups. [#4682](https://github.com/JabRef/jabref/issues/4682)
- The export to MS Office XML now uses the month name for the field `Month` instead of the two digit number [forum#2685](https://discourse.jabref.org/t/export-month-as-text-not-number/2685)
- We reintroduced missing default keybindings for new entries. [#7346](https://github.com/JabRef/jabref/issues/7346) [#7439](https://github.com/JabRef/jabref/issues/7439)
- Lists of available fields are now sorted alphabetically. [#7716](https://github.com/JabRef/jabref/issues/7716)
- The tooltip of the search field explaining the search is always shown. [#7279](https://github.com/JabRef/jabref/pull/7279)
- We rewrote the ACM fetcher to adapt to the new interface. [#5804](https://github.com/JabRef/jabref/issues/5804)
- We moved the select/collapse buttons in the unlinked files dialog into a context menu. [#7383](https://github.com/JabRef/jabref/issues/7383)
- We fixed an issue where journal abbreviations containing curly braces were not recognized [#7773](https://github.com/JabRef/jabref/issues/7773)

### Fixed

- We fixed an issue where some texts (e.g. descriptions) in dialogs could not be translated [#7854](https://github.com/JabRef/jabref/issues/7854)
- We fixed an issue where import hangs for ris files with "ER - " [#7737](https://github.com/JabRef/jabref/issues/7737)
- We fixed an issue where getting bibliograhpic data from DOI or another identifer did not respect the library mode (BibTeX/biblatex)[#6267](https://github.com/JabRef/jabref/issues/6267)
- We fixed an issue where importing entries would not respect the library mode (BibTeX/biblatex)[#1018](https://github.com/JabRef/jabref/issues/1018)
- We fixed an issue where an exception occurred when importing entries from a web search [#7606](https://github.com/JabRef/jabref/issues/7606)
- We fixed an issue where the table column sort order was not properly stored and resulted in unsorted eports [#7524](https://github.com/JabRef/jabref/issues/7524)
- We fixed an issue where the value of the field `school` or `institution` would be printed twice in the HTML Export [forum#2634](https://discourse.jabref.org/t/problem-with-exporting-techreport-phdthesis-mastersthesis-to-html/2634)
- We fixed an issue preventing to connect to a shared database. [#7570](https://github.com/JabRef/jabref/pull/7570)
- We fixed an issue preventing files from being dragged & dropped into an empty library. [#6851](https://github.com/JabRef/jabref/issues/6851)
- We fixed an issue where double-click onto PDF in file list under the 'General' tab section should just open the file. [#7465](https://github.com/JabRef/jabref/issues/7465)
- We fixed an issue where the dark theme did not extend to a group's custom color picker. [#7481](https://github.com/JabRef/jabref/issues/7481)
- We fixed an issue where choosing the fields on which autocompletion should not work in "Entry editor" preferences had no effect. [#7320](https://github.com/JabRef/jabref/issues/7320)
- We fixed an issue where the "Normalize page numbers" formatter did not replace en-dashes or em-dashes with a hyphen-minus sign. [#7239](https://github.com/JabRef/jabref/issues/7239)
- We fixed an issue with the style of highlighted check boxes while searching in preferences. [#7226](https://github.com/JabRef/jabref/issues/7226)
- We fixed an issue where the option "Move file to file directory" was disabled in the entry editor for all files [#7194](https://github.com/JabRef/jabref/issues/7194)
- We fixed an issue where application dialogs were opening in the wrong display when using multiple screens [#7273](https://github.com/JabRef/jabref/pull/7273)
- We fixed an issue where the "Find unlinked files" dialog would freeze JabRef on importing. [#7205](https://github.com/JabRef/jabref/issues/7205)
- We fixed an issue where the "Find unlinked files" would stop importing when importing a single file failed. [#7206](https://github.com/JabRef/jabref/issues/7206)
- We fixed an issue where JabRef froze for a few seconds in MacOS when DNS resolution timed out. [#7441](https://github.com/JabRef/jabref/issues/7441)
- We fixed an issue where an exception would be displayed for previewing and preferences when a custom theme has been configured but is missing [#7177](https://github.com/JabRef/jabref/issues/7177)
- We fixed an issue where URLs in `file` fields could not be handled on Windows. [#7359](https://github.com/JabRef/jabref/issues/7359)
- We fixed an issue where the regex based file search miss-interpreted specific symbols. [#4342](https://github.com/JabRef/jabref/issues/4342)
- We fixed an issue where the Harvard RTF exporter used the wrong default file extension. [4508](https://github.com/JabRef/jabref/issues/4508)
- We fixed an issue where the Harvard RTF exporter did not use the new authors formatter and therefore did not export "organization" authors correctly. [4508](https://github.com/JabRef/jabref/issues/4508)
- We fixed an issue where the field `urldate` was not exported to the corresponding fields `YearAccessed`, `MonthAccessed`, `DayAccessed` in MS Office XML [#7354](https://github.com/JabRef/jabref/issues/7354)
- We fixed an issue where the password for a shared SQL database was only remembered if it was the same as the username [#6869](https://github.com/JabRef/jabref/issues/6869)
- We fixed an issue where some custom exports did not use the new authors formatter and therefore did not export authors correctly [#7356](https://github.com/JabRef/jabref/issues/7356)
- We fixed an issue where alt+keyboard shortcuts do not work [#6994](https://github.com/JabRef/jabref/issues/6994)
- We fixed an issue about the file link editor did not allow to change the file name according to the default pattern after changing an entry. [#7525](https://github.com/JabRef/jabref/issues/7525)
- We fixed an issue where the file path is invisible in dark theme. [#7382](https://github.com/JabRef/jabref/issues/7382)
- We fixed an issue where the secondary sorting is not working for some special fields. [#7015](https://github.com/JabRef/jabref/issues/7015)
- We fixed an issue where changing the font size makes the font size field too small. [#7085](https://github.com/JabRef/jabref/issues/7085)
- We fixed an issue with TexGroups on Linux systems, where the modification of an aux-file did not trigger an auto-update for TexGroups. Furthermore, the detection of file modifications is now more reliable. [#7412](https://github.com/JabRef/jabref/pull/7412)
- We fixed an issue where the Unicode to Latex formatter produced wrong results for characters with a codepoint higher than Character.MAX_VALUE. [#7387](https://github.com/JabRef/jabref/issues/7387)
- We fixed an issue where a non valid value as font size results in an uncaught exception. [#7415](https://github.com/JabRef/jabref/issues/7415)
- We fixed an issue where "Merge citations" in the Openoffice/Libreoffice integration panel did not have a corresponding opposite. [#7454](https://github.com/JabRef/jabref/issues/7454)
- We fixed an issue where drag and drop of bib files for opening resulted in uncaught exceptions [#7464](https://github.com/JabRef/jabref/issues/7464)
- We fixed an issue where columns shrink in width when we try to enlarge JabRef window. [#6818](https://github.com/JabRef/jabref/issues/6818)
- We fixed an issue where Content selector does not seem to work for custom fields. [#6819](https://github.com/JabRef/jabref/issues/6819)
- We fixed an issue where font size of the preferences dialog does not update with the rest of the GUI. [#7416](https://github.com/JabRef/jabref/issues/7416)
- We fixed an issue in which a linked online file consisting of a web page was saved as an invalid pdf file upon being downloaded. The user is now notified when downloading a linked file results in an HTML file. [#7452](https://github.com/JabRef/jabref/issues/7452)
- We fixed an issue where opening BibTex file (doubleclick) from Folder with spaces not working. [#6487](https://github.com/JabRef/jabref/issues/6487)
- We fixed the header title in the Add Group/Subgroup Dialog box. [#4682](https://github.com/JabRef/jabref/issues/4682)
- We fixed an issue with saving large `.bib` files [#7265](https://github.com/JabRef/jabref/issues/7265)
- We fixed an issue with very large page numbers [#7590](https://github.com/JabRef/jabref/issues/7590)
- We fixed an issue where the file extension is missing on saving the library file on linux [#7451](https://github.com/JabRef/jabref/issues/7451)
- We fixed an issue with opacity of disabled icon-buttons [#7195](https://github.com/JabRef/jabref/issues/7195)
- We fixed an issue where journal abbreviations in UTF-8 were not recognized [#5850](https://github.com/JabRef/jabref/issues/5850)
- We fixed an issue where the article title with curly brackets fails to download the arXiv link (pdf file). [#7633](https://github.com/JabRef/jabref/issues/7633)
- We fixed an issue with toggle of special fields does not work for sorted entries [#7016](https://github.com/JabRef/jabref/issues/7016)
- We fixed an issue with the default path of external application. [#7641](https://github.com/JabRef/jabref/issues/7641)
- We fixed an issue where urls must be embedded in a style tag when importing EndNote style Xml files. Now it can parse url with or without a style tag. [#6199](https://github.com/JabRef/jabref/issues/6199)
- We fixed an issue where the article title with colon fails to download the arXiv link (pdf file). [#7660](https://github.com/JabRef/jabref/issues/7660)
- We fixed an issue where the keybinding for delete entry did not work on the main table [7580](https://github.com/JabRef/jabref/pull/7580)
- We fixed an issue where the RFC fetcher is not compatible with the draft [7305](https://github.com/JabRef/jabref/issues/7305)
- We fixed an issue where duplicate files (both file names and contents are the same) is downloaded and add to linked files [#6197](https://github.com/JabRef/jabref/issues/6197)
- We fixed an issue where changing the appearance of the preview tab did not trigger a restart warning. [#5464](https://github.com/JabRef/jabref/issues/5464)
- We fixed an issue where editing "Custom preview style" triggers exception. [#7526](https://github.com/JabRef/jabref/issues/7526)
- We fixed the [SAO/NASA Astrophysics Data System](https://docs.jabref.org/collect/import-using-online-bibliographic-database#sao-nasa-astrophysics-data-system) fetcher. [#7867](https://github.com/JabRef/jabref/pull/7867)
- We fixed an issue where a title with multiple applied formattings in EndNote was not imported correctly [forum#2734](https://discourse.jabref.org/t/importing-endnote-label-field-to-jabref-from-xml-file/2734)
- We fixed an issue where a `report` in EndNote was imported as `article` [forum#2734](https://discourse.jabref.org/t/importing-endnote-label-field-to-jabref-from-xml-file/2734)
- We fixed an issue where the field `publisher` in EndNote was not imported in JabRef [forum#2734](https://discourse.jabref.org/t/importing-endnote-label-field-to-jabref-from-xml-file/2734)

### Removed

- We removed add group button beside the filter group tab. [#4682](https://github.com/JabRef/jabref/issues/4682)

## [5.2] – 2020-12-24

### Added

- We added a validation to check if the current database location is shared, preventing an exception when Pulling Changes From Shared Database. [#6959](https://github.com/JabRef/jabref/issues/6959)
- We added a query parser and mapping layer to enable conversion of queries formulated in simplified lucene syntax by the user into api queries. [#6799](https://github.com/JabRef/jabref/pull/6799)
- We added some basic functionality to customise the look of JabRef by importing a css theme file. [#5790](https://github.com/JabRef/jabref/issues/5790)
- We added connection check function in network preference setting [#6560](https://github.com/JabRef/jabref/issues/6560)
- We added support for exporting to YAML. [#6974](https://github.com/JabRef/jabref/issues/6974)
- We added a DOI format and organization check to detect [American Physical Society](https://journals.aps.org/) journals to copy the article ID to the page field for cases where the page numbers are missing. [#7019](https://github.com/JabRef/jabref/issues/7019)
- We added an error message in the New Entry dialog that is shown in case the fetcher did not find anything . [#7000](https://github.com/JabRef/jabref/issues/7000)
- We added a new formatter to output shorthand month format. [#6579](https://github.com/JabRef/jabref/issues/6579)
- We added support for the new Microsoft Edge browser in all platforms. [#7056](https://github.com/JabRef/jabref/pull/7056)
- We reintroduced emacs/bash-like keybindings. [#6017](https://github.com/JabRef/jabref/issues/6017)
- We added a feature to provide automated cross library search using a cross library query language. This provides support for the search step of systematic literature reviews (SLRs). [koppor#369](https://github.com/koppor/jabref/issues/369)

### Changed

- We changed the default preferences for OpenOffice/LibreOffice integration to automatically sync the bibliography when inserting new citations in a OpenOffic/LibreOffice document. [#6957](https://github.com/JabRef/jabref/issues/6957)
- We restructured the 'File' tab and extracted some parts into the 'Linked files' tab [#6779](https://github.com/JabRef/jabref/pull/6779)
- JabRef now offers journal lists from <https://abbrv.jabref.org>. JabRef the lists which use a dot inside the abbreviations. [#5749](https://github.com/JabRef/jabref/pull/5749)
- We removed two useless preferences in the groups preferences dialog. [#6836](https://github.com/JabRef/jabref/pull/6836)
- Synchronization of SpecialFields to keywords is now disabled by default. [#6621](https://github.com/JabRef/jabref/issues/6621)
- JabRef no longer opens the entry editor with the first entry on startup [#6855](https://github.com/JabRef/jabref/issues/6855)
- We completed the rebranding of `bibtexkey` as `citationkey` which was started in JabRef 5.1.
- JabRef no longer opens the entry editor with the first entry on startup [#6855](https://github.com/JabRef/jabref/issues/6855)
- Fetch by ID: (long) "SAO/NASA Astrophysics Data System" replaced by (short) "SAO/NASA ADS" [#6876](https://github.com/JabRef/jabref/pull/6876)
- We changed the title of the window "Manage field names and content" to have the same title as the corresponding menu item [#6895](https://github.com/JabRef/jabref/pull/6895)
- We renamed the menus "View -> Previous citation style" and "View -> Next citation style" into "View -> Previous preview style" and "View -> Next preview style" and renamed the "Preview" style to "Customized preview style". [#6899](https://github.com/JabRef/jabref/pull/6899)
- We changed the default preference option "Search and store files relative to library file location" to on, as this seems to be a more intuitive behaviour. [#6863](https://github.com/JabRef/jabref/issues/6863)
- We changed the title of the window "Manage field names and content": to have the same title as the corresponding menu item [#6895](https://github.com/JabRef/jabref/pull/6895)
- We improved the detection of "short" DOIs [6880](https://github.com/JabRef/jabref/issues/6880)
- We improved the duplicate detection when identifiers like DOI or arxiv are semantiaclly the same, but just syntactically differ (e.g. with or without http(s):// prefix). [#6707](https://github.com/JabRef/jabref/issues/6707)
- We improved JabRef start up time [6057](https://github.com/JabRef/jabref/issues/6057)
- We changed in the group interface "Generate groups from keywords in a BibTeX field" by "Generate groups from keywords in the following field". [#6983](https://github.com/JabRef/jabref/issues/6983)
- We changed the name of a group type from "Searching for keywords" to "Searching for a keyword". [6995](https://github.com/JabRef/jabref/pull/6995)
- We changed the way JabRef displays the title of a tab and of the window. [4161](https://github.com/JabRef/jabref/issues/4161)
- We changed connect timeouts for server requests to 30 seconds in general and 5 seconds for GROBID server (special) and improved user notifications on connection issues. [7026](https://github.com/JabRef/jabref/pull/7026)
- We changed the order of the library tab context menu items. [#7171](https://github.com/JabRef/jabref/issues/7171)
- We changed the way linked files are opened on Linux to use the native openFile method, compatible with confined packages. [7037](https://github.com/JabRef/jabref/pull/7037)
- We refined the entry preview to show the full names of authors and editors, to list the editor only if no author is present, have the year earlier. [#7083](https://github.com/JabRef/jabref/issues/7083)

### Fixed

- We fixed an issue changing the icon link_variation_off that is not meaningful. [#6834](https://github.com/JabRef/jabref/issues/6834)
- We fixed an issue where the `.sav` file was not deleted upon exiting JabRef. [#6109](https://github.com/JabRef/jabref/issues/6109)
- We fixed a linked identifier icon inconsistency. [#6705](https://github.com/JabRef/jabref/issues/6705)
- We fixed the wrong behavior that font size changes are not reflected in dialogs. [#6039](https://github.com/JabRef/jabref/issues/6039)
- We fixed the failure to Copy citation key and link. [#5835](https://github.com/JabRef/jabref/issues/5835)
- We fixed an issue where the sort order of the entry table was reset after a restart of JabRef. [#6898](https://github.com/JabRef/jabref/pull/6898)
- We fixed an issue where no longer a warning was displayed when inserting references into LibreOffice with an invalid "ReferenceParagraphFormat". [#6907](https://github.com/JabRef/jabref/pull/60907).
- We fixed an issue where a selected field was not removed after the first click in the custom entry types dialog. [#6934](https://github.com/JabRef/jabref/issues/6934)
- We fixed an issue where a remove icon was shown for standard entry types in the custom entry types dialog. [#6906](https://github.com/JabRef/jabref/issues/6906)
- We fixed an issue where it was impossible to connect to OpenOffice/LibreOffice on Mac OSX. [#6970](https://github.com/JabRef/jabref/pull/6970)
- We fixed an issue with the python script used by browser plugins that failed to locate JabRef if not installed in its default location. [#6963](https://github.com/JabRef/jabref/pull/6963/files)
- We fixed an issue where spaces and newlines in an isbn would generate an exception. [#6456](https://github.com/JabRef/jabref/issues/6456)
- We fixed an issue where identity column header had incorrect foreground color in the Dark theme. [#6796](https://github.com/JabRef/jabref/issues/6796)
- We fixed an issue where the RIS exporter added extra blank lines.[#7007](https://github.com/JabRef/jabref/pull/7007/files)
- We fixed an issue where clicking on Collapse All button in the Search for Unlinked Local Files expanded the directory structure erroneously [#6848](https://github.com/JabRef/jabref/issues/6848)
- We fixed an issue, when pulling changes from shared database via shortcut caused creation of a new tech report [6867](https://github.com/JabRef/jabref/issues/6867)
- We fixed an issue where the JabRef GUI does not highlight the "All entries" group on start-up [#6691](https://github.com/JabRef/jabref/issues/6691)
- We fixed an issue where a custom dark theme was not applied to the entry preview tab [7068](https://github.com/JabRef/jabref/issues/7068)
- We fixed an issue where modifications to the Custom preview layout in the preferences were not saved [#6447](https://github.com/JabRef/jabref/issues/6447)
- We fixed an issue where errors from imports were not shown to the user [#7084](https://github.com/JabRef/jabref/pull/7084)
- We fixed an issue where the EndNote XML Import would fail on empty keywords tags [forum#2387](https://discourse.jabref.org/t/importing-in-unknown-format-fails-to-import-xml-library-from-bookends-export/2387)
- We fixed an issue where the color of groups of type "free search expression" not persisting after restarting the application [#6999](https://github.com/JabRef/jabref/issues/6999)
- We fixed an issue where modifications in the source tab where not saved without switching to another field before saving the library [#6622](https://github.com/JabRef/jabref/issues/6622)
- We fixed an issue where the "Document Viewer" did not show the first page of the opened pdf document and did not show the correct total number of pages [#7108](https://github.com/JabRef/jabref/issues/7108)
- We fixed an issue where the context menu was not updated after a file link was changed. [#5777](https://github.com/JabRef/jabref/issues/5777)
- We fixed an issue where the password for a shared SQL database was not remembered [#6869](https://github.com/JabRef/jabref/issues/6869)
- We fixed an issue where newly added entires were not synced to a shared SQL database [#7176](https://github.com/JabRef/jabref/issues/7176)
- We fixed an issue where the PDF-Content importer threw an exception when no DOI number is present at the first page of the PDF document [#7203](https://github.com/JabRef/jabref/issues/7203)
- We fixed an issue where groups created from aux files did not update on file changes [#6394](https://github.com/JabRef/jabref/issues/6394)
- We fixed an issue where authors that only have last names were incorrectly identified as institutes when generating citation keys [#7199](https://github.com/JabRef/jabref/issues/7199)
- We fixed an issue where institutes were incorrectly identified as universities when generating citation keys [#6942](https://github.com/JabRef/jabref/issues/6942)

### Removed

- We removed the Google Scholar fetcher and the ACM fetcher do not work due to traffic limitations [#6369](https://github.com/JabRef/jabref/issues/6369)
- We removed the menu entry "Manage external file types" because it's already in 'Preferences' dialog [#6991](https://github.com/JabRef/jabref/issues/6991)
- We removed the integrity check "Abbreviation detected" for the field journal/journaltitle in the entry editor [#3925](https://github.com/JabRef/jabref/issues/3925)

## [5.1] – 2020-08-30

### Added

- We added a new fetcher to enable users to search mEDRA DOIs [#6602](https://github.com/JabRef/jabref/issues/6602)
- We added a new fetcher to enable users to search "[Collection of Computer Science Bibliographies](https://en.wikipedia.org/wiki/Collection_of_Computer_Science_Bibliographies)". [#6638](https://github.com/JabRef/jabref/issues/6638)
- We added default values for delimiters in Add Subgroup window [#6624](https://github.com/JabRef/jabref/issues/6624)
- We improved responsiveness of general fields specification dialog window. [#6604](https://github.com/JabRef/jabref/issues/6604)
- We added support for importing ris file and load DOI [#6530](https://github.com/JabRef/jabref/issues/6530)
- We added the Library properties to a context menu on the library tabs [#6485](https://github.com/JabRef/jabref/issues/6485)
- We added a new field in the preferences in 'BibTeX key generator' for unwanted characters that can be user-specified. [#6295](https://github.com/JabRef/jabref/issues/6295)
- We added support for searching ShortScience for an entry through the user's browser. [#6018](https://github.com/JabRef/jabref/pull/6018)
- We updated EditionChecker to permit edition to start with a number. [#6144](https://github.com/JabRef/jabref/issues/6144)
- We added tooltips for most fields in the entry editor containing a short description. [#5847](https://github.com/JabRef/jabref/issues/5847)
- We added support for basic markdown in custom formatted previews [#6194](https://github.com/JabRef/jabref/issues/6194)
- We now show the number of items found and selected to import in the online search dialog. [#6248](https://github.com/JabRef/jabref/pull/6248)
- We created a new install screen for macOS. [#5759](https://github.com/JabRef/jabref/issues/5759)
- We added a new integrity check for duplicate DOIs. [koppor#339](https://github.com/koppor/jabref/issues/339)
- We implemented an option to download fulltext files while importing. [#6381](https://github.com/JabRef/jabref/pull/6381)
- We added a progress-indicator showing the average progress of background tasks to the toolbar. Clicking it reveals a pop-over with a list of running background tasks. [6443](https://github.com/JabRef/jabref/pull/6443)
- We fixed the bug when strike the delete key in the text field. [#6421](https://github.com/JabRef/jabref/issues/6421)
- We added a BibTex key modifier for truncating strings. [#3915](https://github.com/JabRef/jabref/issues/3915)
- We added support for jumping to target entry when typing letter/digit after sorting a column in maintable [#6146](https://github.com/JabRef/jabref/issues/6146)
- We added a new fetcher to enable users to search all available E-Libraries simultaneously. [koppor#369](https://github.com/koppor/jabref/issues/369)
- We added the field "entrytype" to the export sort criteria [#6531](https://github.com/JabRef/jabref/pull/6531)
- We added the possibility to change the display order of the fields in the entry editor. The order can now be configured using drag and drop in the "Customize entry types" dialog [#6152](https://github.com/JabRef/jabref/pull/6152)
- We added native support for biblatex-software [#6574](https://github.com/JabRef/jabref/issues/6574)
- We added a missing restart warning for AutoComplete in the preferences dialog. [#6351](https://github.com/JabRef/jabref/issues/6351)
- We added a note to the citation key pattern preferences dialog as a temporary workaround for a JavaFX bug, about committing changes in a table cell, if the focus is lost. [#5825](https://github.com/JabRef/jabref/issues/5825)
- We added support for customized fallback fields in bracketed patterns. [#7111](https://github.com/JabRef/jabref/issues/7111)

### Changed

- We improved the arXiv fetcher. Now it should find entries even more reliably and does no longer include the version (e.g `v1`) in the `eprint` field. [forum#1941](https://discourse.jabref.org/t/remove-version-in-arxiv-import/1941)
- We moved the group search bar and the button "New group" from bottom to top position to make it more prominent. [#6112](https://github.com/JabRef/jabref/pull/6112)
- When JabRef finds a `.sav` file without changes, there is no dialog asking for acceptance of changes anymore.
- We changed the buttons for import/export/show all/reset of preferences to smaller icon buttons in the preferences dialog. [#6130](https://github.com/JabRef/jabref/pull/6130)
- We moved the functionality "Manage field names & content" from the "Library" menu to the "Edit" menu, because it affects the selected entries and not the whole library
- We merged the functionality "Append contents from a BibTeX library into the currently viewed library" into the "Import into database" functionality. Fixes [#6049](https://github.com/JabRef/jabref/issues/6049).
- We changed the directory where fulltext downloads are stored to the directory set in the import-tab in preferences. [#6381](https://github.com/JabRef/jabref/pull/6381)
- We improved the error message for invalid jstyles. [#6303](https://github.com/JabRef/jabref/issues/6303)
- We changed the section name of 'Advanced' to 'Network' in the preferences and removed some obsolete options.[#6489](https://github.com/JabRef/jabref/pull/6489)
- We improved the context menu of the column "Linked identifiers" of the main table, by truncating their texts, if they are too long. [#6499](https://github.com/JabRef/jabref/issues/6499)
- We merged the main table tabs in the preferences dialog. [#6518](https://github.com/JabRef/jabref/pull/6518)
- We changed the command line option 'generateBibtexKeys' to the more generic term 'generateCitationKeys' while the short option remains 'g'.[#6545](https://github.com/JabRef/jabref/pull/6545)
- We improved the "Possible duplicate entries" window to remember its size and position throughout a session. [#6582](https://github.com/JabRef/jabref/issues/6582)
- We divided the toolbar into small parts, so if the application window is to small, only a part of the toolbar is moved into the chevron popup. [#6682](https://github.com/JabRef/jabref/pull/6682)
- We changed the layout for of the buttons in the Open Office side panel to ensure that the button text is always visible, specially when resizing. [#6639](https://github.com/JabRef/jabref/issues/6639)
- We merged the two new library commands in the file menu to one which always creates a new library in the default library mode. [#6359](https://github.com/JabRef/jabref/pull/6539#issuecomment-641056536)

### Fixed

- We fixed an issue where entry preview tab has no name in drop down list. [#6591](https://github.com/JabRef/jabref/issues/6591)
- We fixed to only search file links in the BIB file location directory when preferences has corresponding checkbox checked. [#5891](https://github.com/JabRef/jabref/issues/5891)
- We fixed wrong button order (Apply and Cancel) in ManageProtectedTermsDialog.
- We fixed an issue with incompatible characters at BibTeX key [#6257](https://github.com/JabRef/jabref/issues/6257)
- We fixed an issue where dash (`-`) was reported as illegal BibTeX key [#6295](https://github.com/JabRef/jabref/issues/6295)
- We greatly improved the performance of the overall application and many operations. [#5071](https://github.com/JabRef/jabref/issues/5071)
- We fixed an issue where sort by priority was broken. [#6222](https://github.com/JabRef/jabref/issues/6222)
- We fixed an issue where opening a library from the recent libraries menu was not possible. [#5939](https://github.com/JabRef/jabref/issues/5939)
- We fixed an issue with inconsistent capitalization of file extensions when downloading files. [#6115](https://github.com/JabRef/jabref/issues/6115)
- We fixed the display of language and encoding in the preferences dialog. [#6130](https://github.com/JabRef/jabref/pull/6130)
- Now the link and/or the link description in the column "linked files" of the main table gets truncated or wrapped, if too long, otherwise display issues arise. [#6178](https://github.com/JabRef/jabref/issues/6178)
- We fixed the issue that groups panel does not keep size when resizing window. [#6180](https://github.com/JabRef/jabref/issues/6180)
- We fixed an error that sometimes occurred when using the context menu. [#6085](https://github.com/JabRef/jabref/issues/6085)
- We fixed an issue where search full-text documents downloaded files with same name, overwriting existing files. [#6174](https://github.com/JabRef/jabref/pull/6174)
- We fixed an issue when importing into current library an erroneous message "import cancelled" is displayed even though import is successful. [#6266](https://github.com/JabRef/jabref/issues/6266)
- We fixed an issue where custom jstyles for Open/LibreOffice where not saved correctly. [#6170](https://github.com/JabRef/jabref/issues/6170)
- We fixed an issue where the INSPIRE fetcher was no longer working [#6229](https://github.com/JabRef/jabref/issues/6229)
- We fixed an issue where custom exports with an uppercase file extension could not be selected for "Copy...-> Export to Clipboard" [#6285](https://github.com/JabRef/jabref/issues/6285)
- We fixed the display of icon both in the main table and linked file editor. [#6169](https://github.com/JabRef/jabref/issues/6169)
- We fixed an issue where the windows installer did not create an entry in the start menu [bug report in the forum](https://discourse.jabref.org/t/error-while-fetching-from-doi/2018/3)
- We fixed an issue where only the field `abstract` and `comment` were declared as multiline fields. Other fields can now be configured in the preferences using "Do not wrap the following fields when saving" [4373](https://github.com/JabRef/jabref/issues/4373)
- We fixed an issue where JabRef switched to discrete graphics under macOS [#5935](https://github.com/JabRef/jabref/issues/5935)
- We fixed an issue where the Preferences entry preview will be unexpected modified leads to Value too long exception [#6198](https://github.com/JabRef/jabref/issues/6198)
- We fixed an issue where custom jstyles for Open/LibreOffice would only be valid if a layout line for the entry type `default` was at the end of the layout section [#6303](https://github.com/JabRef/jabref/issues/6303)
- We fixed an issue where a new entry is not shown in the library if a search is active [#6297](https://github.com/JabRef/jabref/issues/6297)
- We fixed an issue where long directory names created from patterns could create an exception. [#3915](https://github.com/JabRef/jabref/issues/3915)
- We fixed an issue where sort on numeric cases was broken. [#6349](https://github.com/JabRef/jabref/issues/6349)
- We fixed an issue where year and month fields were not cleared when converting to biblatex [#6224](https://github.com/JabRef/jabref/issues/6224)
- We fixed an issue where an "Not on FX thread" exception occurred when saving on linux [#6453](https://github.com/JabRef/jabref/issues/6453)
- We fixed an issue where the library sort order was lost. [#6091](https://github.com/JabRef/jabref/issues/6091)
- We fixed an issue where brackets in regular expressions were not working. [6469](https://github.com/JabRef/jabref/pull/6469)
- We fixed an issue where multiple background task popups stacked over each other.. [#6472](https://github.com/JabRef/jabref/issues/6472)
- We fixed an issue where LaTeX citations for specific commands (`\autocite`s) of biblatex-mla were not recognized. [#6476](https://github.com/JabRef/jabref/issues/6476)
- We fixed an issue where drag and drop was not working on empty database. [#6487](https://github.com/JabRef/jabref/issues/6487)
- We fixed an issue where the name fields were not updated after the preferences changed. [#6515](https://github.com/JabRef/jabref/issues/6515)
- We fixed an issue where "null" appeared in generated BibTeX keys. [#6459](https://github.com/JabRef/jabref/issues/6459)
- We fixed an issue where the authors' names were incorrectly displayed in the authors' column when they were bracketed. [#6465](https://github.com/JabRef/jabref/issues/6465) [#6459](https://github.com/JabRef/jabref/issues/6459)
- We fixed an issue where importing certain unlinked files would result in an exception [#5815](https://github.com/JabRef/jabref/issues/5815)
- We fixed an issue where downloaded files would be moved to a directory named after the citationkey when no file directory pattern is specified [#6589](https://github.com/JabRef/jabref/issues/6589)
- We fixed an issue with the creation of a group of cited entries which incorrectly showed the message that the library had been modified externally whenever saving the library. [#6420](https://github.com/JabRef/jabref/issues/6420)
- We fixed an issue with the creation of a group of cited entries. Now the file path to an aux file gets validated. [#6585](https://github.com/JabRef/jabref/issues/6585)
- We fixed an issue on Linux systems where the application would crash upon inotify failure. Now, the user is prompted with a warning, and given the choice to continue the session. [#6073](https://github.com/JabRef/jabref/issues/6073)
- We moved the search modifier buttons into the search bar, as they were not accessible, if autocompletion was disabled. [#6625](https://github.com/JabRef/jabref/issues/6625)
- We fixed an issue about duplicated group color indicators [#6175](https://github.com/JabRef/jabref/issues/6175)
- We fixed an issue where entries with the entry type Misc from an imported aux file would not be saved correctly to the bib file on disk [#6405](https://github.com/JabRef/jabref/issues/6405)
- We fixed an issue where percent sign ('%') was not formatted properly by the HTML formatter [#6753](https://github.com/JabRef/jabref/issues/6753)
- We fixed an issue with the [SAO/NASA Astrophysics Data System](https://docs.jabref.org/collect/add-entry-using-an-id#sao-nasa-a-ds) fetcher where `\textbackslash` appeared at the end of the abstract.
- We fixed an issue with the Science Direct fetcher where PDFs could not be downloaded. Fixes [#5860](https://github.com/JabRef/jabref/issues/5860)
- We fixed an issue with the Library of Congress importer.
- We fixed the [link to the external libraries listing](https://github.com/JabRef/jabref/blob/master/external-libraries.md) in the about dialog
- We fixed an issue regarding pasting on Linux. [#6293](https://github.com/JabRef/jabref/issues/6293)

### Removed

- We removed the option of the "enforce legal key". [#6295](https://github.com/JabRef/jabref/issues/6295)
- We removed the obsolete `External programs / Open PDF` section in the preferences, as the default application to open PDFs is now set in the `Manage external file types` dialog. [#6130](https://github.com/JabRef/jabref/pull/6130)
- We removed the option to configure whether a `.bib.bak` file should be generated upon save. It is now always enabled. Documentation at <https://docs.jabref.org/advanced/autosave>. [#6092](https://github.com/JabRef/jabref/issues/6092)
- We removed the built-in list of IEEE journal abbreviations using BibTeX strings. If you still want to use them, you have to download them separately from <https://abbrv.jabref.org>.

## [5.0] – 2020-03-06

### Changed

- Added browser integration to the snap package for firefox/chromium browsers. [#6062](https://github.com/JabRef/jabref/pull/6062)
- We reintroduced the possibility to extract references from plain text (using [GROBID](https://grobid.readthedocs.io/en/latest/)). [#5614](https://github.com/JabRef/jabref/pull/5614)
- We changed the open office panel to show buttons in rows of three instead of going straight down to save space as the button expanded out to take up unnecessary horizontal space. [#5479](https://github.com/JabRef/jabref/issues/5479)
- We cleaned up the group add/edit dialog. [#5826](https://github.com/JabRef/jabref/pull/5826)
- We reintroduced the index column. [#5844](https://github.com/JabRef/jabref/pull/5844)
- Filenames of external files can no longer contain curly braces. [#5926](https://github.com/JabRef/jabref/pull/5926)
- We made the filters more easily accessible in the integrity check dialog. [#5955](https://github.com/JabRef/jabref/pull/5955)
- We reimplemented and improved the dialog "Customize entry types". [#4719](https://github.com/JabRef/jabref/issues/4719)
- We added an [American Physical Society](https://journals.aps.org/) fetcher. [#818](https://github.com/JabRef/jabref/issues/818)
- We added possibility to enable/disable items quantity in groups. [#6042](https://github.com/JabRef/jabref/issues/6042)

### Fixed

- We fixed an issue where the command line console was always opened in the background. [#5474](https://github.com/JabRef/jabref/issues/5474)
- We fixed and issue where pdf files will not open under some KDE linux distributions when using okular. [#5253](https://github.com/JabRef/jabref/issues/5253)
- We fixed an issue where the Medline fetcher was only working when JabRef was running from source. [#5645](https://github.com/JabRef/jabref/issues/5645)
- We fixed some visual issues in the dark theme. [#5764](https://github.com/JabRef/jabref/pull/5764) [#5753](https://github.com/JabRef/jabref/issues/5753)
- We fixed an issue where non-default previews didn't handle unicode characters. [#5779](https://github.com/JabRef/jabref/issues/5779)
- We improved the performance, especially changing field values in the entry should feel smoother now. [#5843](https://github.com/JabRef/jabref/issues/5843)
- We fixed an issue where the ampersand character wasn't rendering correctly on previews. [#3840](https://github.com/JabRef/jabref/issues/3840)
- We fixed an issue where an erroneous "The library has been modified by another program" message was shown when saving. [#4877](https://github.com/JabRef/jabref/issues/4877)
- We fixed an issue where the file extension was missing after downloading a file (we now fall-back to pdf). [#5816](https://github.com/JabRef/jabref/issues/5816)
- We fixed an issue where cleaning up entries broke web URLs, if "Make paths of linked files relative (if possible)" was enabled, which resulted in various other issues subsequently. [#5861](https://github.com/JabRef/jabref/issues/5861)
- We fixed an issue where the tab "Required fields" of the entry editor did not show all required fields, if at least two of the defined required fields are linked with a logical or. [#5859](https://github.com/JabRef/jabref/issues/5859)
- We fixed several issues concerning managing external file types: Now everything is usable and fully functional. Previously, there were problems with the radio buttons, with saving the settings and with loading an input field value. Furthermore, different behavior for Windows and other operating systems was given, which was unified as well. [#5846](https://github.com/JabRef/jabref/issues/5846)
- We fixed an issue where entries containing Unicode charaters were not parsed correctly [#5899](https://github.com/JabRef/jabref/issues/5899)
- We fixed an issue where an entry containing an external filename with curly braces could not be saved. Curly braces are now longer allowed in filenames. [#5899](https://github.com/JabRef/jabref/issues/5899)
- We fixed an issue where changing the type of an entry did not update the main table [#5906](https://github.com/JabRef/jabref/issues/5906)
- We fixed an issue in the optics of the library properties, that cropped the dialog on scaled displays. [#5969](https://github.com/JabRef/jabref/issues/5969)
- We fixed an issue where changing the type of an entry did not update the main table. [#5906](https://github.com/JabRef/jabref/issues/5906)
- We fixed an issue where opening a library from the recent libraries menu was not possible. [#5939](https://github.com/JabRef/jabref/issues/5939)
- We fixed an issue where the most bottom group in the list got lost, if it was dragged on itself. [#5983](https://github.com/JabRef/jabref/issues/5983)
- We fixed an issue where changing entry type doesn't always work when biblatex source is shown. [#5905](https://github.com/JabRef/jabref/issues/5905)
- We fixed an issue where the group and the link column were not updated after changing the entry in the main table. [#5985](https://github.com/JabRef/jabref/issues/5985)
- We fixed an issue where reordering the groups was not possible after inserting an article. [#6008](https://github.com/JabRef/jabref/issues/6008)
- We fixed an issue where citation styles except the default "Preview" could not be used. [#5622](https://github.com/JabRef/jabref/issues/5622)
- We fixed an issue where a warning was displayed when the title content is made up of two sentences. [#5832](https://github.com/JabRef/jabref/issues/5832)
- We fixed an issue where an exception was thrown when adding a save action without a selected formatter in the library properties [#6069](https://github.com/JabRef/jabref/issues/6069)
- We fixed an issue where JabRef's icon was missing in the Export to clipboard Dialog. [#6286](https://github.com/JabRef/jabref/issues/6286)
- We fixed an issue when an "Abstract field" was duplicating text, when importing from RIS file (Neurons) [#6065](https://github.com/JabRef/jabref/issues/6065)
- We fixed an issue where adding the addition of a new entry was not completely validated [#6370](https://github.com/JabRef/jabref/issues/6370)
- We fixed an issue where the blue and red text colors in the Merge entries dialog were not quite visible [#6334](https://github.com/JabRef/jabref/issues/6334)
- We fixed an issue where underscore character was removed from the file name in the Recent Libraries list in File menu [#6383](https://github.com/JabRef/jabref/issues/6383)
- We fixed an issue where few keyboard shortcuts regarding new entries were missing [#6403](https://github.com/JabRef/jabref/issues/6403)

### Removed

- Ampersands are no longer escaped by default in the `bib` file. If you want to keep the current behaviour, you can use the new "Escape Ampersands" formatter as a save action. [#5869](https://github.com/JabRef/jabref/issues/5869)
- The "Merge Entries" entry was removed from the Quality Menu. Users should use the right-click menu instead. [#6021](https://github.com/JabRef/jabref/pull/6021)

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
- We removed an internal step in the [ISBN-to-BibTeX fetcher](https://docs.jabref.org/collect/add-entry-using-an-id#isbn): The [ISBN to BibTeX Converter](https://manas.tungare.name/software/isbn-to-bibtex) by [@manastungare](https://github.com/manastungare) is not used anymore, because it is offline: "people using this tool have not been generating enough sales for Amazon."
- We removed the option to control the default drag and drop behaviour. You can use the modifier keys (like CtrL or Alt) instead.

## [5.0-alpha] – 2019-08-25

### Changed

- We added eventitle, eventdate and venue fields to `@unpublished` entry type.
- We added `@software` and `@dataSet` entry type to biblatex.
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
- We changed the minimum required version of Java to 1.8.0_171, as this is the latest release for which the automatic Java update works. [#4093](https://github.com/JabRef/jabref/issues/4093)
- The special fields like `Printed` and `Read status` now show gray icons when the row is hovered.
- We added a button in the tab header which allows you to close the database with one click. [#494](https://github.com/JabRef/jabref/issues/494)
- Sorting in the main table now takes information from cross-referenced entries into account. [#2808](https://github.com/JabRef/jabref/issues/2808)
- If a group has a color specified, then entries matched by this group have a small colored bar in front of them in the main table.
- Change default icon for groups to a circle because a colored version of the old icon was hard to distinguish from its black counterpart.
- In the main table, the context menu appears now when you press the "context menu" button on the keyboard. [feature request in the forum](https://discourse.jabref.org/t/how-to-enable-keyboard-context-key-windows)
- We added icons to the group side panel to quickly switch between `union` and `intersection` group view mode. [#3269](https://github.com/JabRef/jabref/issues/3269).
- We use `https` for [fetching from most online bibliographic database](https://docs.jabref.org/collect/import-using-online-bibliographic-database).
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
- We added a new keyboard shortcut so that the entry editor could be closed by <kbd>Ctrl</kbd> + <kbd>E</kbd>. [#4222](https://github.com/JabRef/jabref/issues/4222)
- We added an option in the preference dialog box, that allows user to pick the dark or light theme option. [#4130](https://github.com/JabRef/jabref/issues/4130)
- We updated the Related Articles tab to accept JSON from the new version of the Mr. DLib service
- We added an option in the preference dialog box that allows user to choose behavior after dragging and dropping files in Entry Editor. [#4356](https://github.com/JabRef/jabref/issues/4356)
- We added the ability to have an export preference where previously "File"-->"Export"/"Export selected entries" would not save the user's preference[#4495](https://github.com/JabRef/jabref/issues/4495)
- We optimized the code responsible for connecting to an external database, which should lead to huge improvements in performance.
- For automatically created groups, added ability to filter groups by entry type. [#4539](https://github.com/JabRef/jabref/issues/4539)
- We added the ability to add field names from the Preferences Dialog [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added the ability to change the column widths directly in the main table. [#4546](https://github.com/JabRef/jabref/issues/4546)
- We added a description of how recommendations were chosen and better error handling to Related Articles tab
- We added the ability to execute default action in dialog by using with <kbd>Ctrl</kbd> + <kbd>Enter</kbd> combination [#4496](https://github.com/JabRef/jabref/issues/4496)
- We grouped and reordered the Main Menu (File, Edit, Library, Quality, Tools, and View tabs & icons). [#4666](https://github.com/JabRef/jabref/issues/4666) [#4667](https://github.com/JabRef/jabref/issues/4667) [#4668](https://github.com/JabRef/jabref/issues/4668) [#4669](https://github.com/JabRef/jabref/issues/4669) [#4670](https://github.com/JabRef/jabref/issues/4670) [#4671](https://github.com/JabRef/jabref/issues/4671) [#4672](https://github.com/JabRef/jabref/issues/4672) [#4673](https://github.com/JabRef/jabref/issues/4673)
- We added additional modifiers (capitalize, titlecase and sentencecase) to the Bibtex key generator. [#1506](https://github.com/JabRef/jabref/issues/1506)
- We have migrated from the mysql jdbc connector to the mariadb one for better authentication scheme support. [#4745](https://github.com/JabRef/jabref/issues/4745)
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
- Focus on Name Area instead of 'OK' button whenever user presses 'Add subgroup'. [#6307](https://github.com/JabRef/jabref/issues/6307)
- We changed the behavior of merging that the entry which has "smaller" bibkey will be selected. [#7395](https://github.com/JabRef/jabref/issues/7395)

### Fixed

- We fixed an issue where JabRef died silently for the user without enough inotify instances [#4874](https://github.com/JabRef/jabref/issues/4874)
- We fixed an issue where corresponding groups are sometimes not highlighted when clicking on entries [#3112](https://github.com/JabRef/jabref/issues/3112)
- We fixed an issue where custom exports could not be selected in the 'Export (selected) entries' dialog [#4013](https://github.com/JabRef/jabref/issues/4013)
- Italic text is now rendered correctly. [#3356](https://github.com/JabRef/jabref/issues/3356)
- The entry editor no longer gets corrupted after using the source tab. [#3532](https://github.com/JabRef/jabref/issues/3532) [#3608](https://github.com/JabRef/jabref/issues/3608) [#3616](https://github.com/JabRef/jabref/issues/3616)
- We fixed multiple issues where entries did not show up after import if a search was active. [#1513](https://github.com/JabRef/jabref/issues/1513) [#3219](https://github.com/JabRef/jabref/issues/3219))
- We fixed an issue where the group tree was not updated correctly after an entry was changed. [#3618](https://github.com/JabRef/jabref/issues/3618)
- We fixed an issue where a right-click in the main table selected a wrong entry. [#3267](https://github.com/JabRef/jabref/issues/3267)
- We fixed an issue where in rare cases entries where overlayed in the main table. [#3281](https://github.com/JabRef/jabref/issues/3281)
- We fixed an issue where selecting a group messed up the focus of the main table and the entry editor. [#3367](https://github.com/JabRef/jabref/issues/3367)
- We fixed an issue where composite author names were sorted incorrectly. [#2828](https://github.com/JabRef/jabref/issues/2828)
- We fixed an issue where commands followed by `-` didn't work. [#3805](https://github.com/JabRef/jabref/issues/3805)
- We fixed an issue where a non-existing aux file in a group made it impossible to open the library. [#4735](https://github.com/JabRef/jabref/issues/4735)
- We fixed an issue where some journal names were wrongly marked as abbreviated. [#4115](https://github.com/JabRef/jabref/issues/4115)
- We fixed an issue where the custom file column were sorted incorrectly. [#3119](https://github.com/JabRef/jabref/issues/3119)
- We improved the parsing of author names whose infix is abbreviated without a dot. [#4864](https://github.com/JabRef/jabref/issues/4864)
- We fixed an issues where the entry losses focus when a field is edited and at the same time used for sorting. [#3373](https://github.com/JabRef/jabref/issues/3373)
- We fixed an issue where the menu on Mac OS was not displayed in the usual Mac-specific way. [#3146](https://github.com/JabRef/jabref/issues/3146)
- We improved the integrity check for page numbers. [#4113](https://github.com/JabRef/jabref/issues/4113) and [feature request in the forum](https://discourse.jabref.org/t/pages-field-allow-use-of-en-dash/1199)
- We fixed an issue where the order of fields in customized entry types was not saved correctly. [#4033](https://github.com/JabRef/jabref/issues/4033)
- We fixed an issue where renaming a group did not change the group name in the interface. [#3189](https://github.com/JabRef/jabref/issues/3189)
- We fixed an issue where the groups tree of the last database was still shown even after the database was already closed.
- We fixed an issue where the "Open file dialog" may disappear behind other windows. [#3410](https://github.com/JabRef/jabref/issues/3410)
- We fixed an issue where the number of entries matched was not updated correctly upon adding or removing an entry. [#3537](https://github.com/JabRef/jabref/issues/3537)
- We fixed an issue where the default icon of a group was not colored correctly.
- We fixed an issue where the first field in entry editor was not focused when adding a new entry. [#4024](https://github.com/JabRef/jabref/issues/4024)
- We reworked the "Edit file" dialog to make it resizeable and improved the workflow for adding and editing files [#2970](https://github.com/JabRef/jabref/issues/2970)
- We fixed an issue where custom name formatters were no longer found correctly. [#3531](https://github.com/JabRef/jabref/issues/3531)
- We fixed an issue where the month was not shown in the preview. [#3239](https://github.com/JabRef/jabref/issues/3239)
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
- We fixed an issue where the command line help text had several errors, and arguments and descriptions have been rewritten to simplify and detail them better. [#2016](https://github.com/JabRef/jabref/issues/2016)
- We fixed an issue where the same menu for changing entry type had two different sizes and weights. [#4977](https://github.com/JabRef/jabref/issues/4977)
- We fixed an issue where the "Attach file" dialog, in the right-click menu for an entry, started on the working directory instead of the user's main directory. [#4995](https://github.com/JabRef/jabref/issues/4995)
- We fixed an issue where the JabRef Icon in the macOS launchpad was not displayed correctly [#5003](https://github.com/JabRef/jabref/issues/5003)
- We fixed an issue where the "Search for unlinked local files" would throw an exception when parsing the content of a PDF-file with missing "series" information [#5128](https://github.com/JabRef/jabref/issues/5128)
- We fixed an issue where the XMP Importer would incorrectly return an empty default entry when importing pdfs [#6577](https://github.com/JabRef/jabref/issues/6577)
- We fixed an issue where opening the menu 'Library properties' marked the library as modified [#6451](https://github.com/JabRef/jabref/issues/6451)
- We fixed an issue when importing resulted in an exception [#7343](https://github.com/JabRef/jabref/issues/7343)
- We fixed an issue where the field in the Field formatter dropdown selection were sorted in random order. [#7710](https://github.com/JabRef/jabref/issues/7710)

### Removed

- The feature to "mark entries" was removed and merged with the groups functionality. For migration, a group is created for every value of the `__markedentry` field and the entry is added to this group.
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

The changelog of JabRef 4.x is available at the [v4.3.1 tag](https://github.com/JabRef/jabref/blob/v4.3.1/CHANGELOG.md).
The changelog of JabRef 3.x is available at the [v3.8.2 tag](https://github.com/JabRef/jabref/blob/v3.8.2/CHANGELOG.md).
The changelog of JabRef 2.11 and all previous versions is available as [text file in the v2.11.1 tag](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG).

[Unreleased]: https://github.com/JabRef/jabref/compare/v5.12...HEAD
[5.12]: https://github.com/JabRef/jabref/compare/v5.11...v5.12
[5.11]: https://github.com/JabRef/jabref/compare/v5.10...v5.11
[5.10]: https://github.com/JabRef/jabref/compare/v5.9...v5.10
[5.9]: https://github.com/JabRef/jabref/compare/v5.8...v5.9
[5.8]: https://github.com/JabRef/jabref/compare/v5.7...v5.8
[5.7]: https://github.com/JabRef/jabref/compare/v5.6...v5.7
[5.6]: https://github.com/JabRef/jabref/compare/v5.5...v5.6
[5.5]: https://github.com/JabRef/jabref/compare/v5.4...v5.5
[5.4]: https://github.com/JabRef/jabref/compare/v5.3...v5.4
[5.3]: https://github.com/JabRef/jabref/compare/v5.2...v5.3
[5.2]: https://github.com/JabRef/jabref/compare/v5.1...v5.2
[5.1]: https://github.com/JabRef/jabref/compare/v5.0...v5.1
[5.0]: https://github.com/JabRef/jabref/compare/v5.0-beta...v5.0
[5.0-beta]: https://github.com/JabRef/jabref/compare/v5.0-alpha...v5.0-beta
[5.0-alpha]: https://github.com/JabRef/jabref/compare/v4.3...v5.0-alpha
<!-- markdownlint-disable-file MD012 MD024 MD033 MD053 -->
