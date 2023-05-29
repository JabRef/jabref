package org.jabref.gui.importer;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateEntryFromIdAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEntryFromIdAction.class);

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final String identifier;
    private final TaskExecutor taskExecutor;
    private final PopOver entryFromIdPopOver;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;

    public GenerateEntryFromIdAction(LibraryTab libraryTab,
                                     DialogService dialogService,
                                     PreferencesService preferencesService,
                                     TaskExecutor taskExecutor,
                                     PopOver entryFromIdPopOver,
                                     String identifier,
                                     StateManager stateManager,
                                     FileUpdateMonitor fileUpdateMonitor) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.identifier = identifier;
        this.taskExecutor = taskExecutor;
        this.entryFromIdPopOver = entryFromIdPopOver;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
    }

    @Override
    public void execute() {
        BackgroundTask<Optional<BibEntry>> backgroundTask = searchAndImportEntryInBackground();
        backgroundTask.titleProperty().set(Localization.lang("Import by ID"));
        backgroundTask.showToUser(true);
        backgroundTask.onRunning(() -> dialogService.notify("%s".formatted(backgroundTask.messageProperty().get())));
        backgroundTask.onFailure(exception -> {
            String fetcherExceptionMessage = exception.getMessage();

            String msg;
            if (exception instanceof FetcherClientException) {
                msg = Localization.lang("Bibliographic data not found. Cause is likely the client side. Please check connection and identifier for correctness.") + "\n" + fetcherExceptionMessage;
            } else if (exception instanceof FetcherServerException) {
                msg = Localization.lang("Bibliographic data not found. Cause is likely the server side. Please try again later.") + "\n" + fetcherExceptionMessage;
            } else {
                msg = Localization.lang("Error message %0", fetcherExceptionMessage);
            }

            LOGGER.info(fetcherExceptionMessage, exception);

            if (dialogService.showConfirmationDialogAndWait(Localization.lang("Failed to import by ID"), msg, Localization.lang("Add entry manually"))) {
                // add entry manually
                new NewEntryAction(libraryTab.frame(), StandardEntryType.Article, dialogService,
                                   preferencesService, stateManager).execute();
            }
        });
        backgroundTask.onSuccess(bibEntry -> {
            Optional<BibEntry> result = bibEntry;
            if (result.isPresent()) {
                final BibEntry entry = result.get();
                ImportHandler handler = new ImportHandler(
                        libraryTab.getBibDatabaseContext(),
                        preferencesService,
                        fileUpdateMonitor,
                        libraryTab.getUndoManager(),
                        stateManager,
                        dialogService,
                        taskExecutor);
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
            protected Optional<BibEntry> call() throws FetcherException {
                if (isCanceled()) {
                    return Optional.empty();
                }
                updateMessage(Localization.lang("Searching..."));
                return new CompositeIdFetcher(preferencesService.getImportFormatPreferences()).performSearchById(identifier);
            }
        };
    }
}
