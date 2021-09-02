package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportHandler.class);
    private final BibDatabaseContext bibdatabase;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;

    public ImportHandler(BibDatabaseContext database,
                         ExternalFileTypes externalFileTypes,
                         PreferencesService preferencesService,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager) {

        this.bibdatabase = database;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;

        this.linker = new ExternalFilesEntryLinker(externalFileTypes, preferencesService.getFilePreferences(), database);
        this.contentImporter = new ExternalFilesContentImporter(
                preferencesService.getGeneralPreferences(),
                preferencesService.getImporterPreferences(),
                preferencesService.getImportFormatPreferences());
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getLinker() {
        return linker;
    }

    public BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesInBackground(List<Path> files) {
        return new BackgroundTask<>() {
            private int counter;
            private List<BibEntry> entriesToAdd;
            private final List<ImportFilesResultItemViewModel> results = new ArrayList<>();

            @Override
            protected List<ImportFilesResultItemViewModel> call() {
                counter = 1;
                CompoundEdit ce = new CompoundEdit();
                for (Path file: files) {
                    entriesToAdd = Collections.emptyList();

                    if (isCanceled()) {
                        break;
                    }

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        updateMessage(Localization.lang("Processing file %0", file.getFileName()));
                        updateProgress(counter, files.size() - 1);
                    });

                    try {
                        if (FileUtil.isPDFFile(file)) {
                            var pdfImporterResult = contentImporter.importPDFContent(file);
                            List<BibEntry> pdfEntriesInFile = pdfImporterResult.getDatabase().getEntries();

                            if (pdfImporterResult.hasWarnings()) {
                                addResultToList(file, false, Localization.lang("Error reading PDF content: %0", pdfImporterResult.getErrorMessage()));
                            }

                            if (!pdfEntriesInFile.isEmpty()) {
                                entriesToAdd = pdfEntriesInFile;
                                addResultToList(file, true, Localization.lang("Importing using extracted PDF data"));
                            } else {
                                entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
                                addResultToList(file, false, Localization.lang("No metadata found. Creating empty entry with file link"));
                            }
                        } else if (FileUtil.isBibFile(file)) {
                            var bibtexParserResult = contentImporter.importFromBibFile(file, fileUpdateMonitor);
                            if (bibtexParserResult.hasWarnings()) {
                                addResultToList(file, false, bibtexParserResult.getErrorMessage());
                            }

                            entriesToAdd = bibtexParserResult.getDatabaseContext().getEntries();
                            addResultToList(file, false, Localization.lang("Importing bib entry"));
                        } else {
                            entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
                            addResultToList(file, false, Localization.lang("No BibTeX data found. Creating empty entry with file link"));
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error importing", ex);
                        addResultToList(file, false, Localization.lang("Error from import: %0", ex.getLocalizedMessage()));

                        DefaultTaskExecutor.runInJavaFXThread(() -> updateMessage(Localization.lang("Error")));
                    }

                    // We need to run the actual import on the FX Thread, otherwise we will get some deadlocks with the UIThreadList
                    DefaultTaskExecutor.runInJavaFXThread(() -> importEntries(entriesToAdd));

                    ce.addEdit(new UndoableInsertEntries(bibdatabase.getDatabase(), entriesToAdd));
                    ce.end();
                    undoManager.addEdit(ce);

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

    public void importEntries(List<BibEntry> entries) {
        ImportCleanup cleanup = new ImportCleanup(bibdatabase.getMode());
        cleanup.doPostCleanup(entries);
        bibdatabase.getDatabase().insertEntries(entries);

        // Set owner/timestamp
        UpdateField.setAutomaticFields(entries,
                preferencesService.getOwnerPreferences(),
                preferencesService.getTimestampPreferences());

        // Generate citation keys
        if (preferencesService.getImporterPreferences().isGenerateNewKeyOnImport()) {
            generateKeys(entries);
        }

        // Add to group
        addToGroups(entries, stateManager.getSelectedGroup(bibdatabase));
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
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                bibdatabase.getMetaData().getCiteKeyPattern(preferencesService.getCitationKeyPatternPreferences()
                                                                              .getKeyPattern()),
                bibdatabase.getDatabase(),
                preferencesService.getCitationKeyPatternPreferences());

        for (BibEntry entry : entries) {
            keyGenerator.generateAndSetKey(entry);
        }
    }
}
