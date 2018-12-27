package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ImportException;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.UpdateField;
import org.jabref.model.database.BibDatabase;
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

    public ImportAction(JabRefFrame frame, boolean openInNew) {
        this(frame, openInNew, null);
    }

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
        BackgroundTask.wrap(() -> doImport(files))
                      .onSuccess(this::reportResult)
                      .executeWith(taskExecutor);
    }

    private void reportResult(List<ImportFormatReader.UnknownFormatImport> imports) {
        // Ok, done. Then try to gather in all we have found. Since we might
        // have found
        // one or more bibtex results, it's best to gather them in a
        // BibDatabase.
        ParserResult bibtexResult = mergeImportResults(imports);

        /* show parserwarnings, if any. */
        for (ImportFormatReader.UnknownFormatImport p : imports) {
            if (p != null) {
                ParserResult pr = p.parserResult;
                ParserResultWarningDialog.showParserResultWarningDialog(pr, frame);
            }
        }

        if (bibtexResult == null) {
            if (importer == null) {
                frame.output(Localization.lang("Could not find a suitable import format."));
            } else {
                // Import in a specific format was specified. Check if we have stored error information:
                if (importError == null) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Import failed"),
                                                         Localization.lang("No entries found. Please make sure you are using the correct import filter."));
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("Import failed"), importError);
                }
            }
        } else {
            if (openInNew) {
                frame.addTab(bibtexResult.getDatabaseContext(), true);
                frame.output(Localization.lang("Imported entries") + ": " + bibtexResult.getDatabase().getEntryCount());
            } else {
                final BasePanel panel = frame.getCurrentBasePanel();

                SwingUtilities.invokeLater(() -> {
                    ImportInspectionDialog diag = new ImportInspectionDialog(frame, panel, Localization.lang("Import"), false);
                    diag.addEntries(bibtexResult.getDatabase().getEntries());
                    diag.entryListComplete();
                    diag.setVisible(true);
                    diag.toFront();
                });
            }
        }
    }

    private List<ImportFormatReader.UnknownFormatImport> doImport(List<Path> files) {
        // We import all files and collect their results:
        List<ImportFormatReader.UnknownFormatImport> imports = new ArrayList<>();
        for (Path filename : files) {
            try {
                if (!importer.isPresent()) {
                    // Unknown format:
                    frame.output(Localization.lang("Importing in unknown format") + "...");
                    // This import method never throws an IOException:
                    imports.add(Globals.IMPORT_FORMAT_READER.importUnknownFormat(filename, Globals.getFileUpdateMonitor()));
                } else {
                    frame.output(Localization.lang("Importing in %0 format", importer.get().getName()) + "...");
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
                for (BibEntry entry : pr.getDatabase().getEntries()) {
                    database.insertEntry(entry);
                }

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
                Collection<BibEntry> entries = pr.getDatabase().getEntries();

                anythingUseful = anythingUseful | !entries.isEmpty();

                // set timestamp and owner
                UpdateField.setAutomaticFields(entries, Globals.prefs.getUpdateFieldPreferences()); // set timestamp and owner

                for (BibEntry entry : entries) {
                    database.insertEntry(entry);
                }
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
