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
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.externalfiles.ExternalFilesContentImporter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.ImportFormatReader.UnknownFormatImport;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportHandler.class);
    private final BibDatabaseContext bibDatabaseContext;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final ExternalFilesEntryLinker linker;
    private final ExternalFilesContentImporter contentImporter;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final ImportFormatReader importFormatReader;

    public ImportHandler(BibDatabaseContext database,
                         PreferencesService preferencesService,
                         FileUpdateMonitor fileupdateMonitor,
                         UndoManager undoManager,
                         StateManager stateManager,
                         DialogService dialogService,
                         ImportFormatReader importFormatReader) {

        this.bibDatabaseContext = database;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileupdateMonitor;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.importFormatReader = importFormatReader;

        this.linker = new ExternalFilesEntryLinker(preferencesService.getFilePreferences(), database);
        this.contentImporter = new ExternalFilesContentImporter(preferencesService.getImportFormatPreferences());
        this.undoManager = undoManager;
    }

    public ExternalFilesEntryLinker getLinker() {
        return linker;
    }

    public BackgroundTask<List<ImportFilesResultItemViewModel>> importFilesInBackground(final List<Path> files) {
        return new BackgroundTask<>() {
            private int counter;
            private final List<ImportFilesResultItemViewModel> results = new ArrayList<>();

            @Override
            protected List<ImportFilesResultItemViewModel> call() {
                counter = 1;
                CompoundEdit ce = new CompoundEdit();
                for (final Path file : files) {
                    final List<BibEntry> entriesToAdd = new ArrayList<>();

                    if (isCanceled()) {
                        break;
                    }

                    DefaultTaskExecutor.runInJavaFXThread(() -> {
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
                                entriesToAdd.addAll(pdfEntriesInFile);
                                addResultToList(file, true, Localization.lang("Importing using extracted PDF data"));
                            } else {
                                entriesToAdd.add(createEmptyEntryWithLink(file));
                                addResultToList(file, false, Localization.lang("No metadata found. Creating empty entry with file link"));
                            }
                        } else if (FileUtil.isBibFile(file)) {
                            var bibtexParserResult = contentImporter.importFromBibFile(file, fileUpdateMonitor);
                            if (bibtexParserResult.hasWarnings()) {
                                addResultToList(file, false, bibtexParserResult.getErrorMessage());
                            }

                            entriesToAdd.addAll(bibtexParserResult.getDatabaseContext().getEntries());
                            addResultToList(file, false, Localization.lang("Importing bib entry"));
                        } else {
                            entriesToAdd.add(createEmptyEntryWithLink(file));
                            addResultToList(file, false, Localization.lang("No BibTeX data found. Creating empty entry with file link"));
                        }
                    } catch (IOException ex) {
                        LOGGER.error("Error importing", ex);
                        addResultToList(file, false, Localization.lang("Error from import: %0", ex.getLocalizedMessage()));

                        DefaultTaskExecutor.runInJavaFXThread(() -> updateMessage(Localization.lang("Error")));
                    }

                    // We need to run the actual import on the FX Thread, otherwise we will get some deadlocks with the UIThreadList
                    DefaultTaskExecutor.runInJavaFXThread(() -> importEntries(entriesToAdd));

                    ce.addEdit(new UndoableInsertEntries(bibDatabaseContext.getDatabase(), entriesToAdd));
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
        ImportCleanup cleanup = new ImportCleanup(bibDatabaseContext.getMode());
        cleanup.doPostCleanup(entries);
        bibDatabaseContext.getDatabase().insertEntries(entries);

        // Set owner/timestamp
        UpdateField.setAutomaticFields(entries,
                preferencesService.getOwnerPreferences(),
                preferencesService.getTimestampPreferences());

        // Generate citation keys
        if (preferencesService.getImporterPreferences().isGenerateNewKeyOnImport()) {
            generateKeys(entries);
        }

        // Add to group
        addToGroups(entries, stateManager.getSelectedGroup(bibDatabaseContext));
    }

    public void importEntryWithDuplicateCheck(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        ImportCleanup cleanup = new ImportCleanup(bibDatabaseContext.getMode());
        BibEntry cleanedEntry = cleanup.doPostCleanup(entry);
        BibEntry entryToInsert = cleanedEntry;

        Optional<BibEntry> existingDuplicateInLibrary = new DuplicateCheck(Globals.entryTypesManager).containsDuplicate(bibDatabaseContext.getDatabase(), entryToInsert, bibDatabaseContext.getMode());
        if (existingDuplicateInLibrary.isPresent()) {
            DuplicateResolverDialog dialog = new DuplicateResolverDialog(existingDuplicateInLibrary.get(), entryToInsert, DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK, bibDatabaseContext, stateManager, dialogService);
            switch (dialogService.showCustomDialogAndWait(dialog).orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK)) {
                case KEEP_LEFT:
                    bibDatabaseContext.getDatabase().removeEntry(existingDuplicateInLibrary.get());
                    break;
                case KEEP_BOTH:
                    break;
                case KEEP_MERGE:
                    bibDatabaseContext.getDatabase().removeEntry(existingDuplicateInLibrary.get());
                    entryToInsert = dialog.getMergedEntry();
                    break;
                case KEEP_RIGHT:
                case AUTOREMOVE_EXACT:
                case BREAK:
                default:
                   return;
            }
        }
        // Regenerate CiteKey of imported BibEntry
        if (preferencesService.getImporterPreferences().isGenerateNewKeyOnImport()) {
            generateKeys(List.of(entryToInsert));
        }
        bibDatabaseContext.getDatabase().insertEntry(entryToInsert);

        // Set owner/timestamp
        UpdateField.setAutomaticFields(List.of(entryToInsert),
                                       preferencesService.getOwnerPreferences(),
                                       preferencesService.getTimestampPreferences());

        addToGroups(List.of(entry), stateManager.getSelectedGroup(this.bibDatabaseContext));
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
                bibDatabaseContext.getMetaData().getCiteKeyPattern(preferencesService.getCitationKeyPatternPreferences()
                                                                                     .getKeyPattern()),
                bibDatabaseContext.getDatabase(),
                preferencesService.getCitationKeyPatternPreferences());

        for (BibEntry entry : entries) {
            keyGenerator.generateAndSetKey(entry);
        }
    }

    public List<BibEntry> handleBibTeXData(String entries) {
        BibtexParser parser = new BibtexParser(preferencesService.getImportFormatPreferences(), Globals.getFileUpdateMonitor());
        try {
            return parser.parseEntries(new ByteArrayInputStream(entries.getBytes(StandardCharsets.UTF_8)));
        } catch (ParseException ex) {
            LOGGER.error("Could not paste", ex);
            return Collections.emptyList();
        }
    }

    public List<BibEntry> handleStringData(String data) throws FetcherException {
        if ((data == null) || data.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<DOI> doi = DOI.parse(data);
        if (doi.isPresent()) {
            return fetchByDOI(doi.get());
        }
        Optional<ArXivIdentifier> arXiv = ArXivIdentifier.parse(data);
        if (arXiv.isPresent()) {
            return fetchByArXiv(arXiv.get());
        }

        return tryImportFormats(data);
    }

    private List<BibEntry> tryImportFormats(String data) {
        try {
            UnknownFormatImport unknownFormatImport = importFormatReader.importUnknownFormat(data);
            return unknownFormatImport.parserResult.getDatabase().getEntries();
        } catch (ImportException ignored) {
            return Collections.emptyList();
        }
    }

    private List<BibEntry> fetchByDOI(DOI doi) throws FetcherException {
        LOGGER.info("Found DOI in clipboard");
        Optional<BibEntry> entry = new DoiFetcher(preferencesService.getImportFormatPreferences()).performSearchById(doi.getDOI());
        return OptionalUtil.toList(entry);
    }

    private List<BibEntry> fetchByArXiv(ArXivIdentifier arXivIdentifier) throws FetcherException {
        LOGGER.info("Found arxiv identifier in clipboard");
        Optional<BibEntry> entry = new ArXiv(preferencesService.getImportFormatPreferences()).performSearchById(arXivIdentifier.getNormalizedWithoutVersion());
        return OptionalUtil.toList(entry);
    }
}
