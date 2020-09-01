package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.autosaveandbackup.AutosaveManager;
import org.jabref.logic.autosaveandbackup.BackupManager;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.JabRefPreferences;

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

    private final BasePanel panel;
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;

    public enum SaveDatabaseMode {
        SILENT, NORMAL
    }

    public SaveDatabaseAction(BasePanel panel, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager) {
        this.panel = panel;
        this.frame = panel.frame();
        this.dialogService = frame.getDialogService();
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;
    }

    public boolean save() {
        return save(panel.getBibDatabaseContext(), SaveDatabaseMode.NORMAL);
    }

    public boolean save(SaveDatabaseMode mode) {
        return save(panel.getBibDatabaseContext(), mode);
    }

    /**
     * Asks the user for the path and saves afterwards
     */
    public void saveAs() {
        askForSavePath().ifPresent(this::saveAs);
    }

    public boolean saveAs(Path file) {
        return this.saveAs(file, SaveDatabaseMode.NORMAL);
    }

    public void saveSelectedAsPlain() {
        askForSavePath().ifPresent(path -> {
            try {
                saveDatabase(path, true, preferences.getDefaultEncoding(), SavePreferences.DatabaseSaveType.PLAIN_BIBTEX);
                frame.getFileHistory().newFile(path);
                dialogService.notify(Localization.lang("Saved selected to '%0'.", path.toString()));
            } catch (SaveException ex) {
                LOGGER.error("A problem occurred when trying to save the file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
        });
    }

    /**
     * @param file the new file name to save the data base to. This is stored in the database context of the panel upon
     *             successful save.
     * @return true on successful save
     */
    boolean saveAs(Path file, SaveDatabaseMode mode) {
        BibDatabaseContext context = panel.getBibDatabaseContext();

        // Close AutosaveManager and BackupManager for original library
        Optional<Path> databasePath = context.getDatabasePath();
        if (databasePath.isPresent()) {
            final Path oldFile = databasePath.get();
            context.setDatabasePath(oldFile);
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context);
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
            // thus, we can store the store the path into the context
            context.setDatabasePath(file);
            frame.refreshTitleAndTabs();

            // Reinstall AutosaveManager and BackupManager for the new file name
            panel.resetChangeMonitorAndChangePane();
            if (readyForAutosave(context)) {
                AutosaveManager autosaver = AutosaveManager.start(context);
                autosaver.registerListener(new AutosaveUiManager(panel));
            }
            if (readyForBackup(context)) {
                BackupManager.start(context, entryTypesManager, preferences);
            }

            frame.getFileHistory().newFile(file);
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
                .withInitialDirectory(preferences.get(JabRefPreferences.WORKING_DIRECTORY))
                .build();
        Optional<Path> selectedPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        selectedPath.ifPresent(path -> preferences.setWorkingDir(path.getParent()));
        return selectedPath;
    }

    private boolean save(BibDatabaseContext bibDatabaseContext, SaveDatabaseMode mode) {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (databasePath.isEmpty()) {
            Optional<Path> savePath = askForSavePath();
            if (!savePath.isPresent()) {
                return false;
            }
            return saveAs(savePath.get(), mode);
        }

        return save(databasePath.get(), mode);
    }

    private boolean save(Path targetPath, SaveDatabaseMode mode) {
        if (mode == SaveDatabaseMode.NORMAL) {
            dialogService.notify(String.format("%s...", Localization.lang("Saving library")));
        }

        panel.setSaving(true);
        try {
            Charset encoding = panel.getBibDatabaseContext()
                                    .getMetaData()
                                    .getEncoding()
                                    .orElse(preferences.getDefaultEncoding());
            // Make sure to remember which encoding we used.
            panel.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);

            // Save the database
            boolean success = saveDatabase(targetPath, false, encoding, SavePreferences.DatabaseSaveType.ALL);

            if (success) {
                panel.getUndoManager().markUnchanged();
                // After a successful save the following statement marks that the base is unchanged since last save
                panel.setNonUndoableChange(false);
                panel.setBaseChanged(false);

                frame.setTabTitle(panel, panel.getTabTitle(), targetPath.toAbsolutePath().toString());
                frame.setWindowTitle();
                frame.updateAllTabTitles();
            }
            return success;
        } catch (SaveException ex) {
            LOGGER.error(String.format("A problem occurred when trying to save the file %s", targetPath), ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            return false;
        } finally {
            // release panel from save status
            panel.setSaving(false);
        }
    }

    private boolean saveDatabase(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType) throws SaveException {
        SavePreferences preferences = this.preferences.getSavePreferences()
                                                      .withEncoding(encoding)
                                                      .withSaveType(saveType);
        try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, preferences.getEncoding(), preferences.shouldMakeBackup())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, preferences, entryTypesManager);

            if (selectedOnly) {
                databaseWriter.savePartOfDatabase(panel.getBibDatabaseContext(), panel.getSelectedEntries());
            } else {
                databaseWriter.saveDatabase(panel.getBibDatabaseContext());
            }

            panel.registerUndoableChanges(databaseWriter.getSaveActionsFieldChanges());

            if (fileWriter.hasEncodingProblems()) {
                saveWithDifferentEncoding(file, selectedOnly, preferences.getEncoding(), fileWriter.getEncodingProblems(), saveType);
            }
        } catch (UnsupportedCharsetException ex) {
            throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()), ex);
        } catch (IOException ex) {
            throw new SaveException("Problems saving: " + ex, ex);
        }

        return true;
    }

    private void saveWithDifferentEncoding(Path file, boolean selectedOnly, Charset encoding, Set<Character> encodingProblems, SavePreferences.DatabaseSaveType saveType) throws SaveException {
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
                panel.getBibDatabaseContext().getMetaData().setEncoding(newEncoding.get(), ChangePropagation.DO_NOT_POST_EVENT);

                saveDatabase(file, selectedOnly, newEncoding.get(), saveType);
            }
        }
    }

    private boolean readyForAutosave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED) ||
                ((context.getLocation() == DatabaseLocation.LOCAL)
                        && preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)))
                &&
                context.getDatabasePath().isPresent();
    }

    private boolean readyForBackup(BibDatabaseContext context) {
        return (context.getLocation() == DatabaseLocation.LOCAL) && context.getDatabasePath().isPresent();
    }
}
