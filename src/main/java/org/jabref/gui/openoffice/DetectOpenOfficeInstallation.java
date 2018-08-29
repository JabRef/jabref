package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;

/**
 * Tools for automatically detecting OpenOffice or LibreOffice installations.
 */
public class DetectOpenOfficeInstallation {

    private final OpenOfficePreferences preferences;
    private final JDialog parent;
    private final DialogService dialogService;

    private JDialog progressDialog;

    public DetectOpenOfficeInstallation(JDialog parent, OpenOfficePreferences preferences, DialogService dialogService) {
        this.parent = parent;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    public Future<Boolean> isInstalled() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (this.checkAutoDetectedPaths(preferences)) {
            future.complete(true);
        } else {
            init();
            BackgroundTask.wrap(() -> future.complete(autoDetectPaths()))
                          .onSuccess(x -> SwingUtilities.invokeLater(progressDialog::dispose))
                          .executeWith(Globals.TASK_EXECUTOR);
        }
        return future;
    }

    public void init() {
        progressDialog = showProgressDialog(parent, Localization.lang("Autodetecting paths..."),
                Localization.lang("Please wait..."));
    }

    private Optional<Path> selectInstallationPath() {

        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        Optional<Path> path = DefaultTaskExecutor.runInJavaFXThread(() -> {
            dialogService.showInformationDialogAndWait(Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                    Localization.lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually."));
            DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                    .build();
            return dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        });

        return path;
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
        return ((executablePath != null) && Files.exists(Paths.get(executablePath)));
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

        Optional<Path> selectedPath = DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showChoiceDialogAndWait(
                Localization.lang("Choose OpenOffice/LibreOffice executable"),
                content, Localization.lang("Use selected instance"), installDirs));

        return selectedPath;
    }

    public JDialog showProgressDialog(JDialog progressParent, String title, String message) {
        JProgressBar bar = new JProgressBar(SwingConstants.HORIZONTAL);
        final JDialog progressDialog = new JDialog(progressParent, title, false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bar.setIndeterminate(true);
        progressDialog.add(new JLabel(message), BorderLayout.NORTH);
        progressDialog.add(bar, BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setVisible(true);
        return progressDialog;
    }
}
