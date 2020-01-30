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
import org.jabref.gui.dialogs.AutosaveUIManager;
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
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.ChangePropagation;
import org.jabref.model.database.shared.DatabaseLocation;
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

    public enum SaveDatabaseMode {
        SILENT, NORMAL
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveDatabaseAction.class);

    private final BasePanel panel;
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final JabRefPreferences prefs;
    private final BibEntryTypesManager entryTypesManager;

    public SaveDatabaseAction(BasePanel panel, JabRefPreferences prefs, BibEntryTypesManager entryTypesManager) {
        this.panel = panel;
        this.frame = panel.frame();
        this.dialogService = frame.getDialogService();
        this.prefs = prefs;
        this.entryTypesManager = entryTypesManager;
    }

    private boolean saveDatabase(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType) throws SaveException {
        SavePreferences preferences = prefs.loadForSaveFromPreferences()
                                           .withEncoding(encoding)
                                           .withSaveType(saveType);
        try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, preferences.getEncoding(), preferences.makeBackup())) {
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
            throw new SaveException("Problems saving:", ex);
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
        boolean saveWithDifferentEncoding = frame.getDialogService()
                                                 .showCustomDialogAndWait(Localization.lang("Save library"), pane, ignore, tryDifferentEncoding)
                                                 .filter(buttonType -> buttonType.equals(tryDifferentEncoding))
                                                 .isPresent();
        if (saveWithDifferentEncoding) {
            Optional<Charset> newEncoding = frame.getDialogService().showChoiceDialogAndWait(Localization.lang("Save library"), Localization.lang("Select new encoding"), Localization.lang("Save library"), encoding, Encodings.getCharsets());
            if (newEncoding.isPresent()) {
                // Make sure to remember which encoding we used.
                panel.getBibDatabaseContext().getMetaData().setEncoding(newEncoding.get(), ChangePropagation.DO_NOT_POST_EVENT);

                saveDatabase(file, selectedOnly, newEncoding.get(), saveType);
            }
        }
    }

    private boolean doSave() {
        panel.setSaving(true);
        Path targetPath = panel.getBibDatabaseContext().getDatabasePath().get();
        try {
            Charset encoding = panel.getBibDatabaseContext()
                                    .getMetaData()
                                    .getEncoding()
                                    .orElse(prefs.getDefaultEncoding());
            // Make sure to remember which encoding we used.
            panel.getBibDatabaseContext().getMetaData().setEncoding(encoding, ChangePropagation.DO_NOT_POST_EVENT);

            // Save the database
            boolean success = saveDatabase(targetPath, false, encoding, SavePreferences.DatabaseSaveType.ALL);

            if (success) {
                panel.getUndoManager().markUnchanged();
                // (Only) after a successful save the following
                // statement marks that the base is unchanged
                // since last save:
                panel.setNonUndoableChange(false);
                panel.setBaseChanged(false);

                // Reset title of tab
                frame.setTabTitle(panel, panel.getTabTitle(),
                        panel.getBibDatabaseContext().getDatabasePath().get().toAbsolutePath().toString());
                frame.setWindowTitle();
                frame.updateAllTabTitles();
            }
            return success;
        } catch (SaveException ex) {
            LOGGER.error("A problem occurred when trying to save the file " + targetPath, ex);
            frame.getDialogService().showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            return false;
        } finally {
            // release panel from save status
            panel.setSaving(false);
        }
    }

    public boolean save() {
        return save(SaveDatabaseMode.NORMAL);
    }

    public boolean save(SaveDatabaseMode mode) {
        if (panel.getBibDatabaseContext().getDatabasePath().isPresent()) {
            if (mode == SaveDatabaseMode.NORMAL) {
                panel.frame().getDialogService().notify(Localization.lang("Saving library") + "...");
            }
            return doSave();
        } else {
            Optional<Path> savePath = getSavePath();
            if (savePath.isPresent()) {
                saveAs(savePath.get());
                return true;
            }
        }

        return false;
    }

    public void saveAs() {
        getSavePath().ifPresent(this::saveAs);
    }

    private Optional<Path> getSavePath() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.BIBTEX_DB)
                .withDefaultExtension(StandardFileType.BIBTEX_DB)
                .withInitialDirectory(prefs.get(JabRefPreferences.WORKING_DIRECTORY))
                .build();
        Optional<Path> selectedPath = dialogService.showFileSaveDialog(fileDialogConfiguration);
        selectedPath.ifPresent(path -> prefs.setWorkingDir(path.getParent()));
        return selectedPath;
    }

    public void saveAs(Path file) {
        BibDatabaseContext context = panel.getBibDatabaseContext();

        // Close AutosaveManager and BackupManager for original library
        Optional<Path> databasePath = context.getDatabasePath();
        if (databasePath.isPresent()) {
            final Path oldFile = databasePath.get();
            context.setDatabaseFile(oldFile.toFile());
            AutosaveManager.shutdown(context);
            BackupManager.shutdown(context);
        }

        // Set new location
        if (context.getLocation() == DatabaseLocation.SHARED) {
            // Save all properties dependent on the ID. This makes it possible to restore them.
            new SharedDatabasePreferences(context.getDatabase().generateSharedDatabaseID())
                    .putAllDBMSConnectionProperties(context.getDBMSSynchronizer().getConnectionProperties());
        }
        context.setDatabaseFile(file);

        // Save
        save();

        // Reinstall AutosaveManager and BackupManager
        panel.resetChangeMonitorAndChangePane();
        if (readyForAutosave(context)) {
            AutosaveManager autosaver = AutosaveManager.start(context);
            autosaver.registerListener(new AutosaveUIManager(panel));
        }
        if (readyForBackup(context)) {
            BackupManager.start(context, entryTypesManager, prefs);
        }

        context.getDatabasePath().ifPresent(presentFile -> frame.getFileHistory().newFile(presentFile));
    }

    private boolean readyForAutosave(BibDatabaseContext context) {
        return ((context.getLocation() == DatabaseLocation.SHARED) ||
                ((context.getLocation() == DatabaseLocation.LOCAL)
                        && prefs.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)))
                &&
                context.getDatabasePath().isPresent();
    }

    private boolean readyForBackup(BibDatabaseContext context) {
        return (context.getLocation() == DatabaseLocation.LOCAL) && context.getDatabasePath().isPresent();
    }

    public void saveSelectedAsPlain() {
        getSavePath().ifPresent(path -> {
            try {
                saveDatabase(path, true, prefs.getDefaultEncoding(), SavePreferences.DatabaseSaveType.PLAIN_BIBTEX);
                frame.getFileHistory().newFile(path);
                frame.getDialogService().notify(Localization.lang("Saved selected to '%0'.", path.toString()));
            } catch (SaveException ex) {
                LOGGER.error("A problem occurred when trying to save the file", ex);
                frame.getDialogService().showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
            }
        });
    }
}
