package org.jabref.gui.openoffice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressIndicator;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialog;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.strings.StringUtil;

/**
 * Tools for automatically detecting OpenOffice or LibreOffice installations.
 */
public class DetectOpenOfficeInstallation {

    private final OpenOfficePreferences preferences;
    private final DialogService dialogService;

    public DetectOpenOfficeInstallation(OpenOfficePreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    public boolean isInstalled() {
        return autoDetectPaths();
    }

    public boolean isExecutablePathDefined() {
        return checkAutoDetectedPaths(preferences);
    }

    public FXDialog initProgressDialog() {
        DialogPane dialogPane = new DialogPane();
        ProgressIndicator indicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        dialogPane.setContent(indicator);
        FXDialog progressDialog = dialogService.showCustomDialog(Localization.lang("Autodetecting paths..."), dialogPane, ButtonType.CANCEL);
        progressDialog.show();

        return progressDialog;
    }

    private Optional<Path> selectInstallationPath() {

        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        dialogService.showInformationDialogAndWait(Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                                                   Localization.lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually."));
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                                                                                                        .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                                                        .build();
        return dialogService.showDirectorySelectionDialog(dirDialogConfiguration);

    }

    private boolean autoDetectPaths() {
        List<Path> installations = OpenOfficeFileSearch.detectInstallations();

        // manually add installation path
        if (installations.isEmpty()) {
            selectInstallationPath().ifPresent(installations::add);
        }

        // select among multiple installations
        Optional<Path> actualFile = chooseAmongInstallations(installations);
        if (actualFile.isPresent()) {
            return setOpenOfficePreferences(actualFile.get());
        }

        return false;
    }

    /**
     * Checks whether the executablePath exists
     */
    private boolean checkAutoDetectedPaths(OpenOfficePreferences openOfficePreferences) {
        String executablePath = openOfficePreferences.getExecutablePath();
        return !StringUtil.isNullOrEmpty(executablePath) && Files.exists(Paths.get(executablePath));
    }

    private boolean setOpenOfficePreferences(Path installDir) {
        Optional<Path> execPath = Optional.empty();

        if (OS.WINDOWS) {
            execPath = FileUtil.find(OpenOfficePreferences.WINDOWS_EXECUTABLE, installDir);
        } else if (OS.OS_X) {
            execPath = FileUtil.find(OpenOfficePreferences.OSX_EXECUTABLE, installDir);
        } else if (OS.LINUX) {
            execPath = FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, installDir);
        }

        Optional<Path> jarFilePath = FileUtil.find(OpenOfficePreferences.OO_JARS.get(0), installDir);

        if (execPath.isPresent() && jarFilePath.isPresent()) {
            preferences.setInstallationPath(installDir.toString());
            preferences.setExecutablePath(execPath.get().toString());
            preferences.setJarsPath(jarFilePath.get().getParent().toString());
            return true;
        }

        return false;
    }

    private Optional<Path> chooseAmongInstallations(List<Path> installDirs) {
        if (installDirs.isEmpty()) {
            return Optional.empty();
        }

        if (installDirs.size() == 1) {
            return Optional.of(installDirs.get(0).toAbsolutePath());
        }

        String content = Localization.lang("Found more than one OpenOffice/LibreOffice executable.")
                         + "\n" + Localization.lang("Please choose which one to connect to:");

        Optional<Path> selectedPath = dialogService.showChoiceDialogAndWait(Localization.lang("Choose OpenOffice/LibreOffice executable"),
                                                                            content, Localization.lang("Use selected instance"), installDirs);

        return selectedPath;
    }

}
