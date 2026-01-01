package org.jabref.gui.externalfiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import javafx.scene.input.TransferMode;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.libraryproperties.constants.ConstantsItemModel;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.DragDrop;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.externalfiles.LinkedFileTransferHelper;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.TransferInformation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverResult.BREAK;

// TODO: Much of this code seems to be logic (and not UI)
public class ImportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportHandler.class);
    private final BibDatabaseContext targetBibDatabaseContext;
    private final GuiPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker fileLinker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final FilePreferences filePreferences;

    public ImportHandler(BibDatabaseContext targetBibDatabaseContext,
                         GuiPreferences preferences,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager,
                         DialogService dialogService,
                         TaskExecutor taskExecutor) {
        this.targetBibDatabaseContext = targetBibDatabaseContext;
        this.preferences = preferences;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.filePreferences = preferences.getFilePreferences();

        this.fileLinker = new ExternalFilesEntryLinker(preferences.getExternalApplicationsPreferences(), filePreferences, dialogService, stateManager);
        this.contentImporter = new ExternalFilesContentImporter(preferences.getImportFormatPreferences());
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getFileLinker() {
        return fileLinker;
    }

    public BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesInBackground(final List<Path> files, TransferMode transferMode) {
        // TODO: Make a utility class out of this. Package: org.jabref.logic.externalfiles.
        return new BackgroundTask<>() {
            private int counter;
            private final List<ImportFilesResultItemViewModel> results = new ArrayList<>();
            private final List<BibEntry> allEntriesToAdd = new ArrayList<>();

            @Override
            public List<ImportFilesResultItemViewModel> call() {
                counter = 1;
                CompoundEdit compoundEdit = new CompoundEdit();
                for (final Path file : files) {
                    final List<BibEntry> entriesToAdd = new ArrayList<>();

                    if (isCancelled()) {
                        break;
                    }

                    UiTaskExecutor.runInJavaFXThread(() -> {
                        setTitle(Localization.lang("Importing files into %1 | %2 of %0 file(s) processed.",
                                files.size(),
                                targetBibDatabaseContext.getDatabasePath().map(path -> path.getFileName().toString()).orElse(Localization.lang("untitled")),
                                counter));
                        updateMessage(Localization.lang("Processing %0", FileUtil.shortenFileName(file.getFileName().toString(), 68)));
                        updateProgress(counter, files.size());
                        showToUser(true);
                    });

                    try {
                        if (FileUtil.isPDFFile(file)) {
                            final List<BibEntry> pdfEntriesInFile;

                            // Details: See ADR-0043
                            if (files.size() == 1) {
                                pdfEntriesInFile = new ArrayList<>(1);
                                UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
                                    MultiMergeEntriesView dialog = PdfMergeDialog.createMergeDialog(file, preferences, taskExecutor);
                                    dialogService.showCustomDialogAndWait(dialog).ifPresent(pdfEntriesInFile::add);
                                });
                            } else {
                                ParserResult pdfImporterResult = contentImporter.importPDFContent(file, targetBibDatabaseContext, filePreferences);
                                pdfEntriesInFile = pdfImporterResult.getDatabase().getEntries();
                                if (pdfImporterResult.hasWarnings()) {
                                    addResultToList(file, false, Localization.lang("Error reading PDF content: %0", pdfImporterResult.getErrorMessage()));
                                }
                            }

                            if (pdfEntriesInFile.isEmpty()) {
                                entriesToAdd.add(createEmptyEntryWithLink(file));
                                addResultToList(file, false, Localization.lang("No BibTeX was found. An empty entry was created with file link."));
                            } else {
                                generateKeys(pdfEntriesInFile);
                                pdfEntriesInFile.forEach(entry -> {
                                    if (entry.getFiles().size() > 1) {
                                        LOGGER.warn("Entry has more than one file attached. This is not supported.");
                                        LOGGER.warn("Entry's files: {}", entry.getFiles());
                                    }
                                    entry.clearField(StandardField.FILE);
                                    // Modifiers do not work on macOS: https://bugs.openjdk.org/browse/JDK-8264172
                                    // Similar code as org.jabref.gui.preview.PreviewPanel.PreviewPanel
                                    DragDrop.handleDropOfFiles(List.of(file), transferMode, fileLinker, entry);
                                    addToImportEntriesGroup(pdfEntriesInFile);
                                    entriesToAdd.addAll(pdfEntriesInFile);
                                    addResultToList(file, true, Localization.lang("File was successfully imported as a new entry"));
                                });
                            }
                        } else if (FileUtil.isBibFile(file)) {
                            ParserResult bibtexParserResult = contentImporter.importFromBibFile(file, fileUpdateMonitor);
                            List<BibEntry> entries = bibtexParserResult.getDatabaseContext().getEntries();
                            entriesToAdd.addAll(entries);
                            boolean success = !bibtexParserResult.hasWarnings();
                            String message;
                            if (success) {
                                message = Localization.lang("Bib entry was successfully imported");
                            } else {
                                message = bibtexParserResult.getErrorMessage();
                            }
                            addResultToList(file, success, message);
                        } else {
                            BibEntry emptyEntryWithLink = createEmptyEntryWithLink(file);
                            entriesToAdd.add(emptyEntryWithLink);
                            addResultToList(file, false, Localization.lang("No BibTeX data was found. An empty entry was created with file link."));
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error importing", ex);
                        addResultToList(file, false, Localization.lang("Error from import: %0", ex.getLocalizedMessage()));

                        UiTaskExecutor.runInJavaFXThread(() -> updateMessage(Localization.lang("Error")));
                    }
                    allEntriesToAdd.addAll(entriesToAdd);

                    compoundEdit.addEdit(new UndoableInsertEntries(targetBibDatabaseContext.getDatabase(), entriesToAdd));
                    compoundEdit.end();
                    // prevent fx thread exception in undo manager
                    UiTaskExecutor.runInJavaFXThread(() -> undoManager.addEdit(compoundEdit));

                    counter++;
                }
                // We need to run the actual import on the FX Thread, otherwise we will get some deadlocks with the UIThreadList
                // That method does a clone() on each entry
                UiTaskExecutor.runInJavaFXThread(() -> importEntries(allEntriesToAdd));
                return results;
            }

            private void addResultToList(Path newFile, boolean success, String logMessage) {
                ImportFilesResultItemViewModel result = new ImportFilesResultItemViewModel(newFile, success, logMessage);
                results.add(result);
            }
        };
    }

    private BibEntry createEmptyEntryWithLink(Path file) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, file.getFileName().toString());
        fileLinker.linkFilesToEntry(entry, List.of(file));
        return entry;
    }

    /// Cleans up the given entries and adds them to the library.
    public void importEntries(List<BibEntry> entries) {
        ImportCleanup cleanup = ImportCleanup.targeting(targetBibDatabaseContext.getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(entries);
        importCleanedEntries(null, entries);
    }

    public void importCleanedEntries(@Nullable TransferInformation transferInformation, List<BibEntry> entries) {
        targetBibDatabaseContext.getDatabase().insertEntries(entries);
        generateKeys(entries);
        setAutomaticFields(entries);
        addToGroups(entries, stateManager.getSelectedGroups(targetBibDatabaseContext));
        addToImportEntriesGroup(entries);

        if (transferInformation != null) {
            entries.stream().forEach(entry -> {
                LinkedFileTransferHelper
                        .adjustLinkedFilesForTarget(filePreferences, transferInformation, targetBibDatabaseContext, entry);
            });
        }

        // TODO: Should only be done if NOT copied from other library
        entries.stream().forEach(entry -> downloadLinkedFiles(entry));
    }

    public void importEntryWithDuplicateCheck(@Nullable TransferInformation transferInformation, BibEntry entry) {
        importEntryWithDuplicateCheck(transferInformation, entry, BREAK, new EntryImportHandlerTracker(stateManager));
    }

    /**
     * Imports an entry into the database with duplicate checking and handling.
     * Creates a copy of the entry for processing - the original entry parameter is not modified.
     * The copied entry may be modified during cleanup and duplicate handling.
     *
     * @param entry the entry to import (original will not be modified)
     * @param decision the duplicate resolution strategy to apply
     * @param tracker tracks the import status of the entry
     */
    private void importEntryWithDuplicateCheck(@Nullable TransferInformation transferInformation, BibEntry entry, DuplicateResolverDialog.DuplicateResolverResult decision, EntryImportHandlerTracker tracker) {
        // The original entry should not be modified
        BibEntry entryCopy = new BibEntry(entry);
        BibEntry entryToInsert = cleanUpEntry(entryCopy);

        BackgroundTask.wrap(() -> findDuplicate(entryToInsert))
                      .onFailure(e -> {
                          tracker.markSkipped();
                          LOGGER.error("Error in duplicate search", e);
                      })
                      .onSuccess(existingDuplicateInLibrary -> {
                          BibEntry finalEntry = entryToInsert;
                          if (existingDuplicateInLibrary.isPresent()) {
                              Optional<BibEntry> duplicateHandledEntry = handleDuplicates(entryToInsert, existingDuplicateInLibrary.get(), decision);
                              if (duplicateHandledEntry.isEmpty()) {
                                  tracker.markSkipped();
                                  return;
                              }
                              finalEntry = duplicateHandledEntry.get();
                          }

                          importCleanedEntries(transferInformation, List.of(finalEntry));

                          tracker.markImported(finalEntry);
                      }).executeWith(taskExecutor);
    }

    @VisibleForTesting
    BibEntry cleanUpEntry(BibEntry entry) {
        ImportCleanup cleanup = ImportCleanup.targeting(targetBibDatabaseContext.getMode(), preferences.getFieldPreferences());
        return cleanup.doPostCleanup(entry);
    }

    public Optional<BibEntry> findDuplicate(BibEntry entryToCheck) {
        // FIXME: BibEntryTypesManager needs to be passed via constructor
        return new DuplicateCheck(Injector.instantiateModelOrService(BibEntryTypesManager.class))
                .containsDuplicate(targetBibDatabaseContext.getDatabase(), entryToCheck, targetBibDatabaseContext.getMode());
    }

    public Optional<BibEntry> handleDuplicates(BibEntry originalEntry, BibEntry duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult decision) {
        DuplicateDecisionResult decisionResult = getDuplicateDecision(originalEntry, duplicateEntry, decision);
        switch (decisionResult.decision()) {
            case KEEP_RIGHT:
                targetBibDatabaseContext.getDatabase().removeEntry(duplicateEntry);
                break;
            case KEEP_BOTH:
                break;
            case KEEP_MERGE:
                targetBibDatabaseContext.getDatabase().removeEntry(duplicateEntry);
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
                                 targetBibDatabaseContext,
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
                //    compoundEdit.addEdit(UndoableChangeEntriesOfGroup.getUndoableEdit(new GroupTreeNodeViewModel(node),
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
        if (!preferences.getImporterPreferences().shouldGenerateNewKeyOnImport()) {
            return;
        }
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                targetBibDatabaseContext.getMetaData().getCiteKeyPatterns(preferences.getCitationKeyPatternPreferences()
                                                                                     .getKeyPatterns()),
                targetBibDatabaseContext.getDatabase(),
                preferences.getCitationKeyPatternPreferences());
        entries.forEach(keyGenerator::generateAndSetKey);
    }

    public @NonNull List<@NonNull BibEntry> handleBibTeXData(@NonNull String entries) {
        if (!entries.contains("@")) {
            LOGGER.debug("Seems not to be BibTeX data: {}", entries);
            return List.of();
        }
        BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences(), fileUpdateMonitor);
        try {
            List<BibEntry> result = parser.parseEntries(new ByteArrayInputStream(entries.getBytes(StandardCharsets.UTF_8)));
            Collection<BibtexString> stringConstants = parser.getStringValues();
            importStringConstantsWithDuplicateCheck(stringConstants);
            return result;
        } catch (ParseException ex) {
            LOGGER.info("Data could not be interpreted as Bib(La)TeX", ex);
            dialogService.notify(Localization.lang("Failed to parse Bib(La)TeX: %0", ex.getLocalizedMessage()));
            return List.of();
        }
    }

    public void importStringConstantsWithDuplicateCheck(Collection<BibtexString> stringConstants) {
        List<String> failures = new ArrayList<>();

        for (BibtexString stringConstantToAdd : stringConstants) {
            try {
                ConstantsItemModel checker = new ConstantsItemModel(stringConstantToAdd.getName(), stringConstantToAdd.getContent());
                if (checker.combinedValidationValidProperty().get()) {
                    targetBibDatabaseContext.getDatabase().addString(stringConstantToAdd);
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
            return List.of();
        }
        LOGGER.trace("Checking if URL is a PDF: {}", data);

        if (URLUtil.isURL(data)) {
            String fileName = data.substring(data.lastIndexOf('/') + 1);
            if (FileUtil.isPDFFile(Path.of(fileName))) {
                try {
                    return handlePdfUrl(data);
                } catch (IOException ex) {
                    LOGGER.error("Could not handle PDF URL", ex);
                    return List.of();
                }
            }
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
            return List.of();
        }
    }

    public void importEntriesWithDuplicateCheck(@Nullable TransferInformation transferInformation, List<BibEntry> entriesToAdd) {
        importEntriesWithDuplicateCheck(transferInformation, entriesToAdd, new EntryImportHandlerTracker(stateManager, entriesToAdd.size()));
    }

    public void importEntriesWithDuplicateCheck(@Nullable TransferInformation transferInformation, List<BibEntry> entriesToAdd, EntryImportHandlerTracker tracker) {
        boolean firstEntry = true;
        for (BibEntry entry : entriesToAdd) {
            if (firstEntry) {
                LOGGER.debug("First entry to import, we use BREAK (\"Ask every time\") as decision");
                importEntryWithDuplicateCheck(transferInformation, entry, BREAK, tracker);
                firstEntry = false;
                continue;
            }
            if (preferences.getMergeDialogPreferences().shouldMergeApplyToAllEntries()) {
                DuplicateResolverDialog.DuplicateResolverResult decision = preferences.getMergeDialogPreferences().getAllEntriesDuplicateResolverDecision();
                LOGGER.debug("Not first entry, pref flag is true, we use {}", decision);
                importEntryWithDuplicateCheck(transferInformation, entry, decision, tracker);
            } else {
                LOGGER.debug("not first entry, not pref flag, break will  be used");
                importEntryWithDuplicateCheck(transferInformation, entry, BREAK, tracker);
            }
        }
    }

    private List<BibEntry> handlePdfUrl(String pdfUrl) throws IOException {
        Optional<Path> targetDirectory = targetBibDatabaseContext.getFirstExistingFileDir(preferences.getFilePreferences());
        if (targetDirectory.isEmpty()) {
            LOGGER.warn("File directory not available while downloading {}.", pdfUrl);
            return List.of();
        }
        URLDownload urlDownload = new URLDownload(pdfUrl);
        String filename = URLUtil.getFileNameFromUrl(pdfUrl);
        Path targetFile = targetDirectory.get().resolve(filename);
        try {
            urlDownload.toFile(targetFile);
        } catch (FetcherException fe) {
            LOGGER.error("Error downloading PDF from URL", fe);
            return List.of();
        }
        try {
            PdfMergeMetadataImporter importer = new PdfMergeMetadataImporter(preferences.getImportFormatPreferences());
            ParserResult parserResult = importer.importDatabase(targetFile, targetBibDatabaseContext, preferences.getFilePreferences());
            if (parserResult.hasWarnings()) {
                LOGGER.warn("PDF import had warnings: {}", parserResult.getErrorMessage());
            }
            List<BibEntry> entries = parserResult.getDatabase().getEntries();
            if (!entries.isEmpty()) {
                entries.forEach(entry -> {
                    if (entry.getFiles().isEmpty()) {
                        entry.addFile(new LinkedFile("", targetFile, StandardFileType.PDF.getName()));
                    }
                });
            } else {
                BibEntry emptyEntry = new BibEntry();
                emptyEntry.addFile(new LinkedFile("", targetFile, StandardFileType.PDF.getName()));
                entries.add(emptyEntry);
            }
            return entries;
        } catch (IOException ex) {
            LOGGER.error("Error importing PDF from URL - IO issue", ex);
            return List.of();
        }
    }

    private void addToImportEntriesGroup(List<BibEntry> entriesToInsert) {
        if (preferences.getLibraryPreferences().isAddImportedEntriesEnabled()) {
            String groupName = preferences.getLibraryPreferences().getAddImportedEntriesGroupName();
            // We cannot add the new group here directly because we don't have access to the group node viewmoel stuff here
            // We would need to add the groups to the metadata first which is a bit more complicated, thus we decided against it atm
            this.targetBibDatabaseContext.getMetaData()
                                         .getGroups()
                                         .flatMap(grp -> grp.getChildren()
                                                            .stream()
                                                            .filter(node -> node.getGroup() instanceof ExplicitGroup
                                                                    && node.getGroup().getName().equals(groupName))
                                                            .findFirst())
                                         .ifPresent(importGroup -> importGroup.addEntriesToGroup(entriesToInsert));
        }
    }
}
