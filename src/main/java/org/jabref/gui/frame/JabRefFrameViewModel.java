package org.jabref.gui.frame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
import org.jabref.gui.externalfiles.AutoLinkFilesAction;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.ParserResultWarningDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.shared.DatabaseNotSupportedException;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.NotASharedDatabaseException;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefFrameViewModel implements UiMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrameViewModel.class);

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final LibraryTabContainer tabContainer;
    private final BibEntryTypesManager entryTypesManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final UndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    public JabRefFrameViewModel(GuiPreferences preferences,
                                AiService aiService,
                                StateManager stateManager,
                                DialogService dialogService,
                                LibraryTabContainer tabContainer,
                                BibEntryTypesManager entryTypesManager,
                                FileUpdateMonitor fileUpdateMonitor,
                                UndoManager undoManager,
                                ClipBoardManager clipBoardManager,
                                TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.aiService = aiService;
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
                preferences.getLastFilesOpenedPreferences().getLastFilesOpened().clear();
            } else {
                preferences.getLastFilesOpenedPreferences().setLastFilesOpened(filenames);
                preferences.getLastFilesOpenedPreferences().setLastFocusedFile(focusedDatabase);
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
            checkForBibInUpperDir();
            return;
        }

        assert !uiCommands.isEmpty();

        // Handle blank workspace
        boolean blank = uiCommands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance);

        // Handle OpenDatabases
        if (!blank) {
            uiCommands.stream()
                    .filter(UiCommand.OpenDatabases.class::isInstance)
                    .map(UiCommand.OpenDatabases.class::cast)
                    .forEach(command -> openDatabases(command.parserResults()));
        }

        // Handle automatically setting file links
        uiCommands.stream()
                  .filter(UiCommand.AutoSetFileLinks.class::isInstance).findAny()
                  .map(UiCommand.AutoSetFileLinks.class::cast)
                  .ifPresent(autoSetFileLinks -> autoSetFileLinks(autoSetFileLinks.parserResults()));

        // Handle jumpToEntry
        // Needs to go last, because it requires all libraries opened
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

    private void checkForBibInUpperDir() {
        // "Open last edited databases" happened before this call
        // Moreover, there is not any CLI command (especially, not opening any new tab)
        // Thus, we check if there are any tabs open.
        if (tabContainer.getLibraryTabs().isEmpty()) {
            Optional<Path> firstBibFile = firstBibFile();
            if (firstBibFile.isPresent()) {
                ParserResult parserResult;
                try {
                    parserResult = OpenDatabase.loadDatabase(
                            firstBibFile.get(),
                            preferences.getImportFormatPreferences(),
                            fileUpdateMonitor);
                } catch (IOException e) {
                    LOGGER.error("Could not open bib file {}", firstBibFile.get(), e);
                    return;
                }
                List<ParserResult> librariesToOpen = new ArrayList<>(1);
                librariesToOpen.add(parserResult);
                openDatabases(librariesToOpen);
            }
        }
    }

    /// Use case: User starts `JabRef.bat` or `JabRef.exe`. JabRef should open a "close by" bib file.
    /// By "close by" a `.bib` file in the current folder or one level up of `JabRef.exe`is meant.
    ///
    /// Paths:
    ///   - `...\{example-dir}\JabRef\JabRef.exe` (Windows)
    ///   - `.../{example-dir}/JabRef/bin/JabRef` (Linux)
    ///   - `...\{example-dir}\JabRef\runtime\bin\JabRef.bat` (Windows)
    ///
    /// In the example, `...\{example-dir}\example.bib` should be found.
    ///
    /// We do NOT go up another level (i.e., everything in `...` is not found)
    private Optional<Path> firstBibFile() {
        Path absolutePath = Path.of(".").toAbsolutePath();
        if (OS.LINUX && absolutePath.startsWith("/usr")) {
            return Optional.empty();
        }
        if (OS.OS_X && absolutePath.startsWith("/Applications")) {
            return Optional.empty();
        }
        if (OS.WINDOWS && absolutePath.startsWith("C:\\Program Files")) {
            return Optional.empty();
        }

        boolean isJabRefExe = Files.exists(Path.of("JabRef.exe"));
        boolean isJabRefBat = Files.exists(Path.of("JabRef.bat"));
        boolean isJabRef = Files.exists(Path.of("JabRef"));

        ArrayList<Path> dirsToCheck = new ArrayList<>(2);
        dirsToCheck.add(Path.of(""));
        if (isJabRefExe) {
            dirsToCheck.add(Path.of("../"));       // directory above `JabRef.exe` directory
        } else if (isJabRefBat) {
            dirsToCheck.add(Path.of("../../../")); // directory above `runtime\bin\JabRef.bat`
        } else if (isJabRef) {
            dirsToCheck.add(Path.of("../..(/"));   // directory above `bin/JabRef` directory
        }

        // We want to check dirsToCheck only, not all subdirs (due to unnecessary disk i/o)
        try {
            return dirsToCheck.stream()
                              .map(Path::toAbsolutePath)
                              .flatMap(Unchecked.function(dir -> Files.list(dir)))
                              .filter(path -> FileUtil.getFileExtension(path).equals(Optional.of("bib")))
                              .findFirst();
        } catch (UncheckedIOException ex) {
            // Could be access denied exception - when this is started from the application directory
            // Therefore log level "debug"
            LOGGER.debug("Could not check for existing bib file {}", dirsToCheck, ex);
            return Optional.empty();
        }
    }

    /// Opens the libraries given in `parserResults`. This list needs to be modifiable, because invalidDatabases are removed.
    ///
    /// @param parserResults A modifiable list of parser results
    private void openDatabases(List<ParserResult> parserResults) {
        final List<ParserResult> toOpenTab = new ArrayList<>();

        // Remove invalid databases
        List<ParserResult> invalidDatabases = parserResults.stream()
                                                           .filter(ParserResult::isInvalid)
                                                           .toList();
        final List<ParserResult> failed = new ArrayList<>(invalidDatabases);
        parserResults.removeAll(invalidDatabases);

        // passed file (we take the first one) should be focused
        Path focusedFile = parserResults.stream()
                                        .findFirst()
                                        .flatMap(ParserResult::getPath)
                                        .orElse(preferences.getLastFilesOpenedPreferences()
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
                            aiService,
                            stateManager,
                            entryTypesManager,
                            fileUpdateMonitor,
                            undoManager,
                            clipBoardManager,
                            taskExecutor);
                } catch (SQLException
                         | DatabaseNotSupportedException
                         | InvalidDBMSConnectionPropertiesException
                         | NotASharedDatabaseException e) {
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
                getLibraryTab(parserResult).ifPresent(tabContainer::showLibraryTab);
            }
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        parserResults.forEach(pr -> {
            OpenDatabaseAction.performPostOpenActions(pr, dialogService, preferences);
            if (pr.getChangedOnMigration()) {
                getLibraryTab(pr).ifPresent(LibraryTab::markBaseChanged);
            }
        });

        LOGGER.debug("Finished adding panels");
    }

    private Optional<LibraryTab> getLibraryTab(ParserResult parserResult) {
        return tabContainer.getLibraryTabs().stream()
                           .filter(tab -> parserResult.getDatabase().equals(tab.getDatabase()))
                           .findAny();
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
            // Instanceof implicitly checks for null value
            if (observable instanceof ObservableBooleanValue observableBoolean) {
                loadings.remove(observableBoolean);
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

    void autoSetFileLinks(List<ParserResult> loaded) {
        for (ParserResult parserResult : loaded) {
            new AutoLinkFilesAction(dialogService, preferences, stateManager, undoManager, (UiTaskExecutor) taskExecutor).execute();
        }
    }
}
