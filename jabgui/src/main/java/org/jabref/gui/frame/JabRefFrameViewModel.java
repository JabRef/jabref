package org.jabref.gui.frame;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefFrameViewModel implements UiMessageHandler {
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
            List<Path> pathsToSave = filenames;
            Path focusedToSave = focusedDatabase;

            if (preferences.getInternalPreferences().isMemoryStickMode()) {
                Path baseDir = Path.of("").toAbsolutePath().normalize();
                pathsToSave = filenames.stream()
                                       .map(path -> {
                                           try {
                                               return baseDir.relativize(path.toAbsolutePath().normalize());
                                           } catch (IllegalArgumentException e) {
                                               return path;
                                           }
                                       })
                                       .toList();

                if (focusedDatabase != null) {
                    try {
                        focusedToSave = baseDir.relativize(focusedDatabase.toAbsolutePath().normalize());
                    } catch (IllegalArgumentException e) {
                        focusedToSave = focusedDatabase;
                    }
                }
            }

            if (pathsToSave.isEmpty()) {
                preferences.getLastFilesOpenedPreferences().getLastFilesOpened().clear();
            } else {
                preferences.getLastFilesOpenedPreferences().setLastFilesOpened(pathsToSave);
                preferences.getLastFilesOpenedPreferences().setLastFocusedFile(focusedToSave);
            }
        }
    }

    public boolean close() {
        if (stateManager.getAnyTasksThatWillNotBeRecoveredRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager);
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        List<Path> openedLibraries = tabContainer.getLibraryTabs().stream()
                                                 .map(LibraryTab::getBibDatabaseContext)
                                                 .map(BibDatabaseContext::getDatabasePath)
                                                 .flatMap(Optional::stream)
                                                 .toList();
        Path focusedLibraries = Optional.ofNullable(tabContainer.getCurrentLibraryTab())
                                        .map(LibraryTab::getBibDatabaseContext)
                                        .flatMap(BibDatabaseContext::getDatabasePath)
                                        .orElse(null);

        if (!tabContainer.closeTabs(tabContainer.getLibraryTabs())) {
            return false;
        }

        storeLastOpenedFiles(openedLibraries, focusedLibraries);

        ProcessingLibraryDialog processingLibraryDialog = new ProcessingLibraryDialog(dialogService);
        processingLibraryDialog.showAndWait(tabContainer.getLibraryTabs());

        return true;
    }

    @Override
    public void handleUiCommands(List<UiCommand> uiCommands) {
        LOGGER.debug("Handling UI commands {}", uiCommands);
        if (uiCommands.isEmpty()) {
            checkForBibInUpperDir();
            return;
        }

        boolean blank = uiCommands.stream().anyMatch(UiCommand.BlankWorkspace.class::isInstance);

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

        uiCommands.stream()
                  .filter(UiCommand.JumpToEntryKey.class::isInstance)
                  .map(UiCommand.JumpToEntryKey.class::cast)
                  .map(UiCommand.JumpToEntryKey::citationKey)
                  .filter(Objects::nonNull)
                  .findAny().ifPresent(entryKey -> {
                      LOGGER.debug("Jump to entry {} requested", entryKey);
                      waitForLoadingFinished(() -> jumpToEntry(entryKey));
                  });
    }

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

    private void importFromFileAndOpen(String location) {
        LOGGER.debug("Import file {} requested", location);
        BackgroundTask.wrap(() -> CliImportHelper.importFile(location, preferences, false))
                      .onSuccess(result -> result.ifPresent(this::addParserResult))
                      .onFailure(t -> LOGGER.error("Unable to import file {} ", location, t))
                      .executeWith(taskExecutor);
    }

    private void checkForBibInUpperDir() {
        if (tabContainer.getLibraryTabs().isEmpty()) {
            firstBibFile().ifPresent(firstBibFile -> openDatabaseAction.get().openFile(firstBibFile));
        }
    }

    private Optional<Path> firstBibFile() {
        Path absolutePath = Path.of("").toAbsolutePath();
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
            dirsToCheck.add(Path.of("../"));
        } else if (isJabRefBat) {
            dirsToCheck.add(Path.of("../../../"));
        } else if (isJabRef) {
            dirsToCheck.add(Path.of("../../"));
        }

        try {
            return dirsToCheck.stream()
                              .map(Path::toAbsolutePath)
                              .flatMap(Unchecked.function(Files::list))
                              .filter(path -> FileUtil.getFileExtension(path).equals(Optional.of("bib")))
                              .findFirst();
        } catch (UncheckedIOException ex) {
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

        List<ParserResult> invalidDatabases = parserResults.stream()
                                                           .filter(ParserResult::isInvalid)
                                                           .toList();
        final List<ParserResult> failed = new ArrayList<>(invalidDatabases);
        parserResults.removeAll(invalidDatabases);

        for (ParserResult parserResult : parserResults) {
            addParserResult(parserResult);
        }

        for (ParserResult parserResult : failed) {
            String message = Localization.lang("Error opening file '%0'",
                    parserResult.getPath().map(path -> path.toString()).orElse("(File name unknown)")) + "\n" +
                    parserResult.getErrorMessage();
            dialogService.showErrorDialogAndWait(Localization.lang("Error opening file"), message);
        }
    }

    private void addParserResult(ParserResult parserResult) {
        LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> parserResult);
        ImportCleanup cleanup = ImportCleanup.targeting(libraryTab.getBibDatabaseContext().getMode(), preferences.getFieldPreferences());
        cleanup.doPostCleanup(parserResult.getDatabase().getEntries());
        ImportEntriesDialog dialog = new ImportEntriesDialog(libraryTab.getBibDatabaseContext(), task);
        dialog.setTitle(Localization.lang("Import"));
        dialogService.showCustomDialogAndWait(dialog);
    }

    private void waitForLoadingFinished(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        List<ObservableBooleanValue> loadings = tabContainer.getLibraryTabs().stream()
                                                            .map(LibraryTab::getLoading)
                                                            .collect(Collectors.toList());

        ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
            if (observable instanceof ObservableBooleanValue observableBoolean) {
                loadings.remove(observableBoolean);
            }
            for (ObservableBooleanValue obs : loadings) {
                if (obs.get()) {
                    return;
                }
            }
            future.complete(null);
        };

        for (ObservableBooleanValue obs : loadings) {
            obs.addListener(listener);
        }

        listener.changed(null, false, false);
        future.thenRun(() -> {
            for (ObservableBooleanValue obs : loadings) {
                obs.removeListener(listener);
            }
            runnable.run();
        });
    }

    private void jumpToEntry(String entryKey) {
        LibraryTab currentLibraryTab = tabContainer.getCurrentLibraryTab();
        List<LibraryTab> sortedTabs = tabContainer.getLibraryTabs().stream()
                                                  .sorted(Comparator.comparing(tab -> tab != currentLibraryTab, Comparator.reverseOrder()))
                                                  .toList();
        for (LibraryTab libraryTab : sortedTabs) {
            Optional<BibEntry> bibEntry = libraryTab.getDatabase()
                                                    .getEntries().stream()
                                                    .filter(entry -> entry.getCitationKey().orElse("")
                                                                          .equals(entryKey))
                                                    .findAny();
            if (bibEntry.isPresent()) {
                libraryTab.clearAndSelect(bibEntry.get());
                tabContainer.showLibraryTab(libraryTab);
                break;
            }
        }

        if (stateManager.getSelectedEntries().isEmpty()) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", entryKey));
        }
    }
}
