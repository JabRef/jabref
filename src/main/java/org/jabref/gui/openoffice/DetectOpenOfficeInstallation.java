package org.jabref.gui.openoffice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.strings.StringUtil;

/**
 * Tools for automatically detecting OpenOffice or LibreOffice installations.
 */
public class DetectOpenOfficeInstallation {

    private final OpenOfficePreferences openOfficePreferences;
    private final DialogService dialogService;

    public DetectOpenOfficeInstallation(OpenOfficePreferences openOfficePreferences, DialogService dialogService) {
        this.dialogService = dialogService;
        this.openOfficePreferences = openOfficePreferences;
    }

    public boolean isExecutablePathDefined() {
        return checkAutoDetectedPaths(openOfficePreferences);
    }

    public Optional<Path> selectInstallationPath() {
        final NativeDesktop nativeDesktop = OS.getNativeDesktop();

        dialogService.showInformationDialogAndWait(Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                Localization.lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually."));
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                .build();
        return dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
    }

    /**
     * Checks whether the executablePath exists
     */
    private boolean checkAutoDetectedPaths(OpenOfficePreferences openOfficePreferences) {
        String executablePath = openOfficePreferences.getExecutablePath();

        if (OS.LINUX && (System.getenv("FLATPAK_SANDBOX_DIR") != null)) {
            executablePath = OpenOfficePreferences.DEFAULT_LINUX_FLATPAK_EXEC_PATH;
        }
        return !StringUtil.isNullOrEmpty(executablePath) && Files.exists(Path.of(executablePath));
    }

    public boolean setOpenOfficePreferences(Path installDir) {
        Optional<Path> execPath = Optional.empty();

        if (OS.WINDOWS) {
            execPath = FileUtil.find(OpenOfficePreferences.WINDOWS_EXECUTABLE, installDir);
        } else if (OS.OS_X) {
            execPath = FileUtil.find(OpenOfficePreferences.OSX_EXECUTABLE, installDir);
        } else if (OS.LINUX) {
            execPath = FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, installDir);
        }

        if (execPath.isPresent()) {
            openOfficePreferences.setExecutablePath(execPath.get().toString());
            return true;
        }

        return false;
    }

    public Optional<Path> chooseAmongInstallations(List<Path> installDirs) {
        if (installDirs.isEmpty()) {
            return Optional.empty();
        }

        if (installDirs.size() == 1) {
            return Optional.of(installDirs.get(0).toAbsolutePath());
        }

        return dialogService.showChoiceDialogAndWait(
                Localization.lang("Choose OpenOffice/LibreOffice executable"),
                Localization.lang("Found more than one OpenOffice/LibreOffice executable.") + "\n"
                        + Localization.lang("Please choose which one to connect to:"),
                Localization.lang("Use selected instance"),
                installDirs);
    }
}
