package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.nio.file.Path;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;

public class MainFileDirectoryDialogViewModel extends AbstractViewModel {
    private final StringProperty pathProperty = new SimpleStringProperty("");

    private final FilePreferences filePreferences;
    private final DialogService dialogService;

    public MainFileDirectoryDialogViewModel(GuiPreferences preferences, DialogService dialogService) {
        this.filePreferences = preferences.getFilePreferences();
        this.dialogService = dialogService;

        pathProperty.set(filePreferences.getMainFileDirectory()
                                        .map(Path::toString)
                                        .orElse(""));
    }

    public StringProperty pathProperty() {
        return pathProperty;
    }

    public String getPath() {
        return pathProperty.get();
    }

    public void setPath(String path) {
        pathProperty.set(path);
    }

    public void browseForDirectory() {
        DirectoryDialogConfiguration dirConfig = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        dialogService.showDirectorySelectionDialog(dirConfig)
                     .ifPresent(selectedDir -> setPath(selectedDir.toString()));
    }

    public void saveSettings() {
        filePreferences.setMainFileDirectory(getPath());
        filePreferences.setStoreFilesRelativeToBibFile(false);
    }
}
