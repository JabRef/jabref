package org.jabref.gui.openoffice;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.openoffice.OpenOfficePreferences;

public class ManualConnectDialogViewModel {

    private final StringProperty ooPath = new SimpleStringProperty("");
    private final StringProperty ooExec = new SimpleStringProperty("");
    private final StringProperty ooJars = new SimpleStringProperty("");
    private final DialogService dialogService;
    private final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();
    private final FileDialogConfiguration fileDialogConfiguration;
    private final DirectoryDialogConfiguration dirDialogConfiguration;

    public ManualConnectDialogViewModel(OpenOfficePreferences preferences, DialogService dialogService) {
        this.dialogService = dialogService;

        ooPathProperty().setValue(preferences.getInstallationPath());
        ooExecProperty().setValue(preferences.getExecutablePath());
        ooJarsProperty().setValue(preferences.getJarsPath());

        dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                                                                           .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                           .build();
        fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                       .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                       .build();
    }

    public void browseOOPath() {
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(path -> ooPath.setValue(path.toAbsolutePath().toString()));
    }

    public void browseOOExec() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(f -> ooExec.setValue(f.toAbsolutePath().toString()));

    }

    public void browseOOJars() {
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration).ifPresent(path -> ooJars.setValue(path.toAbsolutePath().toString()));
    }

    public StringProperty ooPathProperty() {
        return ooPath;
    }

    public StringProperty ooExecProperty() {
        return ooExec;
    }

    public StringProperty ooJarsProperty() {
        return ooJars;
    }

}
