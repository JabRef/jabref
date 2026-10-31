# Dialog Font Preference Investigation

Investigation for #13670. This document records the coverage of all 61 user-facing dialogs: a summary, what has been tested, and what remains.

## Summary

- Dialogs were tested at font sizes 9 (default) and 16, with JabRef run from the `main` branch. The font size is applied through `ThemeManager` (CSS font-size classes 8–20, default 9).
- A full inventory of the codebase found 61 user-facing dialogs. Tested so far: 22 of them, plus 6 dialogs created through the shared dialog service. Remaining: 39, listed below.
- No tested dialog ignored the font size or needed a reopen/restart to apply it — the modes from #13670 (`IGNORES-SIZE` / `NEEDS-REOPEN` / `NEEDS-RESTART`) did not occur. The problems found are layouts breaking at the larger size.
- Results: 18 dialogs show layout problems at font size 16, 2 more show a border issue at both sizes (possibly not font-related), and 9 dialogs showed no issue.

## Tested

Per-dialog findings are maintained in the bug report issue (`#17648`). This section is an index of the dialogs tested, grouped by outcome, without repeating those findings.

### Truncation, overlap, or overflow at font size 16

Compared at font sizes 9 and 16:

- **API Key** — `ApiKeyDialog.java`
- **Cleanup** — `CleanupDialog.java`
- **Add New File Type** — `EditExternalFileTypeEntryDialog.java`
- **Replace String** — `ReplaceStringView.java`
- **Shared Database Login** — `SharedDatabaseLoginDialogView.java`
- **Library Properties — citation key patterns tab** — `LibraryPropertiesView.java`
- **Library Properties — Content selectors tab** — `LibraryPropertiesView.java`
- **Parse LaTeX** — `ParseLatexDialogView.java`

Observed at font size 16 only:

- **Search Results** — `GlobalSearchResultDialog.java`
- **Add Group** — `GroupDialogView.java`
- **Customize Entry Table** — `EntryTableConfigurationDialog.java`
- **Searchable preferences table** — `PreferencesFilterDialog.java`

### Incomplete message-area border

Observed at font size 16:

- **TeXstudio error dialog** — `JabRefDialogService.java`
- **Rebuild Fulltext Search Index** — `JabRefDialogService.java`
- **Reset Preferences** — `JabRefDialogService.java`
- **Clear Embeddings Cache** — `JabRefDialogService.java`
- **Redownload Missing Files** — `JabRefDialogService.java`
- **Automatically set file links** — `AutoLinkFilesAction.java` (warning dialog via `JabRefDialogService.java`)

Also observed at font sizes 9 and 16:

- **Event Log** — `ErrorConsoleView.java`
- **Save before closing** — `LibraryTab.java`

### No issue observed

- **About** — `AboutDialogView.java`
- **Preferences** — `PreferencesDialogView.java`
- **Change Appearance** — `ThemeDialog.java`
- **Optimize for Large Libraries** — `LargeLibraryOptimizationDialog.java`
- **Push to Applications** — `PushApplicationDialog.java`
- **Web Search Services** — `OnlineServicesDialog.java`
- **Create/Edit Exporter** — `CreateModifyExporterDialogView.java`
- **New Protected Terms File** — `NewProtectedTermsFileDialog.java`
- **New Entry** — `NewEntryView.java`

## Remaining to test (39)

- **Merge Entries** (Quality → Merge entries). `MergeEntriesDialog.java`
- **Multi-Merge Entries** (PDF metadata merge; entry context menu → Update with bibliographic information from web). `MultiMergeEntriesView.java`
- **Duplicate Resolver** (Quality → Find duplicates; also during import). `DuplicateResolverDialog.java`
- **Integrity Check** (Quality → Check integrity). `IntegrityCheckDialog.java`
- **Consistency Check** (Quality → Check consistency). `ConsistencyCheckDialog.java`
- **Consistency Symbols** (legend button inside Consistency Check). `ConsistencySymbolsDialog.java`
- **Import Entries** (File → Import; also drag-and-drop and web search import). `ImportEntriesDialog.java`
- **Import Custom Entry Types** (automatic on library open when custom entry types are detected). `ImportCustomEntryTypesDialog.java`
- **New Sublibrary from AUX** (Tools → New sublibrary from AUX file). `FromAuxDialog.java`
- **Copy Linked Files** (Tools → Copy linked files to folder). `CopyFilesDialogView.java`
- **Find Unlinked Files Wizard** (Tools → Find unlinked files). `UnlinkedFilesWizard.java`
- **Git Commit** (File → Git → Commit changes). `GitCommitDialogView.java`
- **Git Diff** (View diff inside Git Commit). `GitDiffDialogView.java`
- **Share to GitHub** (File → Git → Share to GitHub). `GitShareToGitHubDialogView.java`
- **Database Changes Resolver** (File → Merge library; also on external-change alert). `DatabaseChangesResolverDialog.java`
- **Backup Resolver** (automatic on library open when a newer backup file is found). `BackupResolverDialog.java`
- **Manage Keywords** (Edit → Manage keywords). `ManageKeywordsDialog.java`
- **Automatic Field Editor** (Edit → Edit field values). `AutomaticFieldEditorDialog.java`
- **Linked File Edit** (entry editor → file links → Add/Edit). `LinkedFileEditDialog.java`
- **Main File Directory** (Welcome → Quick settings → Main file directory). `MainFileDirectoryDialog.java`
- **Cite Special** (OpenOffice/LibreOffice panel → Cite Special). `CiteSpecialDialogView.java`
- **Manage Citations** (OpenOffice/LibreOffice panel → Manage citations). `ManageCitationsDialogView.java`
- **Modify Bibliography Properties** (OpenOffice/LibreOffice panel). `ModifyBibliographyPropertiesDialogView.java`
- **Style Select** (OpenOffice/LibreOffice panel → Select style). `StyleSelectDialogView.java`
- **AI Summary Parameters** (AI summary tab → Regenerate with custom parameters). `AiSummaryParametersDialog.java`
- **Related Work — Extract Comments** (Tools → Extract related work comments). `RelatedWorkDialogView.java`
- **Related Work Results** (opens after the extraction dialog). `RelatedWorkResultDialogView.java`
- **Manage Study Definition (SLR)** (Tools → Systematic literature review → Start new study). `ManageStudyDefinitionView.java`
- **ZIP File Chooser** (choosing an importer from a ZIP/JAR archive). `ZipFileChooser.java`
- **BibTeX Source Viewer** (entry editor → Citation Relations tab → BibTeX source). Built inside `CitationRelationsTab.java`
- **Protected Terms File Content Viewer** (Preferences → Protected terms → View). Built inside `ProtectedTermsTabViewModel.java`
- **Proxy Password** (automatic at startup when a proxy password is needed). Via `JabRefDialogService.java`
- **Background Tasks Exit Confirmation** (automatic when quitting with background tasks running). Via `JabRefDialogService.java`
- **AI Chat Status Window** (a window, not a modal dialog — AI Chat pane → info button). `AiChatStatusWindow.java`
- **AI Group Chat Window** (a window — Groups panel → Chat with group). `AiGroupChatWindow.java`
- **Document Viewer** (a window — View → Show document viewer). `DocumentViewerView.java`
- **Jump to Field** (Entry Editor → Jump to field): located in source; font-size behaviour not verified. `JumpToFieldDialog.java`
- **Parse LaTeX Result**: located in source; not tested. `ParseLatexResultView.java`
- **New Version** (Help → Check for updates): not tested — the application was up to date. `NewVersionDialog.java`

## Font Preference Mechanism

JabRef applies the selected font size to application windows through `ThemeManager`. The stylesheet defines font-size classes from 8 to 20, with 9 as the default (`jabgui/src/main/java/org/jabref/gui/theme/ThemeManager.java`, `jabgui/src/main/resources/org/jabref/gui/theme/internal/jabref-base.css`). This confirms how the font preference is applied. It does not prove that every dialog layout works correctly at larger font sizes.
