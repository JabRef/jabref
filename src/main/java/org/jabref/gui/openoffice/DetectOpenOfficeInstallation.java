package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
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

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Tools for automatically detecting OpenOffice or LibreOffice installations.
 */
public class DetectOpenOfficeInstallation extends AbstractWorker {

    private final OpenOfficePreferences preferences;

    private final JDialog parent;
    private boolean foundPaths;
    private JDialog progressDialog;

    public DetectOpenOfficeInstallation(JDialog parent, OpenOfficePreferences preferences) {
        this.parent = parent;
        this.preferences = preferences;
    }

    public boolean isInstalled() {
        foundPaths = false;
        if (this.checkAutoDetectedPaths(preferences)) {
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
        JOptionPane.showMessageDialog(parent,
                Localization.lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually."),
                Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                JOptionPane.INFORMATION_MESSAGE);

        DialogService ds = new FXDialogService();
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(nativeDesktop.getApplicationDirectory()).build();
        Optional<Path> path = DefaultTaskExecutor.runInJavaFXThread(() -> ds.showDirectorySelectionDialog(dirDialogConfiguration));

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
        // Otherwise more than one installation was found, select among them
        DefaultListModel<File> mod = new DefaultListModel<>();
        for (Path tmpfile : installDirs) {
            mod.addElement(tmpfile.toFile());
        }
        JList<File> fileList = new JList<>(mod);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setSelectedIndex(0);
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("pref:grow", "pref, 2dlu, pref, 4dlu, pref"));
        builder.add(Localization.lang("Found more than one OpenOffice/LibreOffice executable.")).xy(1, 1);
        builder.add(Localization.lang("Please choose which one to connect to:")).xy(1, 3);
        builder.add(fileList).xy(1, 5);
        int answer = JOptionPane.showConfirmDialog(null, builder.getPanel(),
                Localization.lang("Choose OpenOffice/LibreOffice executable"), JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.CANCEL_OPTION) {
            return Optional.empty();
        } else {
            return Optional.of(fileList.getSelectedValue().toPath());
        }
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
