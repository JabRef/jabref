package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;

public class ImportAction {

    private final JabRefFrame frame;
    private final boolean openInNew;
    private final Optional<Importer> importer;
    private final DialogService dialogService;
    private Exception importError;
    private final TaskExecutor taskExecutor = Globals.TASK_EXECUTOR;

    public ImportAction(JabRefFrame frame, boolean openInNew, Importer importer) {
        this.importer = Optional.ofNullable(importer);
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.openInNew = openInNew;
    }

    /**
     * Automatically imports the files given as arguments.
     * @param filenames List of files to import
     */
    public void automatedImport(List<String> filenames) {
        List<Path> files = filenames.stream().map(Paths::get).collect(Collectors.toList());
        BackgroundTask<List<BibEntry>> task = BackgroundTask.wrap(() -> {
            List<ImportFormatReader.UnknownFormatImport> imports = doImport(files);
            // Ok, done. Then try to gather in all we have found. Since we might
            // have found
            // one or more bibtex results, it's best to gather them in a
            // BibDatabase.
            ParserResult bibtexResult = mergeImportResults(imports);

            // TODO: show parserwarnings, if any (not here)
            // for (ImportFormatReader.UnknownFormatImport p : imports) {
            //    ParserResultWarningDialog.showParserResultWarningDialog(p.parserResult, frame);
            //}
            if (bibtexResult == null) {
                if (importError == null) {
                    throw new JabRefException(Localization.lang("No entries found. Please make sure you are using the correct import filter."));
                } else {
                    throw importError;
                }
            }

            return bibtexResult.getDatabase().getEntries();
        });

        if (openInNew) {
            task.onSuccess(entries -> {
                frame.addTab(new BibDatabaseContext(new BibDatabase(entries)), true);
                dialogService.notify(Localization.lang("Imported entries") + ": " + entries.size());
            })
                .executeWith(taskExecutor);
        } else {
            final BasePanel panel = frame.getCurrentBasePanel();

            ImportEntriesDialog dialog = new ImportEntriesDialog(panel.getBibDatabaseContext(), task);
            dialog.setTitle(Localization.lang("Import"));
            dialog.showAndWait();
        }
    }

    private List<ImportFormatReader.UnknownFormatImport> doImport(List<Path> files) {
        // We import all files and collect their results:
        List<ImportFormatReader.UnknownFormatImport> imports = new ArrayList<>();
        for (Path filename : files) {
            try {
                if (!importer.isPresent()) {
                    // Unknown format:
                    DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().notify(Localization.lang("Importing in unknown format") + "..."));
                    // This import method never throws an IOException:
                    imports.add(Globals.IMPORT_FORMAT_READER.importUnknownFormat(filename, Globals.getFileUpdateMonitor()));
                } else {
                    DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().notify(Localization.lang("Importing in %0 format", importer.get().getName()) + "..."));
                    // Specific importer:
                    ParserResult pr = importer.get().importDatabase(filename, Globals.prefs.getDefaultEncoding());
                    imports.add(new ImportFormatReader.UnknownFormatImport(importer.get().getName(), pr));
                }
            } catch (ImportException | IOException e) {
                // This indicates that a specific importer was specified, and that
                // this importer has thrown an IOException. We store the exception,
                // so a relevant error message can be displayed.
                importError = e;
            }
        }
        return imports;
    }

    private ParserResult mergeImportResults(List<ImportFormatReader.UnknownFormatImport> imports) {
        BibDatabase database = new BibDatabase();
        ParserResult directParserResult = null;
        boolean anythingUseful = false;

        for (ImportFormatReader.UnknownFormatImport importResult : imports) {
            if (importResult == null) {
                continue;
            }
            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format)) {
                // Bibtex result. We must merge it into our main base.
                ParserResult pr = importResult.parserResult;

                anythingUseful = anythingUseful || pr.getDatabase().hasEntries() || (!pr.getDatabase().hasNoStrings());

                // Record the parserResult, as long as this is the first bibtex result:
                if (directParserResult == null) {
                    directParserResult = pr;
                }
                // Merge entries:
                database.insertEntries(pr.getDatabase().getEntries());

                // Merge strings:
                for (BibtexString bs : pr.getDatabase().getStringValues()) {
                    try {
                        database.addString((BibtexString) bs.clone());
                    } catch (KeyCollisionException e) {
                        // TODO: This means a duplicate string name exists, so it's not
                        // a very exceptional situation. We should maybe give a warning...?
                    }
                }
            } else {

                ParserResult pr = importResult.parserResult;
                List<BibEntry> entries = pr.getDatabase().getEntries();

                anythingUseful = anythingUseful | !entries.isEmpty();

                // set timestamp and owner
                UpdateField.setAutomaticFields(entries, Globals.prefs.getUpdateFieldPreferences()); // set timestamp and owner

                database.insertEntries(entries);
            }
        }

        if (!anythingUseful) {
            return null;
        }

        if ((imports.size() == 1) && (directParserResult != null)) {
            return directParserResult;
        } else {

            return new ParserResult(database);

        }
    }

}
