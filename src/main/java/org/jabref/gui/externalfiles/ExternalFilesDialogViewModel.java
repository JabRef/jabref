package org.jabref.gui.externalfiles;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class ExternalFilesDialogViewModel {

    private final ImportHandler importHandler;
    private final StringProperty directoryPath = new SimpleStringProperty("");
    private final List<FileChooser.ExtensionFilter> fileFilterList = List.of(
                                                                             FileFilterConverter.ANY_FILE,
                                                                             FileFilterConverter.toExtensionFilter(StandardFileType.PDF),
                                                                             FileFilterConverter.toExtensionFilter(StandardFileType.BIBTEX_DB));
    private final DialogService dialogService;
    private final PreferencesService preferences;

    public ExternalFilesDialogViewModel(DialogService dialogService, ExternalFileTypes externalFileTypes, UndoManager undoManager,
                                        FileUpdateMonitor fileUpdateMonitor, PreferencesService preferences, StateManager stateManager) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        importHandler = new ImportHandler(
                                          dialogService,
                                          stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null")),
                                          externalFileTypes,
                                          preferences,
                                          fileUpdateMonitor,
                                          undoManager,
                                          stateManager);
    }

    public void startImport() {

    }

    public void startExport() {

    }

    public StringProperty directoryPath() {
        return this.directoryPath;
    }

    public List<FileChooser.ExtensionFilter> getFileFilters() {
        return this.fileFilterList;
    }

    public void browseFileDirectory() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
            .withInitialDirectory(preferences.getWorkingDir()).build();

        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                 .ifPresent(selectedDirectory -> {
                     directoryPath.setValue(selectedDirectory.toAbsolutePath().toString());
                     preferences.setWorkingDirectory(selectedDirectory.toAbsolutePath());
                 });
    }
}
