package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;

/**
 * Tools for automatically detecting OpenOffice or LibreOffice installations.
 */
public class DetectOpenOfficeInstallation extends AbstractWorker {

    private final OpenOfficePreferences preferences;
    private final JDialog parent;
    private final DialogService dialogService;

    private boolean foundPaths;
    private JDialog progressDialog;

    public DetectOpenOfficeInstallation(JDialog parent, OpenOfficePreferences preferences, DialogService dialogService) {
        this.parent = parent;
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    public boolean isInstalled() {
        foundPaths = false;
        if (preferences.checkAutoDetectedPaths()) {
            return true;
        }
        init();
        getWorker().run();
        update();
        return foundPaths;
    }

    @Override
    public void run() {
        foundPaths = autoDetectPaths();
    }

    @Override
    public void init() {
        progressDialog = showProgressDialog(parent, Localization.lang("Autodetecting paths..."),
                Localization.lang("Please wait..."));
    }

    @Override
    public void update() {
        progressDialog.dispose();
    }

    private Optional<Path> selectInstallationPath() {

        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showInformationDialogAndWait(Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                Localization.lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually.")));
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                .build();
        Optional<Path> path = DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration));

        if (path.isPresent()) {
            return path;
        }
        return Optional.empty();
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
            preferences.setOOPath(installDir.toString());
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
                content, Localization.lang("Use selected instance"), null, installDirs));

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
