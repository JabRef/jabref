package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import javafx.application.Platform;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibliographyFromPdfImporter;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similar to {@link ExtractReferencesAction}. This action creates a new library, the other action "just" appends to the current library
 *
 * <ul>
 *   <li>Mode choice A: online or offline</li>
 *   <li>Mode choice B: complete entry or single file (the latter is not implemented)</li>
 * </ul>
 * <p>
 * The mode is selected by the preferences whether to use Grobid or not.
 * <p>
 * The different modes should be implemented as sub classes. Moreover, there are synergies with {@link ExtractReferencesAction}. However, this was too complicated.
 */
public class NewLibraryFromPdfAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewLibraryFromPdfAction .class);

    private final LibraryTabContainer libraryTabContainer;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final BibliographyFromPdfImporter bibliographyFromPdfImporter;
    private final TaskExecutor taskExecutor;

    public NewLibraryFromPdfAction(
            LibraryTabContainer libraryTabContainer,
            StateManager stateManager,
            DialogService dialogService,
            PreferencesService preferencesService,
            TaskExecutor taskExecutor) {
        this.libraryTabContainer = libraryTabContainer;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        // Instruct the importer to keep the numbers (instead of generating keys)
        this.bibliographyFromPdfImporter = new BibliographyFromPdfImporter();
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        final FileDialogConfiguration.Builder builder = new FileDialogConfiguration.Builder();
        builder.withDefaultExtension(StandardFileType.PDF);
        // Sensible default for the directory to start browsing is the directory of the currently opened library. The pdf storage dir seems not to be feasible, because extracting references from a PDF itself can be done by the context menu of the respective entry.
        stateManager.getActiveDatabase()
                    .flatMap(db -> db.getDatabasePath())
                    .ifPresent(path -> builder.withInitialDirectory(path.getParent()));
        FileDialogConfiguration fileDialogConfiguration = builder.build();

        LOGGER.trace("Opening file dialog with configuration: {}", fileDialogConfiguration);

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> {
            LOGGER.trace("Selected file: {}", path);
            Callable<ParserResult> parserResultCallable = getParserResultCallable(path);
            BackgroundTask.wrap(parserResultCallable)
                          .withInitialMessage(Localization.lang("Processing PDF(s)"))
                          .onFailure(failure -> Platform.runLater(() -> dialogService.showErrorDialogAndWait(failure)))
                          .onSuccess(result -> {
                              LOGGER.trace("Finished processing PDF(s): {}", result);
                              libraryTabContainer.addTab(result.getDatabaseContext(), true);
                          })
                          .executeWith(taskExecutor);
        });
    }

    private Callable<ParserResult> getParserResultCallable(Path path) {
        Callable<ParserResult> parserResultCallable;
        boolean online = this.preferencesService.getGrobidPreferences().isGrobidEnabled();
        if (online) {
            parserResultCallable = () -> new ParserResult(
                    new GrobidService(this.preferencesService.getGrobidPreferences()).processReferences(path, preferencesService.getImportFormatPreferences()));
        } else {
            parserResultCallable = () -> bibliographyFromPdfImporter.importDatabase(path);
        }
        return parserResultCallable;
    }
}
