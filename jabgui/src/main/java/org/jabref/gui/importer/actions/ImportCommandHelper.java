package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.GrobidUseDialogHelper;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.database.DatabaseMerger;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.UpdateField;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ImportCommandHelper {

    private final CliPreferences preferences;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;

    public ImportCommandHelper(
            CliPreferences preferences,
            DialogService dialogService,
            FileUpdateMonitor fileUpdateMonitor) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
    }

    /// @param importFormat the importer to be used. `null` if not yet known.
    /// @throws IOException of a specified importer
    public ParserResult doImport(List<Path> files, @Nullable Importer importFormat) throws IOException {
        Optional<Importer> importer = Optional.ofNullable(importFormat);
        // We import all files and collect their results
        List<ImportFormatReader.UnknownFormatImport> imports = new ArrayList<>();
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                fileUpdateMonitor
        );
        for (Path filename : files) {
            try {
                if (importer.isEmpty()) {
                    // Unknown format
                    UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
                        if (FileUtil.isPDFFile(filename) && GrobidUseDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences())) {
                            importFormatReader.reset();
                        }
                        dialogService.notify(Localization.lang("Importing file %0 as unknown format", filename.getFileName().toString()));
                    });
                    // This import method never throws an IOException
                    imports.add(importFormatReader.importUnknownFormat(filename, fileUpdateMonitor));
                } else {
                    UiTaskExecutor.runAndWaitInJavaFXThread(() -> {
                        if (((importer.get() instanceof PdfGrobidImporter) || (importer.get() instanceof PdfMergeMetadataImporter))
                                && GrobidUseDialogHelper.showAndWaitIfUserIsUndecided(dialogService, preferences.getGrobidPreferences())) {
                            importFormatReader.reset();
                        }
                        dialogService.notify(Localization.lang("Importing in %0 format", importer.get().getName()) + "...");
                    });
                    // Specific importer
                    ParserResult pr = importer.get().importDatabase(filename);
                    imports.add(new ImportFormatReader.UnknownFormatImport(importer.get().getName(), pr));
                }
            } catch (ImportException ex) {
                UiTaskExecutor.runAndWaitInJavaFXThread(
                        () -> dialogService.showWarningDialogAndWait(
                                Localization.lang("Import error"),
                                Localization.lang("Please check your library file for wrong syntax.")
                                        + "\n\n"
                                        + ex.getLocalizedMessage()));
            }
        }

        if (imports.isEmpty()) {
            UiTaskExecutor.runAndWaitInJavaFXThread(
                    () -> dialogService.showWarningDialogAndWait(
                            Localization.lang("Import error"),
                            Localization.lang("No entries found. Please make sure you are using the correct import filter.")));

            return new ParserResult();
        }

        return mergeImportResults(imports);
    }

    public void importIntoContext(BibDatabaseContext context, List<Path> files, @Nullable Importer importFormat) throws IOException {
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> (doImport(files, importFormat)));
        ImportEntriesDialog dialog = new ImportEntriesDialog(context, task);
        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }

    /// TODO: Move this to logic package. Blocked by undo functionality.
    private ParserResult mergeImportResults(List<ImportFormatReader.UnknownFormatImport> imports) {
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
                new DatabaseMerger(preferences.getBibEntryPreferences().getKeywordSeparator()).mergeMetaData(
                        result.getMetaData(),
                        parserResult.getMetaData(),
                        importResult.parserResult().getPath().map(path -> path.getFileName().toString()).orElse("unknown"),
                        parserResult.getDatabase().getEntries());
            }
            // TODO: collect errors into ParserResult, because they are currently ignored (see caller of this method)
        }

        // set timestamp and owner
        UpdateField.setAutomaticFields(resultDatabase.getEntries(), preferences.getOwnerPreferences(), preferences.getTimestampPreferences()); // set timestamp and owner

        return result;
    }
}
