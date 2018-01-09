
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
- We added [oaDOI](https://oadoi.org/) as a fulltext provider, so that JabRef is now able to provide fulltexts for more than 90 million open-access articles.
- We changed one default of [Cleanup entries dialog](http://help.jabref.org/en/CleanupEntries): Per default, the PDF are not moved to the default file directory anymore. [#3619](https://github.com/JabRef/jabref/issues/3619)
- We completely reworked and redesigned the main table.



### Fixed
- We fixed the missing dot in the name of an exported file. [#3576](https://github.com/JabRef/jabref/issues/3576)
- Autocompletion in the search bar can now be disabled via the preferences. [#3598](https://github.com/JabRef/jabref/issues/3598)

### Removed
- We removed the [Look and Feels from JGoodies](http://www.jgoodies.com/freeware/libraries/looks/), because the open source version is not compatible with Java 9.

















































## [4.1] - 2017-12-23

### Changed
- We added bracketed expresion support for file search patterns, import file name patterns and file directory patters, in addition to bibtexkey patterns.
- We added support for `[entrytype]` bracketed expression.
- Updated French translation
- We improved the handling of abstracts in the "Astrophysics Data System" fetcher. [#2471](https://github.com/JabRef/jabref/issues/2471)
- We added support for pasting entries in different formats [#3143](https://github.com/JabRef/jabref/issues/3143)
- In the annotation tab, PDF files are now monitored for new or changed annotation. A manual reload is no longer necessary. [#3292](https://github.com/JabRef/jabref/issues/3292)
- We increased the relative size of the "abstract" field in the entry editor. [Feature request in the forum](http://discourse.jabref.org/t/entry-preview-in-version-4/827)
- Crossreferenced entries are now used when a BibTex key is generated for an entry with empty fields. [#2811](https://github.com/JabRef/jabref/issues/2811)
- We now set the `WM_CLASS` of the UI to org-jabref-JabRefMain to allow certain Un*x window managers to properly identify its windows
- We changed the default paths for the OpenOffice/LibreOffice binaries to the default path for LibreOffice
- File annotation tab now removes newlines and hyphens before newlines from content and displays an empty String instead of N/A if no contents are found. [#3280](https://github.com/JabRef/jabref/issues/3280)
- We moved the groups field from the "Other fields" tab to "General" (you may have to reset your editor preferences under Options > Set up general fields)
- We no longer create a new entry editor when selecting a new entry to increase performance. [#3187](https://github.com/JabRef/jabref/pull/3187)
- We added the possibility to copy linked files from entries to a single output folder. [#2539](https://github.com/JabRef/jabref/pull/2593)
- We increased performance and decreased the memory footprint of the entry editor drastically. [#3331](https://github.com/JabRef/jabref/pull/3331)
- Late initialization of the context menus in the entry editor. This improves performance and memory footprint further [#3340](https://github.com/JabRef/jabref/pull/3340)
- Integrity check "Abbreviation Detection" detects abbreviated names for journals and booktitles based on the internal list instead of only looking for `.` signs. Fixes [#3144](https://github.com/JabRef/jabref/issues/3144).
- We added a dialog to show that JabRef is working on checking integrity. [#3358](https://github.com/JabRef/jabref/issues/3358)
- When you click the PDF icon in the file list of the entry editor, then the file is opened. [#3491](https://github.com/JabRef/jabref/issues/3491)
- We added an option to mass append to fields via the Quality -> [set/clear/append/rename fields dialog](http://help.jabref.org/en/SetClearRenameFields). [#2721](https://github.com/JabRef/jabref/issues/2721)
- We added a check on startup to ensure JabRef is run with an adequate Java version. [3310](https://github.com/JabRef/jabref/issues/3310)
- In the preference, all installed java Look and Feels are now listed and selectable
- We added an ID fetcher for [IACR eprints](https://eprint.iacr.org/). [#3473](https://github.com/JabRef/jabref/pull/3473)
- We added a clear option to the right-click menu of the text field in the entry editor. [koppor#198](https://github.com/koppor/jabref/issues/198)
- We improved the performance and memory footprint of the citation preview when [CSL styles](http://citationstyles.org/) are used. [#2533](https://github.com/JabRef/jabref/issues/2533)
- We disabled the auto completion as default, because it still causes issues. [#3522](https://github.com/JabRef/jabref/issues/3522)

### Fixed
 - We fixed the translation of `\textendash` and `\textquotesingle` in the entry preview. [#3307](https://github.com/JabRef/jabref/issues/3307)
 - We fixed an issue where JabRef would not terminated after asking to collect anonymous statistics. [#2955 comment](https://github.com/JabRef/jabref/issues/2955#issuecomment-334591123)
 - We fixed an issue where JabRef would not shut down when started with the `-n` (No GUI) option. [#3247](https://github.com/JabRef/jabref/issues/3247)
 - We improved the way metadata is updated in remote databases. [#3235](https://github.com/JabRef/jabref/issues/3235)
 - We improved font rendering of the Entry Editor for Linux based systems. [#3295](https://github.com/JabRef/jabref/issues/3295)
 - We fixed an issue where JabRef would freeze when trying to replace the original entry after a merge with new information from identifiers like DOI/ISBN etc. [#3294](https://github.com/JabRef/jabref/issues/3294)
 - We no longer allow to add a field multiple times in customized entry types and thereby fix an issue in the entry editor that resulted from having a field multiple times. [#3046](https://github.com/JabRef/jabref/issues/3046)
 - We fixed an issue where JabRef would not show the translated content at some points, although there existed a translation
 - We fixed an issue where editing in the source tab would override content of other entries. [#3352](https://github.com/JabRef/jabref/issues/3352#issue-268580818)
 - We fixed an issue where file links created under Windows could not be opened on Linux/OSX. [#3311](https://github.com/JabRef/jabref/issues/3311)
 - We fixed several issues with the automatic linking of files in the entry editor where files were not found or not correctly saved in the bibtex source. [#3346](https://github.com/JabRef/jabref/issues/3346)
 - We fixed an issue where fetching entries from crossref that had no titles caused an error. [#3376](https://github.com/JabRef/jabref/issues/3376)
 - We fixed an issue where the same Java Look and Feel would be listed more than once in the Preferences. [#3391](https://github.com/JabRef/jabref/issues/3391)
 - We fixed an issue where errors in citation styles triggered an exception when opening the preferences dialog. [#3389](https://github.com/JabRef/jabref/issues/3389)
 - We fixed an issue where entries were displayed twice after insertion into a shared database. [#3164](https://github.com/JabRef/jabref/issues/3164)
 - We improved the auto link algorithm so that files starting with a similar key are no longer found (e.g, `Einstein1902` vs `Einstein1902a`). [#3472](https://github.com/JabRef/jabref/issues/3472)
 - We fixed an issue where special fields (such as `printed`) could not be cleared when syncing special fields via the keywords. [#3432](https://github.com/JabRef/jabref/issues/3432)
 - We fixed an issue where the tooltip of the global search bar showed html tags instead of formatting the text. [#3381](https://github.com/JabRef/jabref/issues/3381)
 - We fixed an issue where timestamps were not updated for changed entries. [#2810](https://github.com/JabRef/jabref/issues/2810)
 - We fixed an issue where trying to fetch data from Medline/PubMed resulted in an error. [#3463](https://github.com/JabRef/jabref/issues/3463)
 - We fixed an issue where double clicking on an entry in the integrity check dialog resulted in an exception. [#3485](https://github.com/JabRef/jabref/issues/3485)
 - We fixed an issue where the entry type could sometimes not be changed when the entry editor was open [#3435](https://github.com/JabRef/jabref/issues/3435)
 - We fixed an issue where dropping a pdf on the entry table and renaming it triggered an exception. [#3490](https://github.com/JabRef/jabref/issues/3490)
 - We fixed an issue where no longer existing files could not be removed from the entry by pressing the <kbd>del</kbd> key. [#3493](https://github.com/JabRef/jabref/issues/3493)
 - We fixed an issue where integrating external changes to a bib file caused instability. [#3498](https://github.com/JabRef/jabref/issues/3498)
 - We fixed an issue where fetched entries from the ACM fetcher could not be imported. [#3500](https://github.com/JabRef/jabref/issues/3500)
 - We fixed an issue where custom data in combobox fields in the entry editor was not saved. [#3538](https://github.com/JabRef/jabref/issues/3538)
 - We fixed an issue where automatically found files were not added with a relative paths when the bib file is in the same directory as the files. [#3476](https://github.com/JabRef/jabref/issues/3476)
 - We improved the key generator to remove certain illegal characters such as colons or apostrophes. [#3359](https://github.com/JabRef/jabref/issues/3359)


## [4.0] - 2017-10-04

### Changed

- We added a textArea to see versionInfo in the About JabRef Dialog. [#2942](https://github.com/JabRef/jabref/issues/2942)
- We turned the validation feature in the entry editor off by default, because of a bug in the library we have been using [#3145](https://github.com/JabRef/jabref/issues/3145)
- Added 'Filter All' and 'Filter None' buttons with corresponding functionality to Quality Check tool.
- We increased the size of the keywords and file text areas in the entry editor
- When the entry that is currently shown in the entry editor is deleted externally, the editor is now closed automatically [#2946](https://github.com/JabRef/jabref/issues/2946)
- We added reordering of file and link entries in the `General`-Tab [3165, comment](https://github.com/JabRef/jabref/issues/3165#issuecomment-326269715)
- We added autcompletion for the `crossref` field on basis of the BibTeX-key. To accept such an autcompleted key as new entry-link, you have to press <kbd>Enter</kbd> two times, otherwise the field data is not stored in the library file.[koppor#257](https://github.com/koppor/jabref/issues/257)
- We added drag and drop support for adding files directly in the `General`-Tab. The dragged files are currently only linked from their existing directory. For more advanced features use the `Add files` dialog. [#koppor#244](https://github.com/koppor/jabref/issues/244)
- We added the file description filed back to the list of files in the `General`-Tab [#2930, comment](https://github.com/JabRef/jabref/issues/2930#issuecomment-328328172)
- Added an error dialog if the file is open in another process and cannot be renamed. [#3229]
- On Windows, the `JabRef.exe` executable can now be used to start JabRef from the command line. By default, no output is shown unless the new "-console" option is specified.

### Fixed

- We re-added the "Normalize to BibTeX name format" context menu item [#3136](https://github.com/JabRef/jabref/issues/3136)
- We fixed a memory leak in the source tab of the entry editor [#3113](https://github.com/JabRef/jabref/issues/3113)
- We fixed a [java bug](https://bugs.openjdk.java.net/browse/JDK-8185792) where linux users could not enter accented characters in the entry editor and the search bar [#3028](https://github.com/JabRef/jabref/issues/3028)
- We fixed a regression introduced in v4.0-beta2: A file can be dropped to the entry preview to attach it to the entry [koppor#245](https://github.com/koppor/jabref/issues/245)
- We fixed an issue in the "Replace String" dialog (<kbd>Ctrl</kbd>+<kbd>R</kbd> where search and replace did not work for the `bibtexkey` field. [#3132](https://github.com/JabRef/jabref/issues/3132)
- We fixed an issue in the entry editor where adding a term to a new protected terms list freezed JabRef completely. [#3157](https://github.com/JabRef/jabref/issues/3157)
- We fixed an issue in the "Manage protected terms" dialog where an 'Open file' dialog instead of a 'Save file' dialog was shown when creating a new list. [#3157](https://github.com/JabRef/jabref/issues/3157)
- We fixed an issue where unparseable dates of the FileAnnotations caused the FileAnnotationsTab to crash.
- We fixed an issue where a new protected terms list was not available immediately after its addition. [#3161](https://github.com/JabRef/jabref/issues/3161)
- We fixed an issue where an online file link could not be removed from an entry [#3165](https://github.com/JabRef/jabref/issues/3165)
- We fixed an issue where an online file link did not open the browser and created an error [#3165](https://github.com/JabRef/jabref/issues/3165)
- We fixed an issue where the arrow keys in the search bar did not work as expected [#3081](https://github.com/JabRef/jabref/issues/3081)
- We fixed wrong hotkey being displayed at "automatically file links" in the entry editor
- We fixed an issue where metadata syncing with local and shared database were unstable. It will also fix syncing groups and sub-groups in database. [#2284](https://github.com/JabRef/jabref/issues/2284)
- We fixed an issue where renaming a linked file would fail silently if a file with the same name existed.  Added support for overriding existing file at user discretion. [#3172] (https://github.com/JabRef/jabref/issues/3172)
- We fixed an issue where the "Remove group and subgroups" operation did not remove group information from entries in the group [#3190](https://github.com/JabRef/jabref/issues/3190)
- We fixed an issue where it was possible to leave the entry editor with an imbalance of braces. [#3167](https://github.com/JabRef/jabref/issues/3167)
- Renaming files now truncates the filename to not exceed the limit of 255 chars [#2622](https://github.com/JabRef/jabref/issues/2622)
- We improved the handling of hyphens in names. [#2775](https://github.com/JabRef/jabref/issues/2775)
- We fixed an issue where an entered file description was not written to the bib-file [#3208](https://github.com/JabRef/jabref/issues/3208)
- We improved the auto completion in the search bar. [koppor#253](https://github.com/koppor/jabref/issues/253)
- We fixed renaming files which are not in the main directory. [#3230](https://github.com/JabRef/jabref/issues/3230)

### Removed
- We removed support for LatexEditor, as it is not under active development. [#3199](https://github.com/JabRef/jabref/issues/3199)


## [4.0-beta3] – 2017-08-16

### Changed
- We made the font size in the entry editor and group panel customizable by "Menu and label font size". [#3034](https://github.com/JabRef/jabref/issues/3034)
- If fetched article is already in database, then the entry merge dialog is shown.
- An error message is now displayed if you try to create a group containing the keyword separator or if there is already a group with the same name. [#3075](https://github.com/JabRef/jabref/issues/3075) and [#1495](https://github.com/JabRef/jabref/issues/1495)
- The FileAnnotationsTab was re-implemented in JavaFx. [#3082](https://github.com/JabRef/jabref/pull/3082)
- Integrity warnings are now directly displayed in the entry editor.
- We added the functionality to have `regex` as modifier. [#457](https://github.com/JabRef/jabref/issues/457)

### Fixed

- We fixed an issue where the fetcher for the Astrophysics Data System (ADS) added some non-bibtex data to the entry returned from the search [#3035](https://github.com/JabRef/jabref/issues/3035)
- We improved the auto completion so that minor changes are not added as suggestions. [#2998](https://github.com/JabRef/jabref/issues/2998)
- We readded the undo mechanism for changes in the entry editor [#2973](https://github.com/JabRef/jabref/issues/2973)
- We fixed an issue where assigning an entry via drag and drop to a group caused JabRef to stop/freeze completely [#3036](https://github.com/JabRef/jabref/issues/3036)
- We fixed the shortcut <kbd>Ctrl</kbd>+<kbd>F</kbd> for the search field.
- We fixed an issue where `title_case` and `capitalize` modifiers did not work with shorttitle.
- We fixed an issue where the preferences could not be imported without a restart of JabRef [#3064](https://github.com/JabRef/jabref/issues/3064)
- We fixed an issue where <kbd>DEL</kbd>, <kbd>Ctrl</kbd>+<kbd>C</kbd>, <kbd>Ctrl</kbd>+<kbd>V</kbd> and <kbd>Ctrl</kbd>+<kbd>A</kbd> in the search field triggered corresponding actions in the main table [#3067](https://github.com/JabRef/jabref/issues/3067)
- We fixed an issue where JabRef freezed when editing an assigned file in the `General`-Tab [#2930, comment](https://github.com/JabRef/jabref/issues/2930#issuecomment-311050976)
- We fixed an issue where a file could not be assigned to an existing entry via the entry context menu action `Attach file` [#3080](https://github.com/JabRef/jabref/issues/3080)
- We fixed an issue where entry editor was not focused after opening up. [#3052](https://github.com/JabRef/jabref/issues/3052)
- We fixed an issue where changes in the source tab were not stored when selecting a new entry. [#3086](https://github.com/JabRef/jabref/issues/3086)
- We fixed an issue where the other tab was not updated when fields where changed in the source tab. [#3063](https://github.com/JabRef/jabref/issues/3063)
- We fixed an issue where the source tab was not updated after fetching data by DOI. [#3103](https://github.com/JabRef/jabref/issues/3103)
- We fixed an issue where the move to group operation did not remove the entry from other groups [#3101](https://github.com/JabRef/jabref/issues/3101)
- We fixed an issue where the main table was not updated when grouping changes [#1903](https://github.com/JabRef/jabref/issues/1903)

## [4.0-beta2] – 2017-07-18

### Changed
- We moved the `adsurl` field to `url` field when fetching with the ADS fetcher.
- We continued to improve the new groups interface:
  - You can now again select multiple groups (and a few related settings were added to the preferences) [#2786](https://github.com/JabRef/jabref/issues/2786).
  - We further improved performance of group operations, especially of the new filter feature [#2852](https://github.com/JabRef/jabref/issues/2852).
  - It is now possible to resort groups using drag & drop [#2785](https://github.com/JabRef/jabref/issues/2785).
- The entry editor got a fresh coat of paint:
  - Homogenize the size of text fields.
  - The buttons were changed to icons.
  - Completely new interface to add or modify linked files.
  - Removed the hidden feature that a double click in the editor inserted the current date.
  - Complete new implementation of the the auto complete feature.
- All authors and editors are separated using semicolons when exporting to csv. [#2762](https://github.com/JabRef/jabref/issues/2762)
- Improved wording of "Show recommendations: into "Show 'Related Articles' tab" in the preferences
- We added integration of the Library of Congress catalog as a fetcher based on the [LCCN identifier](https://en.wikipedia.org/wiki/Library_of_Congress_Control_Number). [Feature request 636 in the forum](http://discourse.jabref.org/t/loc-marc-mods-connection/636)
- The integrity check for person names now also tests that the names are specified in one of the standard BibTeX formats.
- Links in the Recommended Articles tab (Mr.DLib), when clicked, are now opened in the system's default browser. [2931](https://github.com/JabRef/jabref/issues/2931)
- We improved the duplicate checker such that different editions of the same publication are not marked as duplicates. [2960](https://github.com/JabRef/jabref/issues/2960)

### Fixed
- We fixed a bug that leaves .sav file after SaveAs [#2947](https://github.com/JabRef/jabref/issues/2947)
- We fixed the function "Edit - Copy BibTeX key and link" to pass a hyperlink rather than an HTML statement.
- We fixed the adding of a new entry from DOI which led to a connection error. The DOI resolution now uses HTTPS to protect the user's privacy.[#2879](https://github.com/JabRef/jabref/issues/2897)
- We fixed the IEEE Xplore web search functionality [#2789](https://github.com/JabRef/jabref/issues/2789)
- We fixed an error in the CrossRef fetcher that occurred if one of the fetched entries had no title
- We fixed an issue that prevented new entries to be automatically assigned to the currently selected group [#2783](https://github.com/JabRef/jabref/issues/2783).
- We fixed a bug that only allowed parsing positive timezones from a FileAnnotation [#2839](https://github.com/JabRef/jabref/issues/2839)
- We fixed a bug that did not allow the correct re-export of the MS-Office XML field `msbib-accessed` with a different date format [#2859](https://github.com/JabRef/jabref/issues/2859).
- We fixed some bugs that prevented the display of FileAnnotations that were created using the Foxit Reader. [#2839, comment](https://github.com/JabRef/jabref/issues/2839#issuecomment-302058227).
- We fixed an error that prevented the FileAnnotation tab to load when the entry had no bibtexkey [#2903](https://github.com/JabRef/jabref/issues/2903).
- We fixed a bug which which could result in an exception when opening/saving files from/to a nonexistent directory [#2917](https://github.com/JabRef/jabref/issues/2917).
- We fixed a bug where recursive RegExpBased search found a file in a subdirectory multiple times and non-recursive RegExpBased search erroneously found files in subdirectories.
- We fixed a bug where new groups information was not stored on save [#2932](https://github.com/JabRef/jabref/issues/2932)
- We fixed a bug where the language files for Brazilian Portugese could not be loaded and the GUI localization remained in English [#1128](https://github.com/JabRef/jabref/issues/1182)
- We fixed a bug where the database was not marked as dirty when entries or groups were changed [#2787](https://github.com/JabRef/jabref/issues/2787)
- We fixed a bug where editors in the DocBook export were not exported [#3020](https://github.com/JabRef/jabref/issues/3020)
- We fixed a bug where the source tab was not updated when one the fields was changed [#2888](https://github.com/JabRef/jabref/issues/2888)
- We restored the original functionality that when browsing through the MainTable, the Entry Editor remembers which tab was opened before [#2896](https://github.com/JabRef/jabref/issues/2896)

## [4.0-beta] – 2017-04-17

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
    - Description text that is shown on mouse hover (implements old feature requests [489](https://sourceforge.net/p/jabref/feature-requests/489/) and [818](https://sourceforge.net/p/jabref/feature-requests/818/))
- We introduced "automatic groups" that automatically create subgroups based on a certain criteria (e.g., a subgroup for every author or keyword) and supports hierarchies. Implements [91](https://sourceforge.net/p/jabref/feature-requests/91/), [398](https://sourceforge.net/p/jabref/feature-requests/398/), [#1173](https://github.com/JabRef/jabref/issues/1173) and [#628](https://github.com/JabRef/jabref/issues/628).
- We added a document viewer which allows you to have a glance at your PDF documents directly from within JabRef.
- Using "Look up document identifier" in the quality menu, it is possible to look up DOIs, ArXiv ids and other identifiers for multiple entries.
- Comments in PDF files can now be displayed inside JabRef in a separate tab
- We separated the `Move file` and `Rename Pdfs` logic and context menu entries in the `General`-Tab for the Field `file` to improve the semantics
- We integrated support for the [paper recommender system Mr.DLib](http://help.jabref.org/en/EntryEditor#related-articles-tab) in a new tab in the entry editor.
- We renamed "database" to "library" to have a real distinction to SQL databases ("shared database") and `bib` files ("library"). [#2095](https://github.com/JabRef/jabref/issues/2095)
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
- We fixed and improved the auto detection of the [OpenOffice and LibreOffice connection](http://help.jabref.org/en/OpenOfficeIntegration)
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
 - Fixed an issue where names were split inconsistently with the BibTeX conventions [#2652](https://github.com/JabRef/jabref/issues/2652)
 - <kbd>Ctrl</kbd> + <kbd>A</kbd> now correctly selects all entries again. [#2615](https://github.com/JabRef/jabref/issues/#2615)
 - We fixed an issue where the dialog for selecting the main file directory in the preferences opened the wrong folder
 - OpenOffice text formatting now handles nested tags properly [#2483](https://github.com/JabRef/jabref/issues/#2483)
 - The group selection is no longer lost when switching tabs [#1104](https://github.com/JabRef/jabref/issues/1104)


## Older versions

The changelog of versions 3.x is available at the [v3.8.2 tag](https://github.com/JabRef/jabref/blob/v3.8.2/CHANGELOG.md).
The changelog of 2.11 and versions before is available as [text file in the v2.11.1 tag](https://github.com/JabRef/jabref/blob/v2.11.1/CHANGELOG).

[Unreleased]: https://github.com/JabRef/jabref/compare/v4.1...HEAD
[4.1]: https://github.com/JabRef/jabref/compare/v4.0...v4.1
[4.0]: https://github.com/JabRef/jabref/compare/v4.0-beta3...v4.0
[4.0-beta3]: https://github.com/JabRef/jabref/compare/v4.0-beta2...v4.0-beta3
[4.0-beta2]: https://github.com/JabRef/jabref/compare/v4.0-beta...v4.0-beta2
[4.0-beta]: https://github.com/JabRef/jabref/compare/v3.8.2...v4.0-beta
[2.11.1]: https://github.com/JabRef/jabref/compare/v2.11...v2.11.1
[JavaFX]: https://en.wikipedia.org/wiki/JavaFX
