package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
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
    private final BibDatabaseContext bibdatabasecontext;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private List<ImportFilesResultItemViewModel> results;
    private int counter;
    private List<BibEntry> entriesToAdd;

    public ImportHandler(DialogService dialogService,
                         BibDatabaseContext database,
                         ExternalFileTypes externalFileTypes,
                         PreferencesService preferencesService,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager) {

        this.bibdatabasecontext = database;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;

        this.linker = new ExternalFilesEntryLinker(externalFileTypes, preferencesService.getFilePreferences(), database);
        this.contentImporter = new ExternalFilesContentImporter(preferencesService.getImportFormatPreferences());
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getLinker() {
        return linker;
    }

    public BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesInBackground(List<Path> files) {
        return new BackgroundTask<>() {

            @Override
            protected List<ImportFilesResultItemViewModel> call() throws Exception {
                results = new ArrayList<>();
                counter = 1;
                CompoundEdit ce = new CompoundEdit();
                for (int i = 0; i < files.size(); i++) {

                    if (isCanceled()) {
                        break;
                    }
                    DefaultTaskExecutor.runInJavaFXThread(() -> {
                        updateMessage(Localization.lang("Processing file %0 of %1", counter, files.size()));
                        updateProgress(counter, files.size() - 1);
                    });

                    var file = files.get(0);
                    entriesToAdd = Collections.emptyList();

                    try {
                        if (FileUtil.getFileExtension(file).filter("pdf"::equals).isPresent()) {

                            var pdfImporterResult = contentImporter.importPDFContent(file);
                            List<BibEntry> pdfEntriesInFile = pdfImporterResult.getDatabase().getEntries();

                            if (pdfImporterResult.hasWarnings()) {
                                addResultToList(file, false, "Error reading PDF content: " + pdfImporterResult.getErrorMessage());
                            }

                            var xmpParserResult = contentImporter.importXMPContent(file);
                            List<BibEntry> xmpEntriesInFile = xmpParserResult.getDatabase().getEntries();

                            if (xmpParserResult.hasWarnings()) {
                                addResultToList(file, false, "Error reading XMP content: " + xmpParserResult.getErrorMessage());
                            }

                            // First try xmp import, if empty try pdf import, otherwise create empty entry
                            if (!xmpEntriesInFile.isEmpty()) {
                                if (!pdfEntriesInFile.isEmpty()) {
                                    // FIXME: Show merge dialog?
                                    entriesToAdd = xmpEntriesInFile;
                                } else {
                                    entriesToAdd = xmpEntriesInFile;
                                }
                                addResultToList(file, true, "Importing using XMP data");
                            } else {
                                if (!pdfEntriesInFile.isEmpty()) {
                                    entriesToAdd = pdfEntriesInFile;
                                    addResultToList(file, true, "Importing using extracted PDF data");
                                } else {
                                    addResultToList(file, false, "No entry found. Creating empty entry with file link");
                                    entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
                                }
                            }
                        } else if (FileUtil.isBibFile(file)) {
                            var bibtexParserResult = contentImporter.importFromBibFile(file, fileUpdateMonitor);
                            if (bibtexParserResult.hasWarnings()) {
                                addResultToList(file, false, bibtexParserResult.getErrorMessage());
                            }

                            addResultToList(file, false, "Importing bib entry");
                            entriesToAdd = bibtexParserResult.getDatabaseContext().getEntries();

                        } else {
                            addResultToList(file, false, "Importing bib entry");
                            addResultToList(file, false, "No entry found. Creating empty entry with file link");
                            entriesToAdd = Collections.singletonList(createEmptyEntryWithLink(file));
                        }

                    } catch (IOException ex) {
                        LOGGER.error("Error importing", ex);
                        addResultToList(file, false, "Error importing " + ex.getLocalizedMessage());

                        DefaultTaskExecutor.runInJavaFXThread(() -> updateMessage("Error"));
                    }

                    // We need to run the actual import on the FX Thread, otherwise we will get some deadlocks with the UIThreadList
                    DefaultTaskExecutor.runInJavaFXThread(() -> importEntries(entriesToAdd));

                    ce.addEdit(new UndoableInsertEntries(bibdatabasecontext.getDatabase(), entriesToAdd));
                    ce.end();
                    undoManager.addEdit(ce);

                    counter++;
                }
                return results;
            }
        };
    }

    private void addResultToList(Path newFile, boolean success, String logMessage) {
        var result = new ImportFilesResultItemViewModel(newFile, success, logMessage);
        results.add(result);
    }

    public void importAsNewEntries(List<Path> files) {
        // Will be replaced
    }

    private BibEntry createEmptyEntryWithLink(Path file) {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, file.getFileName().toString());
        linker.addFilesToEntry(entry, Collections.singletonList(file));
        return entry;
    }

    public void importEntries(List<BibEntry> entries) {
        ImportCleanup cleanup = new ImportCleanup(bibdatabasecontext.getMode());
        cleanup.doPostCleanup(entries);
        bibdatabasecontext.getDatabase().insertEntries(entries);

        // Set owner/timestamp
        UpdateField.setAutomaticFields(entries,
                                       preferencesService.getOwnerPreferences(),
                                       preferencesService.getTimestampPreferences());

        // Generate citation keys
        generateKeys(entries);

        // Add to group
        addToGroups(entries, stateManager.getSelectedGroup(bibdatabasecontext));
    }

    private void addToGroups(List<BibEntry> entries, Collection<GroupTreeNode> groups) {
        for (GroupTreeNode node : groups) {
            if (node.getGroup() instanceof GroupEntryChanger) {
                GroupEntryChanger entryChanger = (GroupEntryChanger) node.getGroup();
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
                                                                     bibdatabasecontext.getMetaData().getCiteKeyPattern(Globals.prefs.getCitationKeyPatternPreferences().getKeyPattern()),
                                                                     bibdatabasecontext.getDatabase(),
                                                                     Globals.prefs.getCitationKeyPatternPreferences());

        for (BibEntry entry : entries) {
            keyGenerator.generateAndSetKey(entry);
        }
    }
}
