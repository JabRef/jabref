package org.jabref.gui.filewizard;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.preferences.PreferencesService;

public class FileWizardViewModel {
    private final StringProperty directory = new SimpleStringProperty("");
    private final DialogService dialogService;
    private final DirectoryDialogConfiguration directoryDialogConfiguration;

    public FileWizardViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir()).build();
    }

    public void browseFileDirectory() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                .ifPresent(dir -> directory.setValue(dir.toAbsolutePath().toString()));
    }

    public StringProperty getDirectoryProperty() {
        return this.directory;
    }
}
