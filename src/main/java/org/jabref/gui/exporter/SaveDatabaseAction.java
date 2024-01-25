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
import org.jabref.gui.autosaveandbackup.AutosaveManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.PdfIndexerManager;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the "Save" and "Save as" operations called from BasePanel. This class is also used for save operations
 * when closing a database or quitting the applications.
 * <p>
 * The save operation is loaded off of the GUI thread using {@link BackgroundTask}. Callers can query whether the
 * operation was canceled, or whether it was successful.
 */
public class SaveDatabaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveDatabaseAction.class);

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final BibEntryTypesManager entryTypesManager;

    public enum SaveDatabaseMode {
        SILENT, NORMAL
    }

    public SaveDatabaseAction(LibraryTab libraryTab,
                              DialogService dialogService,
                              PreferencesService preferences,
                              BibEntryTypesManager entryTypesManager) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
    }

    public boolean save() {
        return save(libraryTab.getBibDatabaseContext(), SaveDatabaseMode.NORMAL);
    }

    public boolean save(SaveDatabaseMode mode) {
        return save(libraryTab.getBibDatabaseContext(), mode);
    }

    /**
     * Asks the user for the path and saves afterward
     */
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
                saveDatabase(path, true, StandardCharsets.UTF_8, BibDatabaseWriter.SaveType.PLAIN_BIBTEX, getSaveOrder());
                preferences.getGuiPreferences().getFileHistory().newFile(path);
                dialogService.notify(Localization.lang("Saved selected to '%0'.", path.toString()));
            } catch (SaveException ex) {
                LOGGER.error("A problem occurred when trying to save the file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
        });
    }

    /**
     * @param file the new file name to save the database to. This is stored in the database context of the panel upon
     *             successful save.
     * @return true on successful save
     */
    boolean saveAs(Path file, SaveDatabaseMode mode) {
        BibDatabaseContext context = libraryTab.getBibDatabaseContext();

        Optional<Path> databasePath = context.getDatabasePath();
        if (databasePath.isPresent()) {
            // Close AutosaveManager, BackupManager, and PdfIndexer for original library
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context, this.preferences.getFilePreferences().getBackupDirectory(), preferences.getFilePreferences().shouldCreateBackup());
            PdfIndexerManager.shutdownIndexer(context);
        }

        // Set new location
        if (context.getLocation() == DatabaseLocation.SHARED) {
            // Save all properties dependent on the ID. This makes it possible to restore them.
            new SharedDatabasePreferences(context.getDatabase().generateSharedDatabaseID())
                    .putAllDBMSConnectionProperties(context.getDBMSSynchronizer().getConnectionProperties());
        }

        boolean saveResult = save(file, mode);

        if (saveResult) {
            // we managed to successfully save the file
            // thus, we can store the path into the context
            context.setDatabasePath(file);
            libraryTab.updateTabTitle(false);

            // Reset (here: uninstall and install again) AutosaveManager and BackupManager for the new file name
            libraryTab.resetChangeMonitor();
            libraryTab.installAutosaveManagerAndBackupManager();
            // PdfIndexerManager does not need to be called; the method {@link org.jabref.logic.pdf.search.PdfIndexerManager.get()} is called if a new indexer is needed

            preferences.getGuiPreferences().getFileHistory().newFile(file);
        }

        return saveResult;
    }

    /**
     * Asks the user for the path to save to. Stores the directory to the preferences, which is used next time when
     * opening the dialog.
     *
     * @return the path set by the user
     */
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
                savePath = Path.of(savePath.toString() + ".bib");
                if (!Files.notExists(savePath)) {
                    if (!dialogService.showConfirmationDialogAndWait(Localization.lang("Overwrite file"), Localization.lang("'%0' exists. Overwrite file?", savePath.getFileName()))) {
                        return Optional.empty();
                    }
                }
                selectedPath = Optional.of(savePath);
            }
        }
        return selectedPath;
    }

    private boolean save(BibDatabaseContext bibDatabaseContext, SaveDatabaseMode mode) {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (databasePath.isEmpty()) {
            Optional<Path> savePath = askForSavePath();
            if (savePath.isEmpty()) {
                return false;
            }
            return saveAs(savePath.get(), mode);
        }

        return save(databasePath.get(), mode);
    }

    private boolean save(Path targetPath, SaveDatabaseMode mode) {
        if (mode == SaveDatabaseMode.NORMAL) {
            dialogService.notify("%s...".formatted(Localization.lang("Saving library")));
        }

        synchronized (libraryTab) {
            if (libraryTab.isSaving()) {
                // if another thread is saving, we do not need to save
                return true;
            }
            libraryTab.setSaving(true);
        }

        try {
            Charset encoding = libraryTab.getBibDatabaseContext()
                                         .getMetaData()
                                         .getEncoding()
                                         .orElse(StandardCharsets.UTF_8);

            // Make sure to remember which encoding we used
            libraryTab.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);

            boolean success = saveDatabase(targetPath, false, encoding, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, getSaveOrder());

            if (success) {
                libraryTab.getUndoManager().markUnchanged();
                libraryTab.resetChangedProperties();
            }
            dialogService.notify(Localization.lang("Library saved"));
            return success;
        } catch (SaveException ex) {
            LOGGER.error("A problem occurred when trying to save the file %s".formatted(targetPath), ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            return false;
        } finally {
            // release panel from save status
            libraryTab.setSaving(false);
        }
    }

    private boolean saveDatabase(Path file, boolean selectedOnly, Charset encoding, BibDatabaseWriter.SaveType saveType, SelfContainedSaveOrder saveOrder) throws SaveException {
        // if this code is adapted, please also adapt org.jabref.logic.autosaveandbackup.BackupManager.performBackup
        SelfContainedSaveConfiguration saveConfiguration
                = new SelfContainedSaveConfiguration(saveOrder, false, saveType, preferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
        BibDatabaseContext bibDatabaseContext = libraryTab.getBibDatabaseContext();
        synchronized (bibDatabaseContext) {
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, encoding, saveConfiguration.shouldMakeBackup())) {
                BibWriter bibWriter = new BibWriter(fileWriter, bibDatabaseContext.getDatabase().getNewLineSeparator());
                BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        preferences.getFieldPreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        entryTypesManager);

                if (selectedOnly) {
                    databaseWriter.savePartOfDatabase(bibDatabaseContext, libraryTab.getSelectedEntries());
                } else {
                    databaseWriter.saveDatabase(bibDatabaseContext);
                }

                libraryTab.registerUndoableChanges(databaseWriter.getSaveActionsFieldChanges());

                if (fileWriter.hasEncodingProblems()) {
                    saveWithDifferentEncoding(file, selectedOnly, encoding, fileWriter.getEncodingProblems(), saveType, saveOrder);
                }
            } catch (UnsupportedCharsetException ex) {
                throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()), ex);
            } catch (IOException ex) {
                throw new SaveException("Problems saving: " + ex, ex);
            }
            return true;
        }
    }

    private void saveWithDifferentEncoding(Path file, boolean selectedOnly, Charset encoding, Set<Character> encodingProblems, BibDatabaseWriter.SaveType saveType, SelfContainedSaveOrder saveOrder) throws SaveException {
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
            Optional<Charset> newEncoding = dialogService.showChoiceDialogAndWait(Localization.lang("Save library"), Localization.lang("Select new encoding"), Localization.lang("Save library"), encoding, Encodings.getCharsets());
            if (newEncoding.isPresent()) {
                // Make sure to remember which encoding we used.
                libraryTab.getBibDatabaseContext().getMetaData().setEncoding(newEncoding.get(), ChangePropagation.DO_NOT_POST_EVENT);

                saveDatabase(file, selectedOnly, newEncoding.get(), saveType, saveOrder);
            }
        }
    }
}
