# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`.
In case, there is no issue present, the pull request implementing the feature is linked.

Note that this project **does not** adhere to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added

- We added confirmation dialog when user wants to close a library where any empty entires are detected. [#8096](https://github.com/JabRef/jabref/issues/8096)
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

### Fixed

- We fixed an issue where an exception occurred when a linked online file was edited in the entry editor [#8008](https://github.com/JabRef/jabref/issues/8008)
- We fixed an issue when checking for a new version when JabRef is used behind a corporate proxy. [#7884](https://github.com/JabRef/jabref/issues/7884)
- We fixed some icons that were drawn in the wrong color when JabRef used a custom theme. [#7853](https://github.com/JabRef/jabref/issues/7853)
- We fixed an issue where the `Aux file` on `Edit group` doesn't support relative sub-directories path to import. [#7719](https://github.com/JabRef/jabref/issues/7719).
- We fixed an issue where it was impossible to add or modify groups. [#7912](https://github.com/JabRef/jabref/pull/793://github.com/JabRef/jabref/pull/7921)
- We fixed an issue where exported entries from a Citavi bib containing URLs could not be imported [#7892](https://github.com/JabRef/jabref/issues/7882)
- We fixed an issue where the icons in the search bar had the same color, toggled as well as untoggled. [#8014](https://github.com/JabRef/jabref/pull/8014)
- We fixed an issue where typing an invalid UNC path into the "Main file directory" text field caused an error. [#8107](https://github.com/JabRef/jabref/issues/8107)
- We fixed an issue where "Open Folder" didn't select the file on macOS in Finder [#8130](https://github.com/JabRef/jabref/issues/8130)
- We fixed an issue where importing PDFs resulted in an uncaught exception [#8143](https://github.com/JabRef/jabref/issues/8143)
- We fixed "The library has been modified by another program" showing up when line breaks change [#4877](https://github.com/JabRef/jabref/issues/4877)
- The default directory of the "LaTeX Citations" tab is now the directory of the currently opened database (and not the directory chosen at the last open file dialog or the last database save) [koppor#538](https://github.com/koppor/jabref/issues/538)
- We fixed an issue where right-clicking on a tab and selecting close will close the focused tab even if it is not the tab we right-clicked [#8193](https://github.com/JabRef/jabref/pull/8193)
- We fixed an issue where selecting a citation style in the preferences would sometimes produce an exception [#7860](https://github.com/JabRef/jabref/issues/7860)

### Removed

- We removed two orphaned preferences options [#8164](https://github.com/JabRef/jabref/pull/8164)





























































































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
- We added auto-key-generation progress to the background task list. [#7267](https://github.com/JabRef/jabref/issues/72)
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
- We fixed an issue where journal abbreviations containing curly braces were not recognized  [#7773](https://github.com/JabRef/jabref/issues/7773)

### Fixed

- We fixed an isuse where some texts (e.g. descriptionss) in dialogs could not be translated [#7854](https://github.com/JabRef/jabref/issues/7854)
- We fixed an issue where import hangs for ris files with "ER - " [#7737](https://github.com/JabRef/jabref/issues/7737)
- We fixed an issue where getting bibliograhpic data from DOI or another identifer did not respect the library mode (BibTeX/biblatex)[#1018](https://github.com/JabRef/jabref/issues/6267)
- We fixed an issue where importing entries would not respect the library mode (BibTeX/biblatex)[#1018](https://github.com/JabRef/jabref/issues/1018)
- We fixed an issue where an exception occured when importing entries from a web search [#7606](https://github.com/JabRef/jabref/issues/7606)
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
- We fixed an issue where the article title with colon fails to download the arXiv link (pdf file). [#7660](https://github.com/JabRef/issues/7660)
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
- We changed the title of the window  "Manage field names and content":  to have the same title as the corresponding menu item  [#6895](https://github.com/JabRef/jabref/pull/6895)
- We improved the detection of "short" DOIs [6880](https://github.com/JabRef/jabref/issues/6880)
- We improved the duplicate detection when identifiers like DOI or arxiv are semantiaclly the same, but just syntactically differ (e.g. with or without http(s):// prefix). [#6707](https://github.com/JabRef/jabref/issues/6707)
- We improved JabRef start up time [6057](https://github.com/JabRef/jabref/issues/6057)
- We changed in the group interface "Generate groups from keywords in a BibTeX field" by "Generate groups from keywords in the following field". [#6983](https://github.com/JabRef/jabref/issues/6983)
- We changed the name of a group type from "Searching for keywords" to "Searching for a keyword". [6995](https://github.com/JabRef/jabref/pull/6995)
- We changed the way JabRef displays the title of a tab and of the window. [4161](https://github.com/JabRef/jabref/issues/4161)
- We changed connect timeouts for server requests to 30 seconds in general and 5 seconds for GROBID server (special) and improved user notifications on connection issues. [7026](https://github.com/JabRef/jabref/pull/7026)
- We changed the order of the library tab context menu items. [#7171](https://github.com/JabRef/jabref/issues/7171)
- We changed the way linked files are opened on Linux to use the native openFile method, compatible with confined  packages. [7037](https://github.com/JabRef/jabref/pull/7037)
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
- We fixed an issue where identity column header had incorrect foreground color in the  Dark theme. [#6796](https://github.com/JabRef/jabref/issues/6796)
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
- We added a new fetcher to enable users to search "[Collection of Computer Science Bibliographies](https://liinwww.ira.uka.de/bibliography/index.html)". [#6638](https://github.com/JabRef/jabref/issues/6638)
- We added default values for delimiters in Add Subgroup window [#6624](https://github.com/JabRef/jabref/issues/6624)
- We improved responsiveness of general fields specification dialog window. [#6643](https://github.com/JabRef/jabref/issues/6604)
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
- We fixed an issue where an "Not on FX thread" exception occured when saving on linux [#6453](https://github.com/JabRef/jabref/issues/6453)
- We fixed an issue where the library sort order was lost. [#6091](https://github.com/JabRef/jabref/issues/6091)
- We fixed an issue where brackets in regular expressions were not working. [6469](https://github.com/JabRef/jabref/pull/6469)
- We fixed an issue where multiple background task popups stacked over each other.. [#6472](https://github.com/JabRef/jabref/issues/6472)
- We fixed an issue where LaTeX citations for specific commands (\autocites) of biblatex-mla were not recognized. [#6476](https://github.com/JabRef/jabref/issues/6476)
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
- We fixed an issue with the [SAO/NASA Astrophysics Data System](https://docs.jabref.org/collect/import-using-online-bibliographic-database/ads) fetcher where `\textbackslash` appeared at the end of the abstract.
- We fixed an issue with the Science Direct fetcher where PDFs could not be downloaded. Fixes [#5860](https://github.com/JabRef/jabref/issues/5860)
- We fixed an issue with the Library of Congress importer.
- We fixed the [link to the external libraries listing](https://github.com/JabRef/jabref/blob/master/external-libraries.md) in the about dialog
- We fixed an issue regarding pasting on Linux. [#6293](https://github.com/JabRef/jabref/issues/6293)

### Removed

- We removed the option of the "enforce legal key". [#6295](https://github.com/JabRef/jabref/issues/6295)
- We removed the obsolete `External programs / Open PDF` section in the preferences, as the default application to open PDFs is now set in the `Manage external file types` dialog. [#6130](https://github.com/JabRef/jabref/pull/6130)
- We removed the option to configure whether a `.bib.bak` file should be generated upon save. It is now always enabled. Documentation at <https://docs.jabref.org/general/autosave>. [#6092](https://github.com/JabRef/jabref/issues/6092)
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
- We fixed an issue where citation styles except the default "Preview" could not be used. [#56220](https://github.com/JabRef/jabref/issues/5622)
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
- We removed an internal step in the [ISBN-to-BibTeX fetcher](https://docs.jabref.org/import-using-publication-identifiers/isbntobibtex): The [ISBN to BibTeX Converter](https://manas.tungare.name/software/isbn-to-bibtex) by [@manastungare](https://github.com/manastungare) is not used anymore, because it is offline: "people using this tool have not been generating enough sales for Amazon."
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
- We changed the minimum required version of Java to 1.8.0_171, as this is the latest release for which the automatic Java update works.  [#4093](https://github.com/JabRef/jabref/issues/4093)
- The special fields like `Printed` and `Read status` now show gray icons when the row is hovered.
- We added a button in the tab header which allows you to close the database with one click. [#494](https://github.com/JabRef/jabref/issues/494)
- Sorting in the main table now takes information from cross-referenced entries into account. [#2808](https://github.com/JabRef/jabref/issues/2808)
- If a group has a color specified, then entries matched by this group have a small colored bar in front of them in the main table.
- Change default icon for groups to a circle because a colored version of the old icon was hard to distinguish from its black counterpart.
- In the main table, the context menu appears now when you press the "context menu" button on the keyboard. [feature request in the forum](http://discourse.jabref.org/t/how-to-enable-keyboard-context-key-windows)
- We added icons to the group side panel to quickly switch between `union` and `intersection` group view mode. [#3269](https://github.com/JabRef/jabref/issues/3269).
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
- We added a new keyboard shortcut so that the entry editor could be closed by <kbd>Ctrl</kbd> + <kbd>E</kbd>. [#4222](https://github.com/JabRef/jabref/issues/4222)
- We added an option in the preference dialog box, that allows user to pick the dark or light theme option. [#4130](https://github.com/JabRef/jabref/issues/4130)
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
- Focus on Name Area instead of 'OK' button whenever user presses 'Add subgroup'. [#6307](https://github.com/JabRef/jabref/issues/6307)
- We changed the behavior of merging that the entry which has "smaller" bibkey will be selected. [#7395](https://github.com/JabRef/jabref/issues/7395)

### Fixed

- We fixed an issue where JabRef died silently for the user without enough inotify instances [#4874](https://github.com/JabRef/jabref/issues/4847)
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
- We improved the integrity check for page numbers. [#4113](https://github.com/JabRef/jabref/issues/4113) and [feature request in the forum](http://discourse.jabref.org/t/pages-field-allow-use-of-en-dash/1199)
- We fixed an issue where the order of fields in customized entry types was not saved correctly. [#4033](http://github.com/JabRef/jabref/issues/4033)
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
- We fixed an issue where the command line help text had several errors, and arguments and descriptions have been rewritten to simplify and detail them better. [#4932](https://github.com/JabRef/jabref/issues/2016)
- We fixed an issue where the same menu for changing entry type had two different sizes and weights. [#4977](https://github.com/JabRef/jabref/issues/4977)
- We fixed an issue where the "Attach file" dialog, in the right-click menu for an entry, started on the working directory instead of the user's main directory. [#4995](https://github.com/JabRef/jabref/issues/4995)
- We fixed an issue where the JabRef Icon in the macOS launchpad was not displayed correctly [#5003](https://github.com/JabRef/jabref/issues/5003)
- We fixed an issue where the "Search for unlinked local files" would throw an exception when parsing the content of a PDF-file with missing "series" information [#5128](https://github.com/JabRef/jabref/issues/5128)
- We fixed an issue where the XMP Importer would incorrectly return an empty default entry when importing pdfs [#6577](https://github.com/JabRef/jabref/issues/6577)
- We fixed an issue where opening the menu 'Library properties' marked the library as modified [#6451](https://github.com/JabRef/jabref/issues/6451)
- We fixed an issue when importing resulted in an exception [#7343](https://github.com/JabRef/jabref/issues/7343)
- We fixed an issue where the field in the Field formatter dropdown selection were sorted in random order. [#7710](https://github.com/JabRef/jabref/issues/7710)

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

The changelog of JabRef 4.x is available at the [v4.3.1 tag](https://github.com/JabRef/jabref/blob/v4.3.1/CHANGELOG.md).
The changelog of JabRef 3.x is available at the [v3.8.2 tag](https://github.com/JabRef/jabref/blob/v3.8.2/CHANGELOG.md).
The changelog of JabRef 2.11 and all previous versions is available as [text file in the v2.11.1 tag](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG).

[Unreleased]: https://github.com/JabRef/jabref/compare/v5.3...HEAD
[5.3]: https://github.com/JabRef/jabref/compare/v5.2...v5.3
[5.2]: https://github.com/JabRef/jabref/compare/v5.1...v5.2
[5.1]: https://github.com/JabRef/jabref/compare/v5.0...v5.1
[5.0]: https://github.com/JabRef/jabref/compare/v5.0-beta...v5.0
[5.0-beta]: https://github.com/JabRef/jabref/compare/v5.0-alpha...v5.0-beta
[5.0-alpha]: https://github.com/JabRef/jabref/compare/v4.3...v5.0-alpha

<!-- markdownlint-disable-file MD012 MD024 MD033 -->
