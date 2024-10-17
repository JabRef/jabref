package org.jabref.gui.externalfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.libraryproperties.constants.ConstantsItemModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverResult.BREAK;

public class ImportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportHandler.class);
    private final BibDatabaseContext bibDatabaseContext;
    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public ImportHandler(BibDatabaseContext database,
                         GuiPreferences preferences,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager,
                         DialogService dialogService,
                         TaskExecutor taskExecutor) {

        this.bibDatabaseContext = database;
        this.preferences = preferences;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.linker = new ExternalFilesEntryLinker(preferences.getExternalApplicationsPreferences(), preferences.getFilePreferences(), database, dialogService);
        this.contentImporter = new ExternalFilesContentImporter(preferences.getImportFormatPreferences());
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getLinker() {
        return linker;
    }

    public BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesInBackground(final List<Path> files, final BibDatabaseContext bibDatabaseContext, final FilePreferences filePreferences) {
        return new BackgroundTask<>() {
            private int counter;
            private final List<ImportFilesResultItemViewModel> results = new ArrayList<>();

            @Override
            public List<ImportFilesResultItemViewModel> call() {
                counter = 1;
                CompoundEdit ce = new CompoundEdit();
                for (final Path file : files) {
                    final List<BibEntry> entriesToAdd = new ArrayList<>();

                    if (isCancelled()) {
                        break;
                    }

                    UiTaskExecutor.runInJavaFXThread(() -> {
                        updateMessage(Localization.lang("Processing file %0", file.getFileName()));
                        updateProgress(counter, files.size() - 1d);
                    });

                    try {
                        if (FileUtil.isPDFFile(file)) {
                            var pdfImporterResult = contentImporter.importPDFContent(file);
                            List<BibEntry> pdfEntriesInFile = pdfImporterResult.getDatabase().getEntries();

                            if (pdfImporterResult.hasWarnings()) {
                                addResultToList(file, false, Localization.lang("Error reading PDF content: %0", pdfImporterResult.getErrorMessage()));
                            }

                            if (!pdfEntriesInFile.isEmpty()) {
                                entriesToAdd.addAll(FileUtil.relativize(pdfEntriesInFile, bibDatabaseContext, filePreferences));
                                addResultToList(file, true, Localization.lang("File was successfully imported as a new entry"));
                            } else {
                                entriesToAdd.add(createEmptyEntryWithLink(file));
                                addResultToList(file, false, Localization.lang("No BibTeX was found. An empty entry was created with file link."));
                            }
                        } else if (FileUtil.isBibFile(file)) {
                            var bibtexParserResult = contentImporter.importFromBibFile(file, fileUpdateMonitor);
                            if (bibtexParserResult.hasWarnings()) {
                                addResultToList(file, false, bibtexParserResult.getErrorMessage());
                            }

                            entriesToAdd.addAll(bibtexParserResult.getDatabaseContext().getEntries());
                            addResultToList(file, true, Localization.lang("Bib entry was successfully imported"));
                        } else {
                            entriesToAdd.add(createEmptyEntryWithLink(file));
                            addResultToList(file, false, Localization.lang("No BibTeX data was found. An empty entry was created with file link."));
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error importing", ex);
                        addResultToList(file, false, Localization.lang("Error from import: %0", ex.getLocalizedMessage()));

                        UiTaskExecutor.runInJavaFXThread(() -> updateMessage(Localization.lang("Error")));
                    }

                    // We need to run the actual import on the FX Thread, otherwise we will get some deadlocks with the UIThreadList
                    UiTaskExecutor.runInJavaFXThread(() -> importEntries(entriesToAdd));

                    ce.addEdit(new UndoableInsertEntries(bibDatabaseContext.getDatabase(), entriesToAdd));
                    ce.end();
                    // prevent fx thread exception in undo manager
                    UiTaskExecutor.runInJavaFXThread(() -> undoManager.addEdit(ce));

                    counter++;
                }
                return results;
            }

            private void addResultToList(Path newFile, boolean success, String logMessage) {
                var result = new ImportFilesResultItemViewModel(newFile, success, logMessage);
                results.add(result);
            }
        };
    }

    private BibEntry createEmptyEntryWithLink(Path file) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, file.getFileName().toString());
        linker.addFilesToEntry(entry, Collections.singletonList(file));
        return entry;
    }

    /**
     * Cleans up the given entries and adds them to the library.
     * There is no automatic download done.
     */
    public void importEntries(List<BibEntry> entries) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(entries);
        importCleanedEntries(entries);
    }

    public void importCleanedEntries(List<BibEntry> entries) {
        entries = entries.stream().map(entry -> (BibEntry) entry.clone()).toList();
        bibDatabaseContext.getDatabase().insertEntries(entries);
        generateKeys(entries);
        setAutomaticFields(entries);
        addToGroups(entries, stateManager.getSelectedGroups(bibDatabaseContext));
    }

    public void importEntryWithDuplicateCheck(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        importEntryWithDuplicateCheck(bibDatabaseContext, entry, BREAK);
    }

    private void importEntryWithDuplicateCheck(BibDatabaseContext bibDatabaseContext, BibEntry entry, DuplicateResolverDialog.DuplicateResolverResult decision) {
        BibEntry entryToInsert = cleanUpEntry(bibDatabaseContext, entry);

        BackgroundTask.wrap(() -> findDuplicate(bibDatabaseContext, entryToInsert))
                      .onFailure(e -> LOGGER.error("Error in duplicate search"))
                      .onSuccess(existingDuplicateInLibrary -> {
                          BibEntry finalEntry = entryToInsert;
                          if (existingDuplicateInLibrary.isPresent()) {
                              Optional<BibEntry> duplicateHandledEntry = handleDuplicates(bibDatabaseContext, entryToInsert, existingDuplicateInLibrary.get(), decision);
                              if (duplicateHandledEntry.isEmpty()) {
                                  return;
                              }
                              finalEntry = duplicateHandledEntry.get();
                          }
                          importCleanedEntries(List.of(finalEntry));
                          downloadLinkedFiles(finalEntry);
                      }).executeWith(taskExecutor);
    }

    @VisibleForTesting
    BibEntry cleanUpEntry(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        ImportCleanup cleanup = ImportCleanup.targeting(bibDatabaseContext.getMode(), preferences.getFieldPreferences());
        return cleanup.doPostCleanup(entry);
    }

    public Optional<BibEntry> findDuplicate(BibDatabaseContext bibDatabaseContext, BibEntry entryToCheck) {
        return new DuplicateCheck(Injector.instantiateModelOrService(BibEntryTypesManager.class))
                .containsDuplicate(bibDatabaseContext.getDatabase(), entryToCheck, bibDatabaseContext.getMode());
    }

    public Optional<BibEntry> handleDuplicates(BibDatabaseContext bibDatabaseContext, BibEntry originalEntry, BibEntry duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult decision) {
        DuplicateDecisionResult decisionResult = getDuplicateDecision(originalEntry, duplicateEntry, decision);
        switch (decisionResult.decision()) {
            case KEEP_RIGHT:
                bibDatabaseContext.getDatabase().removeEntry(duplicateEntry);
                break;
            case KEEP_BOTH:
                break;
            case KEEP_MERGE:
                bibDatabaseContext.getDatabase().removeEntry(duplicateEntry);
                return Optional.of(decisionResult.mergedEntry());
            case KEEP_LEFT:
            case AUTOREMOVE_EXACT:
            case BREAK:
            default:
                return Optional.empty();
        }
        return Optional.of(originalEntry);
    }

    public DuplicateDecisionResult getDuplicateDecision(BibEntry originalEntry, BibEntry duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult decision) {
        DuplicateResolverDialog dialog = new DuplicateResolverDialog(duplicateEntry, originalEntry, DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK, stateManager, dialogService, preferences);
        if (decision == BREAK) {
            decision = dialogService.showCustomDialogAndWait(dialog).orElse(BREAK);
        }
        if (preferences.getMergeDialogPreferences().shouldMergeApplyToAllEntries()) {
            preferences.getMergeDialogPreferences().setAllEntriesDuplicateResolverDecision(decision);
        }
        return new DuplicateDecisionResult(decision, dialog.getMergedEntry());
    }

    public void setAutomaticFields(List<BibEntry> entries) {
        UpdateField.setAutomaticFields(
                entries,
                preferences.getOwnerPreferences(),
                preferences.getTimestampPreferences()
        );
    }

    public void downloadLinkedFiles(BibEntry entry) {
        if (preferences.getFilePreferences().shouldDownloadLinkedFiles()) {
            entry.getFiles().stream()
                 .filter(LinkedFile::isOnlineLink)
                 .forEach(linkedFile ->
                         new LinkedFileViewModel(
                                 linkedFile,
                                 entry,
                                 bibDatabaseContext,
                                 taskExecutor,
                                 dialogService,
                                 preferences
                         ).download(false)
                 );
        }
    }

    private void addToGroups(List<BibEntry> entries, Collection<GroupTreeNode> groups) {
        for (GroupTreeNode node : groups) {
            if (node.getGroup() instanceof GroupEntryChanger entryChanger) {
                List<FieldChange> undo = entryChanger.add(entries);
                // TODO: Add undo
                // if (!undo.isEmpty()) {
                //    ce.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(new GroupTreeNodeViewModel(node),
                //            undo));
                // }
            }
        }
    }

    /**
     * Generate keys for given entries.
     *
     * @param entries entries to generate keys for
     */
    private void generateKeys(List<BibEntry> entries) {
        if (!preferences.getImporterPreferences().isGenerateNewKeyOnImport()) {
            return;
        }
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                bibDatabaseContext.getMetaData().getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences()
                                                                               .getKeyPatterns()),
                bibDatabaseContext.getDatabase(),
                preferences.getCitationKeyPatternPreferences());
        entries.forEach(keyGenerator::generateAndSetKey);
    }

    public List<BibEntry> handleBibTeXData(String entries) {
        BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences(), fileUpdateMonitor);
        try {
            List<BibEntry> result = parser.parseEntries(new ByteArrayInputStream(entries.getBytes(StandardCharsets.UTF_8)));
            Collection<BibtexString> stringConstants = parser.getStringValues();
            importStringConstantsWithDuplicateCheck(stringConstants);
            return result;
        } catch (ParseException ex) {
            LOGGER.error("Could not paste", ex);
            return Collections.emptyList();
        }
    }

    public void importStringConstantsWithDuplicateCheck(Collection<BibtexString> stringConstants) {
        List<String> failures = new ArrayList<>();

        for (BibtexString stringConstantToAdd : stringConstants) {
            try {
                ConstantsItemModel checker = new ConstantsItemModel(stringConstantToAdd.getName(), stringConstantToAdd.getContent());
                if (checker.combinedValidationValidProperty().get()) {
                    bibDatabaseContext.getDatabase().addString(stringConstantToAdd);
                } else {
                    failures.add(Localization.lang("String constant \"%0\" was not imported because it is not a valid string constant", stringConstantToAdd.getName()));
                }
            } catch (KeyCollisionException ex) {
                failures.add(Localization.lang("String constant %0 was not imported because it already exists in this library", stringConstantToAdd.getName()));
            }
        }
        if (!failures.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Importing String constants"), Localization.lang("Could not import the following string constants:\n %0", String.join("\n", failures)));
        }
    }

    public List<BibEntry> handleStringData(String data) throws FetcherException {
        if ((data == null) || data.isEmpty()) {
            return Collections.emptyList();
        }

        if (!CompositeIdFetcher.containsValidId(data)) {
            return tryImportFormats(data);
        }

        CompositeIdFetcher compositeIdFetcher = new CompositeIdFetcher(preferences.getImportFormatPreferences());
        Optional<BibEntry> optional = compositeIdFetcher.performSearchById(data);
        return OptionalUtil.toList(optional);
    }

    private List<BibEntry> tryImportFormats(String data) {
        try {
            ImportFormatReader importFormatReader = new ImportFormatReader(
                    preferences.getImporterPreferences(),
                    preferences.getImportFormatPreferences(),
                    preferences.getCitationKeyPatternPreferences(),
                    fileUpdateMonitor
            );
            UnknownFormatImport unknownFormatImport = importFormatReader.importUnknownFormat(data);
            return unknownFormatImport.parserResult().getDatabase().getEntries();
        } catch (ImportException ex) { // ex is already localized
            dialogService.showErrorDialogAndWait(Localization.lang("Import error"), ex);
            return Collections.emptyList();
        }
    }

    private List<BibEntry> fetchByDOI(DOI doi) throws FetcherException {
        LOGGER.info("Found DOI identifier in clipboard");
        Optional<BibEntry> entry = new DoiFetcher(preferences.getImportFormatPreferences()).performSearchById(doi.getDOI());
        return OptionalUtil.toList(entry);
    }

    private List<BibEntry> fetchByArXiv(ArXivIdentifier arXivIdentifier) throws FetcherException {
        LOGGER.info("Found arxiv identifier in clipboard");
        Optional<BibEntry> entry = new ArXivFetcher(preferences.getImportFormatPreferences()).performSearchById(arXivIdentifier.getNormalizedWithoutVersion());
        return OptionalUtil.toList(entry);
    }

    private List<BibEntry> fetchByISBN(ISBN isbn) throws FetcherException {
        LOGGER.info("Found ISBN identifier in clipboard");
        Optional<BibEntry> entry = new IsbnFetcher(preferences.getImportFormatPreferences()).performSearchById(isbn.getNormalized());
        return OptionalUtil.toList(entry);
    }

    public void importEntriesWithDuplicateCheck(BibDatabaseContext database, List<BibEntry> entriesToAdd) {
        boolean firstEntry = true;
        for (BibEntry entry : entriesToAdd) {
            if (firstEntry) {
                LOGGER.debug("First entry to import, we use BREAK (\"Ask every time\") as decision");
                importEntryWithDuplicateCheck(database, entry, BREAK);
                firstEntry = false;
                continue;
            }
            if (preferences.getMergeDialogPreferences().shouldMergeApplyToAllEntries()) {
                DuplicateResolverDialog.DuplicateResolverResult decision = preferences.getMergeDialogPreferences().getAllEntriesDuplicateResolverDecision();
                LOGGER.debug("Not first entry, pref flag is true, we use {}", decision);
                importEntryWithDuplicateCheck(database, entry, decision);
            } else {
                LOGGER.debug("not first entry, not pref flag, break will  be used");
                importEntryWithDuplicateCheck(database, entry);
            }
        }
    }
}
