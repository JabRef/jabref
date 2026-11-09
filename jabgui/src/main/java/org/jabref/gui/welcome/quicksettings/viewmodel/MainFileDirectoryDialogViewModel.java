package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.nio.file.Path;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.strings.StringUtil;

public class MainFileDirectoryDialogViewModel extends AbstractViewModel {
    private final StringProperty pathProperty = new SimpleStringProperty("");
    // A blank path would be saved as an empty relative path and disable storing files relative to the library
    private final BooleanBinding pathValid = Bindings.createBooleanBinding(() -> StringUtil.isNotBlank(pathProperty.get()), pathProperty);

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

    public BooleanBinding pathValidProperty() {
        return pathValid;
    }

    public void browseForDirectory() {
        DirectoryDialogConfiguration dirConfig = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();

        dialogService.showDirectorySelectionDialog(dirConfig)
                     .ifPresent(selectedDir -> pathProperty.set(selectedDir.toString()));
    }

    public void saveSettings() {
        filePreferences.setMainFileDirectory(Path.of(pathProperty.get()));
        filePreferences.setStoreFilesRelativeToBibFile(false);
    }
}
