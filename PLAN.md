# PLAN: Directory as library — Hayagriva sidecar files

Open a directory as a JabRef library: a Hayagriva `.yml` sidecar next to each PDF carries the
bibliographic entry **and the user's notes**; the main table fills from the `.pdf`/`.yml` files
found in the directory tree; the library stays in live two-way sync with the file system.
As a second step, the groups panel mirrors the directory structure
([#10930](https://github.com/JabRef/jabref/issues/10930)).

Status: **design document** — no implementation yet. All file/line references verified against
`main` at `f57b76876f` (includes the Hayagriva importer, PR
[#16190](https://github.com/JabRef/jabref/pull/16190)).

## 1. User story

1. The user keeps papers as PDFs in a folder tree (possibly synced via Dropbox/Nextcloud/git).
2. *File → Open folder as library…* — JabRef scans the tree and shows one entry per
   `.yml`-described work; PDFs without a sidecar appear as stub entries with metadata pulled
   from the PDF in the background.
3. Editing an entry (including its notes, stored in the Hayagriva `note` field) writes the
   change back to the entry's `.yml` — no Ctrl+S needed. Creating/renaming/deleting files
   outside JabRef shows up in the open library within ~1 second.
4. The `.yml` files are plain Hayagriva, so the same folder is directly usable from Typst.

## 2. Settled design decisions

- **Bare PDFs**: become stub entries (background metadata import); the sidecar `.yml` is only
  written once the user edits/confirms the entry — scanning never writes files.
- **Write-back**: live incremental sync, debounced; no explicit save step.
- **Renames**: PDF and `.yml` always share a basename and are renamed **together**. The
  basename is produced by the existing filename-generation machinery
  (`FilePreferences.getFileNamePattern()` → `FileUtil.createFileNameFromPattern` /
  `BracketedPattern`, e.g. `[citationkey] - [title]`, applied via `LinkedFileHandler` — the
  same code path as the *Rename file to configured pattern* cleanup). The pattern is a global
  preference today; a per-library override is added for directory libraries (see §8).
  No mass-rename on open/scan: files are renamed only when an entry is edited or the cleanup
  is run explicitly.
- **Notes**: Hayagriva `note` ↔ `StandardField.NOTE` — already round-trips in the importer and
  exporter; no format extension needed for notes.

## 3. Architecture

### 3.1 Mirror the shared-SQL seam, don't implement it

`DatabaseSynchronizer` (`jablib/.../logic/shared/DatabaseSynchronizer.java`) is SQL-shaped
(`openSharedDatabase`, `getConnectionProperties`, `getDBName`); implementing it for a
filesystem backend would force dead stubs. Instead:

- New `jablib/src/main/java/org/jabref/logic/directorylibrary/DirectoryLibrarySynchronizer`.
- `BibDatabaseContext.convertToDirectoryLibrary(DirectoryLibrarySynchronizer)`, symmetric to
  `convertToSharedDatabase` (`BibDatabaseContext.java:263-270`): installs a
  `CoarseChangeFilter`, registers the synchronizer, sets the new location; teardown extends
  `convertToLocalDatabase()`.

### 3.2 `DatabaseLocation.DIRECTORY`, empty `databasePath`

A directory library has no `.bib`, so `databasePath` stays empty. That yields correct defaults
at almost every existing switch:

| Call site | Behavior with `DIRECTORY` + empty path |
|---|---|
| `LibraryTab.isDatabaseReadyForAutoSave` | false → no `AutosaveManager` (synchronizer persists) |
| `LibraryTab.isDatabaseReadyForBackup` | false → no `BackupManager` |
| `DatabaseChangeMonitor` install | never installed (needs a path) — our watcher replaces it |
| `LibraryTab.getUntitledLibraryNumber` | directory tabs never counted as "untitled" |
| `ActionHelper.needsSavedLocalDatabase` | false → e.g. git actions correctly disabled |

Call sites needing explicit branches: `LibraryTab` (tab title = directory name, `requestClose`,
`onClosed` teardown), `SaveDatabaseAction` (save = flush + notify; save-as = flush, tear down
synchronizer, convert the tab to a regular `.bib` library — mirrors the SHARED branch at
`SaveDatabaseAction:143-149`, and doubles as "export directory library to .bib"). Roughly five
further `getLocation()` sites (`FromAuxDialogViewModel`, `ImportEntriesDialog`,
`RightClickMenu`, `GeneralPropertiesViewModel`, `SharedDatabaseLoginDialogViewModel`) get a
one-line review; most compare `== SHARED` and are fine as-is.

### 3.3 Watcher

- ADR-0030 (`docs/decisions/0030-use-apache-commons-io-for-directory-monitoring.md`) already
  commits to Apache Commons-IO monitoring **for exactly this feature family** (it names
  #10930).
- Promote `jabgui/.../gui/util/DirectoryMonitor.java` (Commons-IO `FileAlterationMonitor`,
  1 s poll; verified free of GUI imports) to `jablib/.../logic/util/DirectoryMonitor.java`.
  jablib's `module-info.java` already has `requires org.apache.commons.io` — no new
  dependency. Consumers to update: `JabRefGUI` (DI registration + shutdown),
  `LatexCitationsTabViewModel`.
- The observer is filtered to `*.yml`/`*.yaml`/`*.pdf` to keep the periodic tree walk cheap.
- GUI-thread marshalling is injected (`Consumer<Runnable>`): the GUI passes a
  `UiTaskExecutor`-based marshaller, headless callers pass `Runnable::run` — jablib stays free
  of JavaFX.

### 3.4 Echo-loop prevention

- **Inbound** (file → `BibDatabase`): all mutations use `EntriesEventSource.SHARED`; outbound
  handlers accept only `LOCAL || UNDO` (same policy as `DBMSSynchronizer.isEventSourceAccepted`).
- **Outbound** (entry → file): after each write, record a per-path fingerprint (SHA-256 of the
  written bytes + size + mtime). When the poll later fires `onFileChange`/`onFileCreate` for
  that path, a matching fingerprint identifies our own write and the event is swallowed.
- **Serialization**: one single-threaded "directory-sync" executor per library; watcher
  callbacks and `@Subscribe` handlers only enqueue. All file I/O and catalog mutation happen on
  that executor; model mutation is marshalled to the FX thread.

## 4. Entry ↔ file identity (`DirectoryLibraryCatalog`)

```
Map<String entryId, EntrySource(ymlFile, hayagrivaKey)>   // BibEntry.getId() — stable in-memory anchor
Map<Path, List<String entryIds>>                          // file → members, in file order
Map<Path, FileFingerprint>                                // echo suppression
```

- The **top-level YAML map key is the citation key** (Hayagriva's format). It is tracked
  separately from `BibEntry.getId()`, so a citation-key edit in JabRef renames the YAML key on
  write-back without losing the entry's identity.
- **Pairing**: `X.yml`/`X.yaml` next to `X.pdf` (same directory, same basename). Sidecar
  entries get an in-memory `LinkedFile` to the paired PDF, relative to the library root —
  Hayagriva has no file-path field, so the association is convention-based and nothing
  JabRef-specific is written for it.
- **Multi-entry files** are supported (the format is a map of key → entry); each member
  remembers its file + key; any member change rewrites the file read-modify-write (§5). New
  JabRef-created entries never join existing multi-entry files; they get their own
  `<generated-filename>.yml` in the library root.
- **Rename model**: when write-back fires and pattern-relevant fields changed (citation key,
  title, …), both `.yml` and PDF are renamed to the newly generated basename and the
  `LinkedFile` is updated. An external rename of either side drags the partner along to restore
  the shared basename. Multi-entry files have no single generating entry and keep their name.
- **Inbound renames**: Commons-IO reports delete + create. Deletions are staged for a ~2-poll
  grace window; a create whose parsed entries match staged ones (citation keys + field maps) is
  treated as a move — the same `BibEntry` instances survive, preserving selection, undo history
  and group membership. Otherwise the removal is committed.
- **Deletion in JabRef**: remove the entry from its file; delete/trash the `.yml` when its last
  entry goes (respecting `FilePreferences.moveToTrash()`; the trash call is injected from the
  GUI since it uses AWT). The paired PDF is deleted only after the existing linked-file delete
  confirmation. Undoing a deletion re-creates the sidecar (the `UNDO`-sourced add event passes
  the outbound gate).
- **Citation-key collisions**: tolerated across files (`BibDatabase` allows duplicates); within
  one file the writer suffixes `_1`, `_2`, … and logs a warning.

## 5. Hayagriva round-trip writer (prerequisite)

The current exporter is a lossy layout `TemplateExporter` (`ExporterFactory.java:57`,
`resource/layout/hayagrivayaml.layout`, `HayagrivaType`) — unusable for incremental
read-modify-write persistence. Needed:

- **`HayagrivaMapping`**: extract the importer's tables (types, scalar fields, `serial-number`
  composition, `parent` synthesis, `affiliated` roles, person formatting) from
  `HayagrivaImporter` into one shared class; refactor the importer to use it (existing
  `HayagrivaImporterTest` guards behavior).
- **`HayagrivaEntryWriter`**: programmatic writer on the same Jackson `YAMLMapper`, writing
  through `AtomicFileWriter` (atomicity matters — another process may read mid-write).
  **Read-modify-write**: parse the existing file and mutate only the YAML paths the mapping
  owns for fields that actually changed. This preserves structured forms (`title: {value,
  short}`), person details (`prefix`/`suffix`/`alias`), nested parents, and any keys JabRef
  doesn't understand. Caveat: Jackson drops YAML `#` comments on any rewrite (documented; a
  comment-preserving YAML stack is out of scope). Key order is preserved.
- **`HayagrivaExporter`**: replace the layout exporter with a programmatic `Exporter`
  delegating to the writer, so *File → Export* and sidecar write-back share one code path.
- **JabRef-only fields** (keywords, groups, read status, …) have no Hayagriva field. To avoid
  losing them, the writer adds one namespaced sub-map inside the entry (e.g.
  `jabref: {keywords: …}`) which the importer reads back. This hinges on Typst's Hayagriva
  parser ignoring unknown entry fields — evidence it does: the upstream project's own
  `basic.yml` test fixture contains the made-up field `tongus: 2`. Verify before the
  write-back phase; fallback: drop with a warning.

## 6. Scan pipeline on open (`DirectoryLibraryScanner`)

1. **Enumerate**: recursive walk collecting `.yml`/`.yaml`/`.pdf`, honoring
   `GitIgnoreFileFilter` (verified GUI-free — move from `jabgui/.../externalfiles/` to jablib),
   skipping hidden and `.git` directories.
2. **Stage 1 — sidecars** (inside the tab's `BackgroundTask<ParserResult>`, standard loading
   spinner via `LibraryTab.createLibraryTab`): parse each YAML file with `HayagrivaImporter`,
   record `(file, hayagrivaKey)` per entry in the catalog, inject `LinkedFile`s for paired
   PDFs, and set `MetaData.setLibrarySpecificFileDirectory(root)` so
   `BibDatabaseContext.getFileDirectories()` resolves relative links despite the empty
   `databasePath`. Malformed YAML → `ParserResult` warning; the file is excluded from
   write-back and left untouched.
3. **Stage 2 — bare PDFs** (background, cancellable, visible in the task overlay): for each PDF
   without a sidecar and not linked by any parsed entry, run `PdfMergeMetadataImporter`
   (verbatim-BibTeX / embedded `.bib` / [GROBID] / XMP / content candidates, DOI-arXiv-ISBN
   enrichment, `RelativePathsCleanup` overload). Empty result → stub entry titled by filename
   (pattern: `ImportHandler.createEmptyEntryWithLink`). Inserts use source `SHARED`, so **no
   sidecar is written during scan** — it appears on the first user edit, which fires a `LOCAL`
   event into the write-back path. Fetcher failures are logged per file and never abort the
   scan (offline-friendly).

## 7. Directory-structure groups (#10930) — step 2

- New `jablib/.../model/groups/DirectoryStructureGroup`, modeled on
  `AutomaticGroup.createSubgroups(entries)` (GUI-materialized subtree), keyed on each entry's
  source-file path relative to the library root. The path lookup is injected as a
  `Function<BibEntry, Optional<Path>>` backed by the catalog, keeping the model layer free of
  logic dependencies.
- The group does **not** watch the filesystem itself — the synchronizer already does; on
  structural changes it calls `metaData.groupsBinding().invalidate()` (the `TexGroup` →
  `GroupNodeViewModel` refresh precedent).
- Registration checklist for a new group type: `GroupSerializer`, `GroupsParser`,
  `MetadataSerializationConfiguration` ID, and the capability switches in `GroupNodeViewModel`
  (`canAddEntriesIn=false`, `canBeDragged=false`, `canAddGroupsIn=false`, `canRemove=true`,
  `isEditable=false`), group-dialog exclusion. Auto-installed as a root child when a directory
  library opens.
- Future work (out of scope here): bidirectional moves — dragging an entry onto a directory
  group moves its sidecar + PDF.

## 8. Phased delivery (each phase = one reviewable PR)

| Phase | Content |
|---|---|
| **0** | `HayagrivaMapping` + `HayagrivaEntryWriter` + programmatic `HayagrivaExporter`, round-trip tests (jablib only; replaces the layout exporter) |
| **1** | *File → Open folder as library…*: `OpenDirectoryLibraryAction` (directory chooser), `StandardActions` constant, `MainMenu` item; `DatabaseLocation.DIRECTORY`; scanner + catalog; one-shot scan, in-memory edits, prompt on close. ADR + requirements doc + l10n + CHANGELOG |
| **2** | Live inbound sync: `DirectoryLibrarySynchronizer` inbound half; `DirectoryMonitor` moved to jablib; rename grace window; fingerprint echo filter. Tests pump `FileAlterationObserver.checkAndNotify()` directly (no sleeps) |
| **3** | Write-back: outbound `@Subscribe` handlers via `CoarseChangeFilter` (source-gated, `isFilteredOut()` respected, ~500 ms per-file debounce, `flush()`); pattern-driven pair renames; YAML-key rename on citation-key edit; deletion policy; close flushes instead of prompting |
| **4** | Directory-structure groups (#10930) per §7 |
| **5** | Polish: preferences (live vs. explicit write-back, trash vs. delete, auto-write sidecars on scan, per-library filename pattern — see §9 metadata question); recent-libraries & drag-drop routing for directories; fulltext-index path for path-less contexts; jabkit subcommand (+ `skills/users/jabkit/SKILL.md`); user-documentation PR |

## 9. Open questions / risks

- **Hayagriva parser tolerance** for the extra `jabref:` entry field — verify against
  typst/hayagriva before Phase 3 (fallback: drop JabRef-only fields with a warning).
- **Library-level MetaData persistence**: with no `.bib`, groups/save-actions/per-library
  filename pattern set via Library properties are volatile. Ignore for Phases 1–3; later
  option: a `jabref-metadata.yml` in the root — the per-library filename-pattern override
  lands together with this.
- **Rename + edit within one poll cycle** is detected as delete + add (entry identity lost);
  acceptable, documented.
- **Large directories**: Stage-2 fetcher traffic must be cancellable and rate-limited; the
  observer's file filter keeps the 1 s tree walk cheap, but very large trees should be
  measured.
- **Concurrent instances / cloud-sync folders**: atomic writes + fingerprints cover the common
  cases; conflict resolution is last-writer-wins, documented.
- **Pattern renames of multi-entry files**: no single generating entry → such files keep their
  name; only single-entry sidecars follow the pattern.
