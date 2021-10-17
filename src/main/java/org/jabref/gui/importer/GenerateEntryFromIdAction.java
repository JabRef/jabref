package org.jabref.gui.importer;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.JabRefException;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateEntryFromIdAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEntryFromIdDialog.class);

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final String identifier;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;

    public GenerateEntryFromIdAction(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor, String identifier) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.identifier = identifier;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        BackgroundTask<Optional<BibEntry>> backgroundTask = searchAndImportEntryInBackground();
        backgroundTask.titleProperty().set(Localization.lang("Import by ID"));
        backgroundTask.showToUser(true);
        backgroundTask.onRunning(() -> dialogService.notify("%s".formatted(backgroundTask.messageProperty().get())));
        backgroundTask.onFailure((e) -> dialogService.notify(Localization.lang("Entry could not be created")));
        backgroundTask.onSuccess((entry) -> entry.ifPresentOrElse(libraryTab::insertEntry,
                () -> dialogService.notify(Localization.lang("Import canceled"))
        ));
        backgroundTask.executeWith(taskExecutor);
    }

    private BackgroundTask<Optional<BibEntry>> searchAndImportEntryInBackground() {
        return new BackgroundTask<>() {
            @Override
            protected Optional<BibEntry> call() throws JabRefException {
                if (isCanceled()) {
                    return Optional.empty();
                }

                this.updateMessage(Localization.lang("Searching..."));

                // later catch more exceptions here and notify user
                Optional<BibEntry> result = new CompositeIdFetcher(preferencesService.getImportFormatPreferences()).performSearchById(identifier);
                LOGGER.debug("Resulted in " + result);

                if (result.isPresent()) {
                    final BibEntry entry = result.get();
                    ImportCleanup cleanup = new ImportCleanup(libraryTab.getBibDatabaseContext().getMode());
                    cleanup.doPostCleanup(entry);
                    Optional<BibEntry> duplicate = new DuplicateCheck(Globals.entryTypesManager).containsDuplicate(libraryTab.getDatabase(), entry, libraryTab.getBibDatabaseContext().getMode());
                    if ((duplicate.isPresent())) {
                        DuplicateResolverDialog dialog = new DuplicateResolverDialog(entry, duplicate.get(), DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK, libraryTab.getBibDatabaseContext(), stateManager);
                        switch (dialogService.showCustomDialogAndWait(dialog).orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK)) {
                            case KEEP_LEFT -> {
                                libraryTab.getDatabase().removeEntry(duplicate.get());
                                libraryTab.getDatabase().insertEntry(entry);
                            }
                            case KEEP_BOTH -> libraryTab.getDatabase().insertEntry(entry);
                            case KEEP_MERGE -> {
                                libraryTab.getDatabase().removeEntry(duplicate.get());
                                libraryTab.getDatabase().insertEntry(dialog.getMergedEntry());
                            }
                            default -> {
                            }
                        }
                    }
                } else {
                    updateMessage(Localization.lang("Error"));
                    // There could be more exact feedback if the CompositeIdFetcher.java / future IdFetcherManager
                    // had a better structure or there would be individual exceptions for fetchers.
                    throw new JabRefException("Invalid identifier or connection failure.");
                }
                updateMessage(Localization.lang("Imported one entry"));
                return result;
            }
        };
    }

}
