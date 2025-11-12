package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * SIDE EFFECT: Sets the "cites" field of the entry having the linked files
 *
 * <ul>
 *   <li>Mode choice A: online or offline</li>
 *   <li>Mode choice B: complete entry or single file (the latter is not implemented)</li>
 * </ul>
 * <p>
 * The mode is selected by the preferences whether to use Grobid or not.
 */
public class ExtractReferencesAction extends SimpleCommand {
    private final int FILES_LIMIT = 10;

    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CliPreferences preferences;
    private final BibEntry entry;
    private final LinkedFile linkedFile;

    private final RuleBasedBibliographyPdfImporter ruleBasedBibliographyPdfImporter;

    public ExtractReferencesAction(DialogService dialogService,
                                   StateManager stateManager,
                                   CliPreferences preferences) {
        this(dialogService, stateManager, preferences, null, null);
    }

    /**
     * Can be used to bind the action on a context menu in the linked file view (future work)
     *
     * @param entry      the entry to handle (can be null)
     * @param linkedFile the linked file (can be null)
     */
    private ExtractReferencesAction(@NonNull DialogService dialogService,
                                    @NonNull StateManager stateManager,
                                    @NonNull CliPreferences preferences,
                                    @Nullable BibEntry entry,
                                    @Nullable LinkedFile linkedFile) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.entry = entry;
        this.linkedFile = linkedFile;
        ruleBasedBibliographyPdfImporter = new RuleBasedBibliographyPdfImporter(preferences.getCitationKeyPatternPreferences());

        if (this.linkedFile == null) {
            this.executable.bind(
                    ActionHelper.needsEntriesSelected(stateManager)
                                .and(ActionHelper.hasLinkedFileForSelectedEntries(stateManager))
            );
        } else {
            this.setExecutable(true);
        }
    }

    @Override
    public void execute() {
        extractReferences();
    }

    private void extractReferences() {
        stateManager.getActiveDatabase().ifPresent(databaseContext -> {
            List<BibEntry> selectedEntries;
            if (entry == null) {
                selectedEntries = stateManager.getSelectedEntries();
            } else {
                selectedEntries = List.of(entry);
            }

            boolean online = this.preferences.getGrobidPreferences().isGrobidEnabled();
            Callable<ParserResult> parserResultCallable;
            if (online) {
                Optional<Callable<ParserResult>> parserResultCallableOnline = getParserResultCallableOnline(databaseContext, selectedEntries);
                if (parserResultCallableOnline.isEmpty()) {
                    return;
                }
                parserResultCallable = parserResultCallableOnline.get();
            } else {
                parserResultCallable = getParserResultCallableOffline(databaseContext, selectedEntries);
            }
            BackgroundTask<ParserResult> task = BackgroundTask.wrap(parserResultCallable)
                                                              .withInitialMessage(Localization.lang("Processing PDF(s)"));

            task.onFailure(dialogService::showErrorDialogAndWait);

            ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task);
            String title;
            if (online) {
                title = Localization.lang("Extract References (online)");
            } else {
                title = Localization.lang("Extract References (offline)");
            }
            dialog.setTitle(title);
            dialogService.showCustomDialogAndWait(dialog);
        });
    }

    private @NonNull Callable<ParserResult> getParserResultCallableOffline(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries) {
        return () -> {
            BibEntry currentEntry = selectedEntries.getFirst();
            List<Path> fileList = FileUtil.getListOfLinkedFiles(selectedEntries, databaseContext.getFileDirectories(preferences.getFilePreferences()));

            // We need to have ParserResult handled at the importer, because it imports the meta data (library type, encoding, ...)
            ParserResult result = ruleBasedBibliographyPdfImporter.importDatabase(fileList.getFirst());

            // subsequent files are just appended to result
            Iterator<Path> fileListIterator = fileList.iterator();
            fileListIterator.next(); // skip first file
            extractReferences(fileListIterator, result, currentEntry);

            // handle subsequent entries
            Iterator<BibEntry> selectedEntriesIterator = selectedEntries.iterator();
            selectedEntriesIterator.next(); // skip first entry
            while (selectedEntriesIterator.hasNext()) {
                currentEntry = selectedEntriesIterator.next();
                fileList = FileUtil.getListOfLinkedFiles(List.of(currentEntry), databaseContext.getFileDirectories(preferences.getFilePreferences()));
                fileListIterator = fileList.iterator();
                extractReferences(fileListIterator, result, currentEntry);
            }

            return result;
        };
    }

    private void extractReferences(Iterator<Path> fileListIterator, ParserResult result, BibEntry currentEntry) {
        while (fileListIterator.hasNext()) {
            result.getDatabase().insertEntries(ruleBasedBibliographyPdfImporter.importDatabase(fileListIterator.next()).getDatabase().getEntries());
        }

        String cites = getCites(result.getDatabase().getEntries(), currentEntry);
        currentEntry.setField(StandardField.CITES, cites);
    }

    /**
     * Creates the field content for the "cites" field. The field contains the citation keys of the imported entries.
     * <p>
     * TODO: Move this part to logic somehow
     *
     * @param currentEntry used to create citation keys if the importer did not provide one from the imported entry
     */
    private static String getCites(List<BibEntry> entries, BibEntry currentEntry) {
        StringJoiner cites = new StringJoiner(",");
        int count = 0;
        for (BibEntry importedEntry : entries) {
            count++;
            Optional<String> citationKey = importedEntry.getCitationKey();
            String citationKeyToAdd;
            if (citationKey.isPresent()) {
                citationKeyToAdd = citationKey.get();
            } else {
                // No key present -> generate one based on
                //   the citation key of the entry holding the files and
                //   the number of the current entry (extracted from the reference; fallback: current number of the entry (count variable))

                String sourceCitationKey = currentEntry.getCitationKey().orElse("unknown");
                String newCitationKey;
                // Could happen if no author and no year is present
                // We use the number of the comment field (because there is no other way to get the number reliable)
                Pattern pattern = Pattern.compile("^\\[(\\d+)\\]");
                Matcher matcher = pattern.matcher(importedEntry.getField(StandardField.COMMENT).orElse(""));
                if (matcher.hasMatch()) {
                    newCitationKey = sourceCitationKey + "-" + matcher.group(1);
                } else {
                    newCitationKey = sourceCitationKey + "-" + count;
                }
                importedEntry.setCitationKey(newCitationKey);
                citationKeyToAdd = newCitationKey;
            }
            cites.add(citationKeyToAdd);
        }
        return cites.toString();
    }

    private Optional<Callable<ParserResult>> getParserResultCallableOnline(BibDatabaseContext databaseContext, List<BibEntry> selectedEntries) {
        List<Path> fileList = FileUtil.getListOfLinkedFiles(selectedEntries, databaseContext.getFileDirectories(preferences.getFilePreferences()));
        if (fileList.size() > FILES_LIMIT) {
            boolean continueOpening = dialogService.showConfirmationDialogAndWait(Localization.lang("Processing a large number of files"),
                    Localization.lang("You are about to process %0 files. Continue?", fileList.size()),
                    Localization.lang("Continue"), Localization.lang("Cancel"));
            if (!continueOpening) {
                return Optional.empty();
            }
        }
        return Optional.of(() -> new ParserResult(
                new GrobidService(this.preferences.getGrobidPreferences()).processReferences(fileList, preferences.getImportFormatPreferences())
        ));
    }
}
