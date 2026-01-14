package org.jabref.gui.frame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.ButtonType;

import org.jabref.cli.CliImportHelper;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.UiCommand;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefFrameViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefFrameViewModel.class);

    private final GuiPreferences preferences;
    private final AiService aiService;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final LibraryTabContainer tabContainer;
    private final Supplier<OpenDatabaseAction> openDatabaseAction;
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
                                Supplier<OpenDatabaseAction> openDatabaseAction,
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
        this.openDatabaseAction = openDatabaseAction;
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
     * Does NOT handle focus - this is done in JabRefFrame
     *
     * @param uiCommands to be handled
     */
    public void handleUiCommands(List<UiCommand> uiCommands) {
        LOGGER.debug("Handling UI commands {}", uiCommands);
        if (uiCommands.isEmpty()) {
            checkForBibInUpperDir();
            return;
        }

        // Handle blank workspace
        boolean blank = uiCommands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance);

        // Handle OpenDatabases
        if (!blank) {
            uiCommands.stream()
                      .filter(UiCommand.OpenLibraries.class::isInstance)
                      .map(UiCommand.OpenLibraries.class::cast)
                      .forEach(command -> openDatabaseAction.get().openFiles(command.toImport()));

            uiCommands.stream()
                      .filter(UiCommand.AppendToCurrentLibrary.class::isInstance)
                      .map(UiCommand.AppendToCurrentLibrary.class::cast)
                      .map(UiCommand.AppendToCurrentLibrary::toAppend)
                      .filter(Objects::nonNull)
                      .findAny().ifPresent(toAppend -> {
                          LOGGER.debug("Append to current library {} requested", toAppend);
                          waitForLoadingFinished(() -> appendToCurrentLibrary(toAppend));
                      });

            uiCommands.stream().filter(UiCommand.AppendFileOrUrlToCurrentLibrary.class::isInstance)
                      .map(UiCommand.AppendFileOrUrlToCurrentLibrary.class::cast)
                      .findAny().ifPresent(importFile -> importFromFileAndOpen(importFile.location()));

            uiCommands.stream().filter(UiCommand.AppendBibTeXToCurrentLibrary.class::isInstance)
                      .map(UiCommand.AppendBibTeXToCurrentLibrary.class::cast)
                      .findAny().ifPresent(importBibTex -> importBibtexStringAndOpen(importBibTex.bibtex()));
        }

        // Handle jumpToEntry
        // Needs to go last, because it requires all libraries opened
        uiCommands.stream()
                  .filter(UiCommand.SelectEntryKeys.class::isInstance)
                  .map(UiCommand.SelectEntryKeys.class::cast)
                  .map(UiCommand.SelectEntryKeys::citationKey)
                  .filter(Objects::nonNull)
                  .findAny().ifPresent(entryKeys -> {
                      LOGGER.debug("Jump to entry(s) {} requested", entryKeys);
                      // tabs must be present and contents async loaded for an entry to be selected
                      waitForLoadingFinished(() -> selectEntries(entryKeys));
                  });
    }

    /// @deprecated used by the browser extension only
    private void importBibtexStringAndOpen(String importStr) {
        LOGGER.debug("ImportBibtex {} requested", importStr);
        BackgroundTask.wrap(() -> {
                          BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences());
                          List<BibEntry> entries = parser.parseEntries(importStr);
                          return new ParserResult(entries);
                      }).onSuccess(this::addParserResult)
                      .onFailure(e -> LOGGER.error("Unable to parse provided bibtex {}", importStr, e))
                      .executeWith(taskExecutor);
    }

    /// @deprecated used by the browser extension only
    private void importFromFileAndOpen(String location) {
        LOGGER.debug("Import file {} requested", location);
        BackgroundTask.wrap(() -> CliImportHelper.importFile(location, preferences, false))
                      .onSuccess(result -> result.ifPresent(this::addParserResult))
                      .onFailure(t -> LOGGER.error("Unable to import file {} ", location, t))
                      .executeWith(taskExecutor);
    }

    private void checkForBibInUpperDir() {
        // "Open last edited databases" happened before this call
        // Moreover, there is not any CLI command (especially, not opening any new tab)
        // Thus, we check if there are any tabs open.
        if (tabContainer.getLibraryTabs().isEmpty()) {
            firstBibFile().ifPresent(firstBibFile -> openDatabaseAction.get().openFile(firstBibFile));
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
                              .flatMap(Unchecked.function(Files::list))
                              .filter(path -> FileUtil.getFileExtension(path).equals(Optional.of("bib")))
                              .findFirst();
        } catch (UncheckedIOException ex) {
            // Could be access denied exception - when this is started from the application directory
            // Therefore log level "debug"
            LOGGER.debug("Could not check for existing bib file {}", dirsToCheck, ex);
            return Optional.empty();
        }
    }

    private void appendToCurrentLibrary(List<Path> libraries) {
        List<ParserResult> parserResults = new ArrayList<>();
        try {
            for (Path file : libraries) {
                parserResults.add(OpenDatabase.loadDatabase(
                        file,
                        preferences.getImportFormatPreferences(),
                        fileUpdateMonitor));
            }
        } catch (IOException e) {
            LOGGER.error("Could not open bib file {}", libraries, e);
            return;
        }

        // Remove invalid databases
        List<ParserResult> invalidDatabases = parserResults.stream()
                                                           .filter(ParserResult::isInvalid)
                                                           .toList();
        final List<ParserResult> failed = new ArrayList<>(invalidDatabases);
        parserResults.removeAll(invalidDatabases);

        // Add parserResult to the currently opened tab
        for (ParserResult parserResult : parserResults) {
            addParserResult(parserResult);
        }

        for (ParserResult parserResult : failed) {
            String message = Localization.lang("Error opening file '%0'",
                    parserResult.getPath().map(Path::toString).orElse("(File name unknown)")) + "\n" +
                    parserResult.getErrorMessage();
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }
    }

    private void addParserResult(ParserResult parserResult) {
        LOGGER.trace("Adding the entries to the open tab.");
        LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportCleanup cleanup = ImportCleanup.targeting(libraryTab.getBibDatabaseContext().getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(parserResult.getDatabase().getEntries());
        ImportEntriesDialog dialog = new ImportEntriesDialog(libraryTab.getBibDatabaseContext(), task);

        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }

    private void waitForLoadingFinished(Runnable runnable) {
        LOGGER.trace("Waiting for all tabs being loaded");

        CompletableFuture<Void> future = new CompletableFuture<>();

        List<ObservableBooleanValue> loadings = tabContainer.getLibraryTabs().stream()
                                                            .map(LibraryTab::getLoading)
                                                            .collect(Collectors.toList());

        // Create a listener for each observable
        ChangeListener<Boolean> listener = (observable, _, _) -> {
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

    @NullMarked
    private void selectEntries(List<String> entryKeys) {
        // check current library tab first
        LibraryTab currentLibraryTab = tabContainer.getCurrentLibraryTab();
        List<LibraryTab> sortedTabs = tabContainer.getLibraryTabs().stream()
                                                  .sorted(Comparator.comparing(tab -> tab != currentLibraryTab))
                                                  .toList();
        Set<String> keysToSelect = new HashSet<>(entryKeys);
        LibraryTab firstFoundTab = null;
        for (LibraryTab libraryTab : sortedTabs) {
            List<BibEntry> entrysToSelectInCurrentTab = new ArrayList<>(keysToSelect.size());
            for (BibEntry entry : libraryTab.getDatabase()
                                            .getEntries()) {
                Optional<String> citationKeyOptional = entry.getCitationKey();
                if (citationKeyOptional.isEmpty()) {
                    continue;
                }
                String citationKey = citationKeyOptional.get();
                if (!keysToSelect.contains(citationKey)) {
                    continue;
                }
                if (firstFoundTab != null) {
                    firstFoundTab = libraryTab;
                }
                LOGGER.debug("Found entry {} in library tab {}", citationKey, libraryTab);
                keysToSelect.remove(citationKey);
                entrysToSelectInCurrentTab.add(entry);
            }
            libraryTab.clearAndSelect(entrysToSelectInCurrentTab);
        }
        LOGGER.trace("End of loop");
        if (firstFoundTab != null) {
            tabContainer.showLibraryTab(firstFoundTab);
        }
        for (String key : keysToSelect) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", key));
        }
    }
}
