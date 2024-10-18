package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar to {@link ExtractReferencesAction}. This action creates a new library, the other action "just" appends to the current library
 *
 * <ul>
 *   <li>Mode choice A: online or offline</li>
 *   <li>Mode choice B: complete entry or single file (the latter is not implemented)</li>
 * </ul>
 */
public abstract class NewLibraryFromPdfAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewLibraryFromPdfAction.class);

    protected final CliPreferences preferences;

    private final LibraryTabContainer libraryTabContainer;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public NewLibraryFromPdfAction(
            LibraryTabContainer libraryTabContainer,
            StateManager stateManager,
            DialogService dialogService,
            CliPreferences preferences,
            TaskExecutor taskExecutor) {
        this.libraryTabContainer = libraryTabContainer;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        final FileDialogConfiguration.Builder builder = new FileDialogConfiguration.Builder();
        builder.withDefaultExtension(StandardFileType.PDF);
        // Sensible default for the directory to start browsing is the directory of the currently opened library. The pdf storage dir seems not to be feasible, because extracting references from a PDF itself can be done by the context menu of the respective entry.
        stateManager.getActiveDatabase()
                    .flatMap(BibDatabaseContext::getDatabasePath)
                    .ifPresent(path -> builder.withInitialDirectory(path.getParent()));
        FileDialogConfiguration fileDialogConfiguration = builder.build();

        LOGGER.trace("Opening file dialog with configuration: {}", fileDialogConfiguration);

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> {
            LOGGER.trace("Selected file: {}", path);
            Callable<ParserResult> parserResultCallable = getParserResultCallable(path);
            BackgroundTask.wrap(parserResultCallable)
                          .withInitialMessage(Localization.lang("Processing PDF(s)"))
                          .onFailure(dialogService::showErrorDialogAndWait)
                          .onSuccess(result -> {
                              LOGGER.trace("Finished processing PDF(s): {}", result);
                              libraryTabContainer.addTab(result.getDatabaseContext(), true);
                          })
                          .executeWith(taskExecutor);
        });
    }

    protected abstract Callable<ParserResult> getParserResultCallable(Path path);
}
