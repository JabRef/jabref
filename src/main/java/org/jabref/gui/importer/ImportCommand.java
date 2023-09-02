package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.JabRefException;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * Perform import operation
 */
public class ImportCommand extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCommand.class);

    private final JabRefFrame frame;
    private final boolean openInNew;

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    private Exception importError;

    /**
     * @param openInNew Indicate whether the entries should import into a new database or into the currently open one.
     */
    public ImportCommand(JabRefFrame frame,
                         boolean openInNew,
                         PreferencesService preferencesService,
                         StateManager stateManager,
                         FileUpdateMonitor fileUpdateMonitor,
                         TaskExecutor taskExecutor,
                         DialogService dialogService) {
        this.frame = frame;
        this.openInNew = openInNew;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        if (!openInNew) {
            this.executable.bind(needsDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferencesService.getImporterPreferences(),
                preferencesService.getImportFormatPreferences(),
                fileUpdateMonitor);
        SortedSet<Importer> importers = importFormatReader.getImportFormats();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.ANY_FILE)
                .addExtensionFilter(FileFilterConverter.forAllImporters(importers))
                .addExtensionFilter(FileFilterConverter.importerToExtensionFilter(importers))
                .withInitialDirectory(preferencesService.getImporterPreferences().getImportWorkingDirectory())
                .build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(path -> automatedImport(path, importers, fileDialogConfiguration.getSelectedExtensionFilter()));
    }

    private void automatedImport(Path file, SortedSet<Importer> importers, FileChooser.ExtensionFilter selectedExtensionFilter) {
        if (!Files.exists(file)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Import"),
                    Localization.lang("File not found") + ": '" + file.getFileName() + "'.");

            return;
        }
        Optional<Importer> format = FileFilterConverter.getImporter(selectedExtensionFilter, importers);

        List<String> filenames = Collections.singletonList(file.toString());

        ////////////////

        List<Path> files = filenames.stream().map(Path::of).collect(Collectors.toList());
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> {
            List<ImportFormatReader.UnknownFormatImport> imports = doImport(files, format.orElse(null));
            // Ok, done. Then try to gather in all we have found. Since we might
            // have found
            // one or more bibtex results, it's best to gather them in a
            // BibDatabase.
            ParserResult bibtexResult = mergeImportResults(imports);

            // TODO: show parserwarnings, if any (not here)
            // for (ImportFormatReader.UnknownFormatImport p : imports) {
            //    ParserResultWarningDialog.showParserResultWarningDialog(p.parserResult, frame);
            // }
            if (bibtexResult.isEmpty()) {
                if (importError == null) {
                    // TODO: No control flow using exceptions
                    throw new JabRefException(Localization.lang("No entries found. Please make sure you are using the correct import filter."));
                } else if (importError instanceof ImportException) {
                    String content = Localization.lang("Please check your library file for wrong syntax.") + "\n\n"
                            + importError.getLocalizedMessage();
                    DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> dialogService.showWarningDialogAndWait(Localization.lang("Import error"), content));
                } else {
                    throw importError;
                }
            }

            return bibtexResult;
        });

        if (openInNew) {
            task.onSuccess(parserResult -> {
                    frame.addTab(parserResult.getDatabaseContext(), true);
                    dialogService.notify(Localization.lang("Imported entries") + ": " + parserResult.getDatabase().getEntries().size());
                })
                .onFailure(ex -> {
                    LOGGER.error("Error importing", ex);
                    dialogService.notify(Localization.lang("Error importing. See the error log for details."));
                })
                .executeWith(taskExecutor);
        } else {
            final LibraryTab libraryTab = frame.getCurrentLibraryTab();

            ImportEntriesDialog dialog = new ImportEntriesDialog(libraryTab.getBibDatabaseContext(), task);
            dialog.setTitle(Localization.lang("Import"));
            dialogService.showCustomDialogAndWait(dialog);
        }

        ///////////////////////

        // Set last working dir for import
        preferencesService.getImporterPreferences().setImportWorkingDirectory(file.getParent());
    }

    public List<ImportFormatReader.UnknownFormatImport> doImport(List<Path> files, Importer importFormat) {
        Optional<Importer> importer = Optional.ofNullable(importFormat);
        // We import all files and collect their results
        List<ImportFormatReader.UnknownFormatImport> imports = new ArrayList<>();
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferencesService.getImporterPreferences(),
                preferencesService.getImportFormatPreferences(),
                fileUpdateMonitor);
        for (Path filename : files) {
            try {
                if (importer.isEmpty()) {
                    // Unknown format
                    DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> {
                        if (fileIsPdf(filename) && GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferencesService.getGrobidPreferences())) {
                            importFormatReader.reset();
                        }
                        dialogService.notify(Localization.lang("Importing in unknown format") + "...");
                    });
                    // This import method never throws an IOException
                    imports.add(importFormatReader.importUnknownFormat(filename, fileUpdateMonitor));
                } else {
                    DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> {
                        if (((importer.get() instanceof PdfGrobidImporter)
                                || (importer.get() instanceof PdfMergeMetadataImporter))
                                && GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferencesService.getGrobidPreferences())) {
                            importFormatReader.reset();
                        }
                        dialogService.notify(Localization.lang("Importing in %0 format", importer.get().getName()) + "...");
                    });
                    // Specific importer
                    ParserResult pr = importer.get().importDatabase(filename);
                    imports.add(new ImportFormatReader.UnknownFormatImport(importer.get().getName(), pr));
                }
            } catch (ImportException |
                     IOException e) {
                // This indicates that a specific importer was specified, and that
                // this importer has thrown an IOException. We store the exception,
                // so a relevant error message can be displayed.
                importError = e;
            }
        }
        return imports;
    }

    /**
     * TODO: Move this to logic package. Blocked by undo functionality.
     */
    public ParserResult mergeImportResults(List<ImportFormatReader.UnknownFormatImport> imports) {
        BibDatabase resultDatabase = new BibDatabase();
        ParserResult result = new ParserResult(resultDatabase);

        for (ImportFormatReader.UnknownFormatImport importResult : imports) {
            if (importResult == null) {
                continue;
            }
            ParserResult parserResult = importResult.parserResult();
            resultDatabase.insertEntries(parserResult.getDatabase().getEntries());

            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format())) {
                // additional treatment of BibTeX
                new DatabaseMerger(preferencesService.getBibEntryPreferences().getKeywordSeparator()).mergeMetaData(
                        result.getMetaData(),
                        parserResult.getMetaData(),
                        importResult.parserResult().getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                        parserResult.getDatabase().getEntries());
            }
            // TODO: collect errors into ParserResult, because they are currently ignored (see caller of this method)
        }

        // set timestamp and owner
        UpdateField.setAutomaticFields(resultDatabase.getEntries(), preferencesService.getOwnerPreferences(), preferencesService.getTimestampPreferences()); // set timestamp and owner

        return result;
    }

    private boolean fileIsPdf(Path filename) {
        Optional<String> extension = FileUtil.getFileExtension(filename);
        return extension.isPresent() && StandardFileType.PDF.getExtensions().contains(extension.get());
    }
}
