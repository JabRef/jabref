# Architecture and Components

This page discusses JabRef's main architecture and components.
Note that components are seen as "logical" components and summarize features and "real" code-components.

## High-level architecture

In JabRef's code structure,

The `model` package encompasses the most important data structures (`BibDatases`, `BibEntries`, `Events`, and related aspects) and has minimal logic attached.
The `logic` package is responsible for business logic such as reading/writing/importing/exporting and manipulating the `model`, and it is structured often as an API the `gui` can call and use.
Only the `gui` knows the user and their preferences and can interact with them to help them solving tasks.
For each layer, we form packages according to their responsibility, i.e., vertical structuring.
The `model` classes should have no dependencies to other classes of JabRef and the `logic` classes should only depend on `model` classes.
The `cli` package bundles classes that are responsible for JabRef's command line interface.
The `preferences` package represents all information customizable by a user for their personal needs.

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

### UI

- Open issues: [component: ui](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+ui%22)
- Docs: <https://docs.jabref.org/getting-started>

This component encompasses the graphical user interface elements of JabRef, including layout, styling, usability, and interaction design across all views and dialogs.

### PDF Viewer

- Open issues: [component: pdf viewer](https://github.com/JabRef/jabref/issues?q=is%3Aissue+is%3Aopen+label%3A%22component%3A+pdf+viewer%22)
- Docs: TBD

This component relates to the built-in PDF viewer functionality in JabRef, including rendering PDFs and annotations.
