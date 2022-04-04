package org.jabref.gui.importer;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.PopOver;

public class GenerateEntryFromIdAction extends SimpleCommand {

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final String identifier;
    private final TaskExecutor taskExecutor;
    private final PopOver entryFromIdPopOver;
    private final StateManager stateManager;

    public GenerateEntryFromIdAction(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService, TaskExecutor taskExecutor, PopOver entryFromIdPopOver, String identifier, StateManager stateManager) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.identifier = identifier;
        this.taskExecutor = taskExecutor;
        this.entryFromIdPopOver = entryFromIdPopOver;
        this.stateManager = stateManager;
    }

    @Override
    public void execute() {

        BackgroundTask<Optional<BibEntry>> backgroundTask = searchAndImportEntryInBackground();
        backgroundTask.titleProperty().set(Localization.lang("Import by ID"));
        backgroundTask.showToUser(true);
        backgroundTask.onRunning(() -> dialogService.notify("%s".formatted(backgroundTask.messageProperty().get())));
        backgroundTask.onFailure((e) -> {
            // When unable to import by ID, present the user options to cancel or add entry manually
            boolean addEntryFlag = dialogService.showConfirmationDialogAndWait(Localization.lang("Failed to import by ID"),
                                                                               e.getMessage(),
                                                                               Localization.lang("Add entry manually"));
            if (addEntryFlag) {
                // add entry manually
                new NewEntryAction(libraryTab.frame(), StandardEntryType.Article, dialogService,
                                   preferencesService, stateManager).execute();
            }
        });
        backgroundTask.onSuccess((bibEntry) -> {
            Optional<BibEntry> result = bibEntry;
            if (result.isPresent()) {
                final BibEntry entry = result.get();
                ImportHandler handler = new ImportHandler(libraryTab.getBibDatabaseContext(), ExternalFileTypes.getInstance(), preferencesService, Globals.getFileUpdateMonitor(), libraryTab.getUndoManager(), stateManager, dialogService);
                handler.importEntryWithDuplicateCheck(libraryTab.getBibDatabaseContext(), entry);
            } else {
                dialogService.notify("No entry found or import canceled");
            }

            entryFromIdPopOver.hide();
        });
        backgroundTask.executeWith(taskExecutor);
    }

    private BackgroundTask<Optional<BibEntry>> searchAndImportEntryInBackground() {
        return new BackgroundTask<>() {

            @Override
            protected Optional<BibEntry> call() throws JabRefException {
                if (isCanceled()) {
                    return Optional.empty();
                }

                updateMessage(Localization.lang("Searching..."));
                try {
                    return new CompositeIdFetcher(preferencesService.getImportFormatPreferences()).performSearchById(identifier);

                } catch (FetcherException fetcherException) {
                    throw new JabRefException("Fetcher error: %s".formatted(fetcherException.getMessage()));
                }
            }
        };
    }

}
