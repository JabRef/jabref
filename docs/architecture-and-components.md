---
---
# Architecture and Components

This page discusses JabRef's main architecture and components.
Note that components are seen as "logical" components and summarize features and "real" code-components.

## High-level architecture

JabRef's code is structured into these packages:

- The `model` package encompasses the most important data structures (`BibDatases`, `BibEntries`, `Events`, and related aspects) and has minimal logic attached.
- The `logic` package is responsible for business logic such as reading/writing/importing/exporting and manipulating the `model`, and it is structured often as an API the `gui` can call and use.
- Only the `gui` knows the user and their preferences and can interact with them to help them solving tasks.
- For each layer, we form packages according to their responsibility, i.e., vertical structuring.
- The `model` classes should have no dependencies to other classes of JabRef and the `logic` classes should only depend on `model` classes.
- The `cli` package bundles classes that are responsible for JabRef's command line interface.
- The `preferences` package represents all information customizable by a user for their personal needs.

We use an event bus to publish events from the `model` to the other layers.
This allows us to keep the architecture but still react upon changes within the core in the outer layers.
Note that we are currently switching to JavaFX's observables, as we aim for a stronger coupling to the data producers.

### Package Structure

Permitted dependencies in our architecture are:

```monospaced
gui --> logic --> model
gui ------------> model
gui ------------> preferences
gui ------------> cli
gui ------------> global classes

logic ------------> model

global classes ------------> everywhere

cli ------------> model
cli ------------> logic
cli ------------> global classes
cli ------------> preferences
```

All packages and classes which are currently not part of these packages (we are still in the process of structuring) are considered as gui classes from a dependency standpoint.

### Most Important Classes and their Relation

Both GUI and CLI are started via the `JabRefMain` which, in turn, calls `JabRef` which then decides whether the GUI (`JabRefFrame`) or the CLI (`JabRefCLI` and a lot of code in `JabRef`) will be started. The `JabRefFrame` represents the Window which contains a `SidePane` on the left used for the fetchers/groups. Each tab is a `BasePanel` which has a `SearchBar` at the top, a `MainTable` at the center and a `PreviewPanel` or an `EntryEditor` at the bottom. Any right click on the `MainTable` is handled by the `RightClickMenu`. Each `BasePanel` holds a `BibDatabaseContext` consisting of a `BibDatabase` and the `MetaData` (which is relevant data of the currently focussed database). A `BibDatabase` has a list of `BibEntries`. Each `BibEntry` has an ID, a citation key and a key/value store for the fields with their values. Interpreted data (such as the type or the file field) is stored in the `TypedBibentry` type. The user can change the `JabRefPreferences` through the `PreferencesDialog`.

## Architectural Decision Records

JabRef collects core architecture decisions using "Architectural Decision Records".
They are available at <https://devdocs.jabref.org/decisions/>.

For new ADRs, please use [adr-template.md](https://github.com/JabRef/jabref/blob/main/docs/decisions/adr-template.md) as basis.
More information on MADR is available at <https://adr.github.io/madr/>.
General information about architectural decision records is available at <https://adr.github.io/>.

## Components

We regard each "larger" feature as component.
Each such component gets a label "component: {component-name}" to enable ease issue searching of it.

### AI

- Open issues: [component: ai](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+ai%22)
- Docs: <https://docs.jabref.org/ai>

This component covers AI-assisted features in JabRef, such as "chatting" with PDF files.

### Autocompletion

- Open issues: [component: autocompletion](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+autocompletion%22)
- Docs: [Autocompletion](https://docs.jabref.org/advanced/entryeditor#word-name-autocompletion)

This component handles the automatic suggestion of previously used field values (e.g., authors, journals) while editing entries to improve data consistency and entry speed.

### Automatic Field Editor

- Open issues: [component: automatic-field-editor](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+automatic-field-editor%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/managing-field-names-and-their-content>

This component refers to the automatic editing or updating of entry fields based on predefined rules or heuristics, such as generating citation keys or filling missing metadata.

### Backup

- Open issues: [component: backup](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+backup%22)
- Docs: <https://docs.jabref.org/advanced/autosave>

This component manages the creation and recovery of automatic backups and autosave files to prevent data loss during unexpected shutdowns or crashes.

### Bib(La)TeX

- Open issues: [component: bib(la)tex](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+bib(la)tex%22)
- Docs: <https://docs.jabref.org/cite/bibtex-and-biblatex>

This component deals with BibTeX and BibLaTeX support, including parsing, validation, formatting, and compatibility with LaTeX-based workflows.

### Browser Plugin

- Open issues: [component: browser-plugin](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+browser-plugin%22)
- Docs: <https://docs.jabref.org/collect/jabref-browser-extension>

This component refers to the JabRef browser extension, which allows direct import of bibliographic metadata from supported websites into the JabRef library.

### Citation Key Generator

- Open issues: [component: citationkey-generator](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+citationkey-generator%22)
- Docs: <https://docs.jabref.org/setup/citationkeypatterns>

This component manages the generation of citation keys based on customizable patterns, ensuring consistent and meaningful citation identifiers for BibTeX/BibLaTeX entries.

### Citation Relations

- Open issues: [component: citation relations](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+citation+relations%22)
- Docs: TBD

This component focuses on features that analyze and visualize relationships between cited and citing works, helping users understand bibliographic networks and dependencies.

### Cite-As-You-Write

- Open issues: [component: cite-as-you-write](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+cite-as-you-write%22)
- Docs: TBD

This component covers integration with TeX processors to allow inserting citations and bibliographies directly while writing documents similar to [Better BibTeX for Zotero's cite as you write](https://retorque.re/zotero-better-bibtex/citing/cayw/).

### Cleanup Operations

- Open issues: [component: cleanup-ops](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+cleanup-ops%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/cleanupentries> and <https://docs.jabref.org/finding-sorting-and-cleaning-entries/saveactions>

This component manages automatic and manual cleanup actions such as formatting fields, unifying entry data, removing unwanted characters, and applying field-specific transformations.

### Duplicate Finder

- Open issues: [component: duplicate-finder](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+duplicate-finder%22)
- Docs: [Find duplicates](https://docs.jabref.org/finding-sorting-and-cleaning-entries/findduplicates)

This component detects and helps resolve duplicate bibliographic entries.

### Entry Editor

- Open issues: [component: entry-editor](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+entry-editor%22)
- Docs: [Entry Editor](https://docs.jabref.org/advanced/entryeditor)

This component is responsible for the editing interface of bibliographic entries, including field input, validation, source tab, and customization of visible fields.

### Entry Preview

- Open issues: [component: entry-preview](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+entry-preview%22)
- Docs: <https://docs.jabref.org/setup/preview>

This component controls the preview pane that renders formatted citations or abstracts of the selected entry, using configurable citation styles such as APA, IEEE, or custom layouts.

### Event Bus

- Open issues: [component: event bus](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+event+bus%22)
- Docs: <../code-howtos/eventbus.md>

This component manages JabRef’s internal event bus system, which enables communication between decoupled components through event publishing and subscription.

### Export or Save

- Open issues: [component: export-or-save](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+export-or-save%22)
- Docs: <https://docs.jabref.org/collaborative-work/export>

This component deals with saving and exporting bibliographic data to various formats and destinations, including file exports, custom layouts, and compatibility with external tools.

### External Changes

- Open issues: [component: external-changes](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+external-changes%22)
- Docs: TBD

This component monitors and responds to modifications of library files made outside JabRef, allowing users to review and merge external changes into the current library.

### External Files

- Open issues: [component: external-files](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+external-files%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks>

This component manages the linking, detection, renaming, moving, and organization of external files (such as PDFs) associated with bibliographic entries.

## Fetcher

- Open issues: [component: fetcher](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+fetcher%22)
- Docs: <https://docs.jabref.org/collect>

This component handles the retrieval of bibliographic data from online sources such as CrossRef, PubMed, arXiv, and other academic databases using fetchers integrated into JabRef.

## GitHub Action

- Open issues: [component: github-action](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+github-action%22)
- Docs: TBD

This component refers to the GitHub Action offered by JabRef.

### Groups

- Open issues: [component: groups](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+groups%22)
- Docs: [Groups](https://docs.jabref.org/finding-sorting-and-cleaning-entries/groups)

This component manages the grouping functionality in JabRef, allowing users to organize and categorize entries using static or dynamic groups.

### Import/Load

- Open issues: [component: import-load](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+import-load%22)
- Docs: <https://docs.jabref.org/collect>

This component handles loading of existing bibliographic files and importing data from external sources, such as databases, file formats (BibTeX, RIS, etc.), or PDFs.

### Installation

- Open issues: [component: installation](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+installation%22)
- Docs: [Installation](https://docs.jabref.org/installation)

This component includes issues related to installing JabRef on supported platforms (Windows, macOS, Linux), including packaging, platform-specific bugs, and setup instructions.

### Integrity Checker

- Open issues: [component: integrity-checker](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+integrity-checker%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/checkintegrity>

This component validates entries against predefined rules to detect inconsistencies, missing fields, or incorrect formatting, helping maintain a high-quality bibliographic library.

### JabKit [CLI]

- Open issues: [component: JabKit [cli]](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+JabKit+%5Bcli%5D%22)
- Docs: TBD

This component covers the command-line interface for JabRef, known as JabKit, enabling batch operations such as conversion, validation, or citation key generation without the GUI.

### Journal Abbreviations

- Open issues: [component: journal abbreviations](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+journal+abbreviations%22)
- Docs: <https://docs.jabref.org/advanced/journalabbreviations>

This component deals with the management and application of journal abbreviation lists, supporting consistent formatting for citations in different publication styles.

## Keybinding

- Open issues: [component: keybinding](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+keybinding%22)
- Docs: <https://docs.jabref.org/setup/customkeybindings>

This component manages keyboard shortcuts within JabRef, allowing users to configure and use key combinations for quicker navigation and action execution.

### Keywords

- Open issues: [component: keywords](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+keywords%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/keywords>

This component handles the management of keywords in bibliographic entries, including editing, merging, filtering, and automatic keyword generation or cleaning.

### LaTeX Citations

- Open issues: [component: latex-citations](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+latex-citations%22)
- Docs: <https://docs.jabref.org/advanced/entryeditor/latex-citations>

This component manages support for LaTeX citation formats, including parsing and interpreting `\cite` commands in `.tex` files and linking them to corresponding BibTeX entries.

### Logging

- Open issues: [component: logging](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+logging%22)
- Docs: TBD

This component handles JabRef’s internal logging infrastructure, including debug output, error reporting, log file generation, and diagnostics to aid development and support.

### Main Table

- Open issues: [component: maintable](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+maintable%22)
- Docs: <https://docs.jabref.org/getting-started>

This component refers to the central entry table in JabRef, including its layout, column configuration, sorting, grouping, and customization of displayed fields.

### Microsoft Word Integration

- Open issues: [component: microsoft-word-integration](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+microsoft-word-integration%22)
- Docs: <https://docs.jabref.org/cite/export-to-microsoft-word>

### PDF Viewer

- Open issues: [component: pdf viewer](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+pdf+viewer%22)
- Docs: TBD

This component relates to the built-in PDF viewer functionality in JabRef, including rendering PDFs and annotations.

## Preferences

- Open issues: [component: preferences](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+preferences%22)
- Docs: TBD

Covers all aspects of configuration settings for features, appearance, and behavior.

### Search

- Open issues: [component: search](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+search%22)
- Docs: <https://docs.jabref.org/finding-sorting-and-cleaning-entries/search>

This component covers the search functionality within JabRef, including simple and advanced search modes, live filtering, and field-specific queries.

### SLR

- Open issues: [component: slr](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+slr%22)
- Docs: [TBD](https://github.com/JabRef/user-documentation/issues/391)

This component provides support for conducting Systematic Literature Reviews, including study tracking, classification, and advanced filtering of bibliographic entries.

### Theming

- Open issues: [component: theming](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+theming%22)
- Docs: <https://themes.jabref.org/>

This component manages the visual appearance of JabRef, including support for light/dark themes, font settings, and overall UI styling customization.

### UI

- Open issues: [component: ui](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+ui%22)
- Docs: <https://docs.jabref.org/getting-started>

This component encompasses the graphical user interface elements of JabRef, including layout, styling, usability, and interaction design across all views and dialogs.

### Unicode

- Open issues: [component: unicode](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+unicode%22)
- Docs: TBD

This component ensures correct handling, display, and conversion of Unicode characters in bibliographic entries, metadata, and file contents.

### Welcome Tab

- Open issues: [component: welcome tab](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+welcome+tab%22)
- Docs: TBD

This component covers the welcome/startup screen shown when JabRef launches, providing quick access to recent libraries, documentation, and getting-started resources.
