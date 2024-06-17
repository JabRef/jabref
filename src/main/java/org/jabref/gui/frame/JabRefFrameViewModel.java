package org.jabref.gui.frame;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.ButtonType;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefFrameViewModel implements UiMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrameViewModel.class);

    private final PreferencesService preferences;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final LibraryTabContainer tabContainer;
    private final BibEntryTypesManager entryTypesManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final UndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    public JabRefFrameViewModel(PreferencesService preferencesService,
                                StateManager stateManager,
                                DialogService dialogService,
                                LibraryTabContainer tabContainer,
                                BibEntryTypesManager entryTypesManager,
                                FileUpdateMonitor fileUpdateMonitor,
                                UndoManager undoManager,
                                ClipBoardManager clipBoardManager,
                                TaskExecutor taskExecutor) {
        this.preferences = preferencesService;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.tabContainer = tabContainer;
        this.entryTypesManager = entryTypesManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
    }

    void storeLastOpenedFiles(List<Path> filenames, Path focusedDatabase) {
        if (preferences.getWorkspacePreferences().shouldOpenLastEdited()) {
            // Here we store the names of all current files. If there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                preferences.getGuiPreferences().getLastFilesOpened().clear();
            } else {
                preferences.getGuiPreferences().setLastFilesOpened(filenames);
                preferences.getGuiPreferences().setLastFocusedFile(focusedDatabase);
            }
        }
    }

    /**
     * Quit JabRef
     *
     * @return true if the user chose to quit; false otherwise
     */
    public boolean close() {
        // Ask if the user really wants to close, if there are still background tasks running
        // The background tasks may make changes themselves that need saving.
        if (stateManager.getAnyTasksThatWillNotBeRecoveredRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager);
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        // Read the opened and focused databases before closing them
        List<Path> openedLibraries = tabContainer.getLibraryTabs().stream()
                                          .map(LibraryTab::getBibDatabaseContext)
                                          .map(BibDatabaseContext::getDatabasePath)
                                          .flatMap(Optional::stream)
                                          .toList();
        Path focusedLibraries = Optional.ofNullable(tabContainer.getCurrentLibraryTab())
                                        .map(LibraryTab::getBibDatabaseContext)
                                        .flatMap(BibDatabaseContext::getDatabasePath)
                                        .orElse(null);

        // Then ask if the user really wants to close, if the library has not been saved since last save.
        if (!tabContainer.closeTabs(tabContainer.getLibraryTabs())) {
            return false;
        }

        storeLastOpenedFiles(openedLibraries, focusedLibraries); // store only if successfully having closed the libraries

        ProcessingLibraryDialog processingLibraryDialog = new ProcessingLibraryDialog(dialogService);
        processingLibraryDialog.showAndWait(tabContainer.getLibraryTabs());

        return true;
    }

    /**
     * Handles commands submitted by the command line or by the remote host to be executed in the ui
     * Needs to run in a certain order. E.g. databases have to be loaded before selecting an entry.
     *
     * @param uiCommands to be handled
     */
    @Override
    public void handleUiCommands(List<UiCommand> uiCommands) {
        LOGGER.debug("Handling UI commands {}", uiCommands);
        if (uiCommands.isEmpty()) {
            return;
        }

        // Handle blank workspace
        boolean blank = uiCommands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance);

        // Handle OpenDatabases
        if (!blank) {
            uiCommands.stream()
                    .filter(UiCommand.OpenDatabases.class::isInstance)
                    .map(UiCommand.OpenDatabases.class::cast)
                    .forEach(command -> openDatabases(command.parserResults()));
        }

        // Handle jumpToEntry
        uiCommands.stream()
                  .filter(UiCommand.JumpToEntryKey.class::isInstance)
                  .map(UiCommand.JumpToEntryKey.class::cast)
                  .map(UiCommand.JumpToEntryKey::citationKey)
                  .filter(Objects::nonNull)
                  .findAny().ifPresent(entryKey -> {
                      LOGGER.debug("Jump to entry {} requested", entryKey);
                      // tabs must be present and contents async loaded for an entry to be selected
                      waitForLoadingFinished(() -> jumpToEntry(entryKey));
                  });
    }

    private void openDatabases(List<ParserResult> parserResults) {
        final List<ParserResult> failed = new ArrayList<>();
        final List<ParserResult> toOpenTab = new ArrayList<>();

        // Remove invalid databases
        List<ParserResult> invalidDatabases = parserResults.stream()
                                                           .filter(ParserResult::isInvalid)
                                                           .toList();
        failed.addAll(invalidDatabases);
        parserResults.removeAll(invalidDatabases);

        // passed file (we take the first one) should be focused
        Path focusedFile = parserResults.stream()
                                        .findFirst()
                                        .flatMap(ParserResult::getPath)
                                        .orElse(preferences.getGuiPreferences()
                                                           .getLastFocusedFile())
                                        .toAbsolutePath();

        // Add all bibDatabases databases to the frame:
        boolean first = false;
        for (ParserResult parserResult : parserResults) {
            // Define focused tab
            if (parserResult.getPath().filter(path -> path.toAbsolutePath().equals(focusedFile)).isPresent()) {
                first = true;
            }

            if (parserResult.getDatabase().isShared()) {
                try {
                    OpenDatabaseAction.openSharedDatabase(
                            parserResult,
                            tabContainer,
                            dialogService,
                            preferences,
                            stateManager,
                            entryTypesManager,
                            fileUpdateMonitor,
                            undoManager,
                            clipBoardManager,
                            taskExecutor);
                } catch (
                        SQLException |
                        DatabaseNotSupportedException |
                        InvalidDBMSConnectionPropertiesException |
                        NotASharedDatabaseException e) {
                    LOGGER.error("Connection error", e);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Connection error"),
                            Localization.lang("A local copy will be opened."),
                            e);
                    toOpenTab.add(parserResult);
                }
            } else if (parserResult.toOpenTab()) {
                // things to be appended to an opened tab should be done after opening all tabs
                // add them to the list
                toOpenTab.add(parserResult);
            } else {
                addParserResult(parserResult, first);
                first = false;
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult parserResult : toOpenTab) {
            addParserResult(parserResult, first);
            first = false;
        }

        for (ParserResult parserResult : failed) {
            String message = Localization.lang("Error opening file '%0'",
                    parserResult.getPath().map(Path::toString).orElse("(File name unknown)")) + "\n" +
                    parserResult.getErrorMessage();
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }

        // Display warnings, if any
        for (ParserResult parserResult : parserResults) {
            if (parserResult.hasWarnings()) {
                ParserResultWarningDialog.showParserResultWarningDialog(parserResult, dialogService);
                tabContainer.getLibraryTabs().stream()
                     .filter(tab -> parserResult.getDatabase().equals(tab.getDatabase()))
                     .findAny()
                     .ifPresent(tabContainer::showLibraryTab);
            }
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        parserResults.forEach(pr -> OpenDatabaseAction.performPostOpenActions(pr, dialogService, preferences));

        LOGGER.debug("Finished adding panels");
    }

    /**
     * Should be called when a user asks JabRef at the command line
     * i) to import a file or
     * ii) to open a .bib file
     */
    private void addParserResult(ParserResult parserResult, boolean raisePanel) {
        if (parserResult.toOpenTab()) {
            LOGGER.trace("Adding the entries to the open tab.");
            LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();
            if (libraryTab == null) {
                LOGGER.debug("No open tab found to add entries to. Creating a new tab.");
                tabContainer.addTab(parserResult.getDatabaseContext(), raisePanel);
            } else {
                addImportedEntries(libraryTab, parserResult);
            }
        } else {
            // only add tab if library is not already open
            Optional<LibraryTab> libraryTab = tabContainer.getLibraryTabs().stream()
                                                          .filter(p -> p.getBibDatabaseContext()
                                                                        .getDatabasePath()
                                                                        .equals(parserResult.getPath()))
                                                          .findFirst();

            if (libraryTab.isPresent()) {
                tabContainer.showLibraryTab(libraryTab.get());
            } else {
                // On this place, a tab is added after loading using the command line
                // This takes a different execution path than loading a library using the GUI
                tabContainer.addTab(parserResult.getDatabaseContext(), raisePanel);
            }
        }
    }

    private void waitForLoadingFinished(Runnable runnable) {
        LOGGER.trace("Waiting for all tabs being loaded");

        CompletableFuture<Void> future = new CompletableFuture<>();

        List<ObservableBooleanValue> loadings = tabContainer.getLibraryTabs().stream()
                                                            .map(LibraryTab::getLoading)
                                                            .collect(Collectors.toList());

        // Create a listener for each observable
        ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
            if (observable != null) {
                loadings.remove(observable);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Count of loading tabs: {}", loadings.size());
                LOGGER.trace("Count of loading tabs really true: {}", loadings.stream().filter(ObservableBooleanValue::get).count());
            }
            for (ObservableBooleanValue obs : loadings) {
                if (obs.get()) {
                    // Exit the listener if any of the observables is still true
                    return;
                }
            }
            // All observables are false, complete the future
            LOGGER.trace("Future completed");
            future.complete(null);
        };

        for (ObservableBooleanValue obs : loadings) {
            obs.addListener(listener);
        }

        LOGGER.trace("Fire once");
        // Due to concurrency, it might be that the observables are already false, so we trigger one evaluation
        listener.changed(null, null, false);
        LOGGER.trace("Waiting for state changes...");

        future.thenRun(() -> {
            LOGGER.debug("All tabs loaded. Jumping to entry.");
            for (ObservableBooleanValue obs : loadings) {
                obs.removeListener(listener);
            }
            runnable.run();
        });
    }

    private void jumpToEntry(String entryKey) {
        // check current library tab first
        LibraryTab currentLibraryTab = tabContainer.getCurrentLibraryTab();
        List<LibraryTab> sortedTabs = tabContainer.getLibraryTabs().stream()
                                                  .sorted(Comparator.comparing(tab -> tab != currentLibraryTab))
                                                  .toList();
        for (LibraryTab libraryTab : sortedTabs) {
            Optional<BibEntry> bibEntry = libraryTab.getDatabase()
                                                    .getEntries().stream()
                                                    .filter(entry -> entry.getCitationKey().orElse("")
                                                                          .equals(entryKey))
                                                    .findAny();
            if (bibEntry.isPresent()) {
                LOGGER.debug("Found entry {} in library tab {}", entryKey, libraryTab);
                libraryTab.clearAndSelect(bibEntry.get());
                tabContainer.showLibraryTab(libraryTab);
                break;
            }
        }

        LOGGER.trace("End of loop");

        if (stateManager.getSelectedEntries().isEmpty()) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", entryKey));
        }
    }

    /**
     * Opens the import inspection dialog to let the user decide which of the given entries to import.
     *
     * @param tab        The LibraryTab to add to.
     * @param parserResult The entries to add.
     */
    void addImportedEntries(final LibraryTab tab, final ParserResult parserResult) {
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportCleanup cleanup = ImportCleanup.targeting(tab.getBibDatabaseContext().getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(parserResult.getDatabase().getEntries());
        ImportEntriesDialog dialog = new ImportEntriesDialog(tab.getBibDatabaseContext(), task);
        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }
}
