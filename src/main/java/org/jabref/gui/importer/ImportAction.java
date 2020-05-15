package org.jabref.gui.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.ContentSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportAction.class);

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
     *
     * @param filenames List of files to import
     */
    public void automatedImport(List<String> filenames) {
        List<Path> files = filenames.stream().map(Path::of).collect(Collectors.toList());
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> {
            List<ImportFormatReader.UnknownFormatImport> imports = doImport(files);
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

    /**
     * TODO: Move this to logic package. Blocked by undo functionality.
     */
    private ParserResult mergeImportResults(List<ImportFormatReader.UnknownFormatImport> imports) {
        BibDatabase resultDatabase = new BibDatabase();
        ParserResult result = new ParserResult(resultDatabase);

        for (ImportFormatReader.UnknownFormatImport importResult : imports) {
            if (importResult == null) {
                continue;
            }
            ParserResult parserResult = importResult.parserResult;
            List<BibEntry> entries = parserResult.getDatabase().getEntries();
            resultDatabase.insertEntries(entries);

            if (ImportFormatReader.BIBTEX_FORMAT.equals(importResult.format)) {
                // additional treatment of BibTeX
                // merge into existing database

                // Merge strings
                for (BibtexString bibtexString : parserResult.getDatabase().getStringValues()) {
                    String bibtexStringName = bibtexString.getName();
                    if (resultDatabase.hasStringByName(bibtexStringName)) {
                        String importedContent = bibtexString.getContent();
                        String existingContent = resultDatabase.getStringByName(bibtexStringName).get().getContent();
                        if (!importedContent.equals(existingContent)) {
                            LOGGER.warn("String contents differ for {}: {} != {}", bibtexStringName, importedContent, existingContent);
                            // TODO: decide what to do here (in case the same string exits)
                        }
                    } else {
                        resultDatabase.addString(bibtexString);
                    }
                }

                // Merge groups
                // Adds the specified node as a child of the current root. The group contained in <b>newGroups </b> must not be of
                // type AllEntriesGroup, since every tree has exactly one AllEntriesGroup (its root). The <b>newGroups </b> are
                // inserted directly, i.e. they are not deepCopy()'d.
                parserResult.getMetaData().getGroups().ifPresent(newGroups -> {
                    // ensure that there is always only one AllEntriesGroup in the resulting database
                    // "Rename" the AllEntriesGroup of the imported database to "Imported"
                    if (newGroups.getGroup() instanceof AllEntriesGroup) {
                        // create a dummy group
                        try {
                            // This will cause a bug if the group already exists
                            // There will be group where the two groups are merged
                            String newGroupName = importResult.parserResult.getFile().map(File::getName).orElse("unknown");
                            ExplicitGroup group = new ExplicitGroup("Imported " + newGroupName, GroupHierarchyType.INDEPENDENT,
                                    Globals.prefs.getKeywordDelimiter());
                            newGroups.setGroup(group);
                            group.add(parserResult.getDatabase().getEntries());
                        } catch (IllegalArgumentException e) {
                            LOGGER.error("Problem appending entries to group", e);
                        }
                    }
                    result.getMetaData().getGroups().ifPresent(newGroups::moveTo);
                });

                for (ContentSelector selector : parserResult.getMetaData().getContentSelectorList()) {
                    result.getMetaData().addContentSelector(selector);
                }
            }
            // TODO: collect errors into ParserResult, because they are currently ignored (see caller of this method)
        }

        // set timestamp and owner
        UpdateField.setAutomaticFields(resultDatabase.getEntries(), Globals.prefs.getOwnerPreferences(), Globals.prefs.getTimestampPreferences()); // set timestamp and owner

        return result;
    }
}
