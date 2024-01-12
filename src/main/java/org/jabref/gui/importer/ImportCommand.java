package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * Perform an import action
 */
public class ImportCommand extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCommand.class);

    public enum ImportMethod { AS_NEW, TO_EXISTING }

    private final LibraryTabContainer tabContainer;
    private final ImportMethod importMethod;

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    public ImportCommand(LibraryTabContainer tabContainer,
                         ImportMethod importMethod,
                         PreferencesService preferencesService,
                         StateManager stateManager,
                         FileUpdateMonitor fileUpdateMonitor,
                         TaskExecutor taskExecutor,
                         DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.importMethod = importMethod;
        this.preferencesService = preferencesService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        if (importMethod == ImportMethod.TO_EXISTING) {
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
                     .ifPresent(path -> importSingleFile(path, importers, fileDialogConfiguration.getSelectedExtensionFilter()));
    }

    private void importSingleFile(Path file, SortedSet<Importer> importers, FileChooser.ExtensionFilter selectedExtensionFilter) {
        if (!Files.exists(file)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Import"),
                    Localization.lang("File not found") + ": '" + file.getFileName() + "'.");

            return;
        }

        Optional<Importer> format = FileFilterConverter.getImporter(selectedExtensionFilter, importers);
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(
                () -> doImport(Collections.singletonList(file), format.orElse(null)));

        if (importMethod == ImportMethod.AS_NEW) {
            task.onSuccess(parserResult -> {
                    tabContainer.addTab(parserResult.getDatabaseContext(), true);
                    dialogService.notify(Localization.lang("Imported entries") + ": " + parserResult.getDatabase().getEntries().size());
                })
                .onFailure(ex -> {
                    LOGGER.error("Error importing", ex);
                    dialogService.notify(Localization.lang("Error importing. See the error log for details."));
                })
                .executeWith(taskExecutor);
        } else {
            ImportEntriesDialog dialog = new ImportEntriesDialog(tabContainer.getCurrentLibraryTab().getBibDatabaseContext(), task);
            dialog.setTitle(Localization.lang("Import"));
            dialogService.showCustomDialogAndWait(dialog);
        }

        // Set last working dir for import
        preferencesService.getImporterPreferences().setImportWorkingDirectory(file.getParent());
    }

    /**
     * @throws IOException of a specified importer
     */
    private ParserResult doImport(List<Path> files, Importer importFormat) throws IOException {
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
                        if (FileUtil.isPDFFile(filename) && GrobidOptInDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferencesService.getGrobidPreferences())) {
                            importFormatReader.reset();
                        }
                        dialogService.notify(Localization.lang("Importing file %0 as unknown format", filename.getFileName().toString()));
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
            } catch (ImportException ex) {
                DefaultTaskExecutor.runAndWaitInJavaFXThread(
                        () -> dialogService.showWarningDialogAndWait(
                                Localization.lang("Import error"),
                                Localization.lang("Please check your library file for wrong syntax.")
                                + "\n\n"
                                + ex.getLocalizedMessage()));
            }
        }

        if (imports.isEmpty()) {
            DefaultTaskExecutor.runAndWaitInJavaFXThread(
                    () -> dialogService.showWarningDialogAndWait(
                            Localization.lang("Import error"),
                            Localization.lang("No entries found. Please make sure you are using the correct import filter.")));

            return new ParserResult();
        }

        return mergeImportResults(imports);
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
}
