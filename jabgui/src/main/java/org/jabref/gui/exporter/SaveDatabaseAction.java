package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.autosaveandbackup.AutosaveManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.git.GitAutoSync;
import org.jabref.gui.git.GitPullScheduler;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.FileChangedException;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileSnapshot;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for save operations
/// when closing a database or quitting the applications.
///
/// The save operation is loaded off of the GUI thread using [org.jabref.logic.util.BackgroundTask]. Callers can query whether the
/// operation was canceled, or whether it was successful.
public class SaveDatabaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveDatabaseAction.class);

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;
    private final StateManager stateManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final TaskExecutor taskExecutor;

    public enum SaveDatabaseMode {
        SILENT, NORMAL
    }

    /// `ALREADY_SAVING` reports that another thread is writing the very same library: nothing was written by this call,
    /// and the file on disk is not yet the state the user sees.
    public enum SaveResult {
        SUCCESS, FAILURE, ALREADY_SAVING
    }

    public SaveDatabaseAction(LibraryTab libraryTab,
                              DialogService dialogService,
                              GuiPreferences preferences,
                              BibEntryTypesManager entryTypesManager,
                              StateManager stateManager,
                              JournalAbbreviationRepository journalAbbreviationRepository) {
        this(libraryTab,
                dialogService,
                preferences,
                entryTypesManager,
                stateManager,
                journalAbbreviationRepository,
                Injector.instantiateModelOrService(GitHandlerRegistry.class),
                Injector.instantiateModelOrService(TaskExecutor.class));
    }

    public SaveDatabaseAction(LibraryTab libraryTab,
                              DialogService dialogService,
                              GuiPreferences preferences,
                              BibEntryTypesManager entryTypesManager,
                              StateManager stateManager,
                              JournalAbbreviationRepository journalAbbreviationRepository,
                              GitHandlerRegistry gitHandlerRegistry,
                              TaskExecutor taskExecutor) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
        this.stateManager = stateManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.taskExecutor = taskExecutor;
    }

    public SaveResult save() {
        return save(SaveDatabaseMode.NORMAL);
    }

    public SaveResult save(SaveDatabaseMode mode) {
        return save(libraryTab.getBibDatabaseContext(), mode, true);
    }

    /// Saves the library without triggering the automatic Git commit configured in the library properties.
    public SaveResult saveWithoutGitAutoCommit(SaveDatabaseMode mode) {
        return save(libraryTab.getBibDatabaseContext(), mode, false);
    }

    /// Asks the user for the path and saves afterward
    public void saveAs() {
        askForSavePath().ifPresent(this::saveAs);
    }

    public boolean saveAs(Path file) {
        return this.saveAs(file, SaveDatabaseMode.NORMAL);
    }

    private SelfContainedSaveOrder getSaveOrder() {
        return libraryTab.getBibDatabaseContext()
                         .getMetaData().getSaveOrder()
                         .map(so -> {
                             if (so.getOrderType() == SaveOrder.OrderType.TABLE) {
                                 // We need to "flatten out" SaveOrder.OrderType.TABLE as BibWriter does not have access to preferences
                                 List<TableColumn<BibEntryTableViewModel, ?>> sortOrder = libraryTab.getMainTable().getSortOrder();
                                 return new SelfContainedSaveOrder(
                                         SaveOrder.OrderType.SPECIFIED,
                                         sortOrder.stream()
                                                  .filter(col -> col instanceof MainTableColumn<?>)
                                                  .map(column -> ((MainTableColumn<?>) column).getModel())
                                                  .flatMap(model -> model.getSortCriteria().stream())
                                                  .toList());
                             } else {
                                 return SelfContainedSaveOrder.of(so);
                             }
                         })
                         .orElse(SaveOrder.getDefaultSaveOrder());
    }

    public void saveSelectedAsPlain() {
        askForSavePath().ifPresent(path -> {
            try {
                saveDatabase(path, true, StandardCharsets.UTF_8, BibDatabaseWriter.SaveType.PLAIN_BIBTEX, getSaveOrder(), null);
                preferences.getLastFilesOpenedPreferences().getFileHistory().newFile(path);
                dialogService.notify(Localization.lang("Saved selected to '%0'.", path.toString()));
            } catch (SaveException ex) {
                LOGGER.error("A problem occurred when trying to save the file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
        });
    }

    boolean saveAs(Path file, SaveDatabaseMode mode) {
        return saveAs(file, mode, true);
    }

    /// @param file the new file name to save the database to. This is stored in the database context of the panel upon successful save.
    /// @return true on successful save
    boolean saveAs(Path file, SaveDatabaseMode mode, boolean mayAutoCommit) {
        BibDatabaseContext context = libraryTab.getBibDatabaseContext();

        boolean managersShutDown = context.getDatabasePath().isPresent();
        if (managersShutDown) {
            // Close AutosaveManager, BackupManager, and IndexManager for original library
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context, this.preferences.getFilePreferences().getBackupDirectory(), preferences.getFilePreferences().shouldCreateBackup());
            GitPullScheduler.shutdown(context);
            libraryTab.closeSearchContext();
        }

        // Set new location
        if (context.getLocation() == DatabaseLocation.SHARED) {
            // Save all properties dependent on the ID. This makes it possible to restore them.
            new SharedDatabasePreferences(context.getDatabase().generateSharedDatabaseID())
                    .putAllDBMSConnectionProperties(context.getDBMSSynchronizer().getConnectionProperties());
        }

        SaveResult saveResult = save(file, mode, mayAutoCommit);
        if (saveResult == SaveResult.ALREADY_SAVING) {
            // Nothing was written to the new file, so the library has to keep pointing at the old one.
            dialogService.notify(Localization.lang("The library is currently being saved. Please try again."));
        }

        if (saveResult == SaveResult.SUCCESS) {
            // we managed to successfully save the file
            // thus, we can store the path into the context
            context.setDatabasePath(file);
            stateManager.setActiveDatabase(context);
            libraryTab.updateTabTitle(false);

            // Reset (here: uninstall and install again) AutosaveManager, BackupManager and IndexManager for the new file name
            libraryTab.resetChangeMonitor();
            libraryTab.installAutosaveManagerAndBackupManager();
            libraryTab.createSearchContext();

            preferences.getLastFilesOpenedPreferences().getFileHistory().newFile(file);
        } else if (managersShutDown) {
            // The library stays at its old path, so the managers shut down above have to come back for that file.
            libraryTab.installAutosaveManagerAndBackupManager();
            libraryTab.createSearchContext();
        }
        return saveResult == SaveResult.SUCCESS;
    }

    /// Asks the user for the path to save to. Stores the directory to the preferences, which is used next time when
    /// opening the dialog.
    ///
    /// @return the path set by the user
    private Optional<Path> askForSavePath() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();
        Optional<Path> selectedPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        selectedPath.ifPresent(path -> preferences.getFilePreferences().setWorkingDirectory(path.getParent()));
        if (selectedPath.isPresent()) {
            Path savePath = selectedPath.get();
            // Workaround for linux systems not adding file extension
            if (!savePath.getFileName().toString().toLowerCase().endsWith(".bib")) {
                savePath = Path.of(savePath + ".bib");
                if (!Files.notExists(savePath) && !dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Overwrite file"),
                        Localization.lang("'%0' exists. Overwrite file?", savePath.getFileName()))) {
                    return Optional.empty();
                }

                selectedPath = Optional.of(savePath);
            }
        }
        return selectedPath;
    }

    private SaveResult save(BibDatabaseContext bibDatabaseContext, SaveDatabaseMode mode, boolean mayAutoCommit) {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (databasePath.isEmpty()) {
            Optional<Path> savePath = askForSavePath();
            return savePath.filter(path -> saveAs(path, mode, mayAutoCommit)).isPresent() ? SaveResult.SUCCESS : SaveResult.FAILURE;
        }

        return save(databasePath.get(), mode, mayAutoCommit);
    }

    private SaveResult save(Path targetPath, SaveDatabaseMode mode, boolean mayAutoCommit) {
        if (mode == SaveDatabaseMode.NORMAL && libraryTab.getBibDatabaseContext().getEntries().size() > 2_000) {
            dialogService.notify("%s...".formatted(Localization.lang("Saving library")));
        }

        synchronized (libraryTab) {
            if (libraryTab.isSaving()) {
                // Another thread is already writing this library; that save is still in flight.
                return SaveResult.ALREADY_SAVING;
            }
            libraryTab.setSaving(true);
        }

        libraryTab.suspendChangeMonitor();

        boolean fileChangedDuringSave = false;
        try {
            Charset encoding = libraryTab.getBibDatabaseContext()
                                         .getMetaData()
                                         .getEncoding()
                                         .orElse(StandardCharsets.UTF_8);

            // Make sure to remember which encoding we used
            libraryTab.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);

            FileSnapshot committedState = saveDatabase(targetPath, false, encoding, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, getSaveOrder(), null);

            libraryTab.getUndoManager().markUnchanged();
            libraryTab.resetChangedProperties(committedState);
            if (mayAutoCommit) {
                commitToGit(targetPath);
            }
            dialogService.notify(Localization.lang("Library saved"));
            return SaveResult.SUCCESS;
        } catch (SaveException ex) {
            if (ex.getCause() instanceof FileChangedException) {
                LOGGER.info("Library {} was modified by another program while saving; save aborted", targetPath, ex);
                fileChangedDuringSave = true;
            } else {
                LOGGER.error("A problem occurred when trying to save the file {}", targetPath, ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
            return SaveResult.FAILURE;
        } finally {
            libraryTab.resumeChangeMonitor();
            // release panel from save status
            libraryTab.setSaving(false);
            if (fileChangedDuringSave) {
                // resumeChangeMonitor() above already scans for the concurrent write and offers the review flow
                dialogService.notify(Localization.lang("Library was not saved: the file was modified by another program."));
            }
        }
    }

    private void commitToGit(Path targetPath) {
        BibDatabaseContext databaseContext = libraryTab.getBibDatabaseContext();
        if (!databaseContext.getMetaData().isGitAutoCommit()) {
            return;
        }
        GitAutoSync gitAutoSync = new GitAutoSync(dialogService,
                gitHandlerRegistry,
                taskExecutor,
                preferences,
                stateManager);
        if (databaseContext.getMetaData().isGitAutoPush()) {
            gitAutoSync.commitAndPush(targetPath, databaseContext);
        } else {
            gitAutoSync.commit(targetPath, databaseContext);
        }
    }

    /// @return the state of the file as committed by this save (or by its encoding retry), `null` when unreadable
    @Nullable
    private FileSnapshot saveDatabase(Path file, boolean selectedOnly, Charset encoding, BibDatabaseWriter.SaveType saveType, SelfContainedSaveOrder saveOrder, @Nullable FileSnapshot expectedState) throws SaveException {
        // if this code is adapted, please also adapt org.jabref.logic.autosaveandbackup.BackupManager.performBackup
        SelfContainedSaveConfiguration saveConfiguration
                = new SelfContainedSaveConfiguration(saveOrder, false, saveType, preferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
        BibDatabaseContext bibDatabaseContext = libraryTab.getBibDatabaseContext();
        synchronized (bibDatabaseContext) {
            Set<Character> encodingProblems = Set.of();
            AtomicFileWriter fileWriter = createFileWriter(file, encoding, saveConfiguration.shouldMakeBackup(), expectedState);
            try (fileWriter) {
                BibWriter bibWriter = new BibWriter(fileWriter, bibDatabaseContext.getDatabase().getNewLineSeparator());
                BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        preferences.getFieldPreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        entryTypesManager)
                        .withJournalAbbreviationRepository(
                                journalAbbreviationRepository,
                                preferences.getAbbreviationPreferences().shouldUseFJournalField());

                if (selectedOnly) {
                    databaseWriter.writePartOfDatabase(bibDatabaseContext, libraryTab.getSelectedEntries());
                } else {
                    databaseWriter.writeDatabase(bibDatabaseContext);
                }

                libraryTab.registerUndoableChanges(databaseWriter.getSaveActionsFieldChanges());

                encodingProblems = fileWriter.getEncodingProblems();
            } catch (UnsupportedCharsetException ex) {
                throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()), ex);
            } catch (IOException ex) {
                throw new SaveException("Problems saving: " + ex, ex);
            }
            FileSnapshot committedState = fileWriter.getCommittedTargetFileState();
            // Deliberately outside the try-with-resources: the retry must run after the writer above committed its
            // content, otherwise the writer's close() would overwrite the re-encoded file with the problematic one
            if (!encodingProblems.isEmpty()) {
                FileSnapshot retriedState = saveWithDifferentEncoding(file, selectedOnly, encoding, encodingProblems, saveType, saveOrder, committedState);
                if (retriedState != null) {
                    committedState = retriedState;
                }
            }
            return committedState;
        }
    }

    private static AtomicFileWriter createFileWriter(Path file, Charset encoding, boolean keepBackup, @Nullable FileSnapshot expectedState) throws SaveException {
        try {
            return new AtomicFileWriter(file, encoding, keepBackup, expectedState);
        } catch (IOException ex) {
            throw new SaveException("Problems saving: " + ex, ex);
        }
    }

    /// @return the state committed by the retry, or `null` when no retry happened and the first write stands
    @Nullable
    private FileSnapshot saveWithDifferentEncoding(Path file, boolean selectedOnly, Charset encoding, Set<Character> encodingProblems, BibDatabaseWriter.SaveType saveType, SelfContainedSaveOrder saveOrder, @Nullable FileSnapshot committedState) throws SaveException {
        DialogPane pane = new DialogPane();
        VBox vbox = new VBox();
        vbox.getChildren().addAll(
                new Text(Localization.lang("The chosen encoding '%0' could not encode the following characters:", encoding.displayName())),
                new Text(encodingProblems.stream().map(Object::toString).collect(Collectors.joining("."))),
                new Text(Localization.lang("What do you want to do?"))
        );
        pane.setContent(vbox);

        ButtonType tryDifferentEncoding = new ButtonType(Localization.lang("Try different encoding"), ButtonBar.ButtonData.OTHER);
        ButtonType ignore = new ButtonType(Localization.lang("Ignore"), ButtonBar.ButtonData.APPLY);
        boolean saveWithDifferentEncoding = dialogService
                .showCustomDialogAndWait(Localization.lang("Save library"), pane, ignore, tryDifferentEncoding)
                .filter(buttonType -> buttonType.equals(tryDifferentEncoding))
                .isPresent();
        if (saveWithDifferentEncoding) {
            Optional<Charset> newEncoding = dialogService.showChoiceDialogAndWait(
                    Localization.lang("Save library"),
                    Localization.lang("Select new encoding"),
                    Localization.lang("Save library"),
                    encoding,
                    OS.ENCODINGS);
            if (newEncoding.isPresent()) {
                // Make sure to remember which encoding we used.
                libraryTab.getBibDatabaseContext().getMetaData().setEncoding(newEncoding.get(), ChangePropagation.DO_NOT_POST_EVENT);

                // The committed state of the first write is the retry's expected baseline, so its writer detects a
                // save that another program completed while the dialogs above were open
                return saveDatabase(file, selectedOnly, newEncoding.get(), saveType, saveOrder, committedState);
            }
        }
        // No retry happened ("Ignore", or the encoding choice was cancelled), so the first write is the result of the
        // save — unless another program overwrote it while the dialogs were open, in which case reporting success
        // would mark the library as saved although the file on disk no longer contains it
        if (committedState != null && !committedState.matches(file)) {
            throw new SaveException(new FileChangedException(file));
        }
        return null;
    }
}
