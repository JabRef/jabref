package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficeFileSearch;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Tools for automatically detecting OpenOffice and/or LibreOffice installations.
 */
public class AutoDetectPaths extends AbstractWorker {

    private final OpenOfficePreferences preferences;

    private boolean foundPaths;
    private boolean fileSearchCanceled;
    private JDialog prog;
    private final JDialog parent;

    private final OpenOfficeFileSearch fileSearch = new OpenOfficeFileSearch();

    public AutoDetectPaths(JDialog parent, OpenOfficePreferences preferences) {
        this.parent = parent;
        this.preferences = preferences;
    }

    public boolean runAutodetection() {
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

    public boolean canceled() {
        return fileSearchCanceled;
    }

    @Override
    public void init() {
        prog = showProgressDialog(parent, Localization.lang("Autodetecting paths..."),
                Localization.lang("Please wait..."), true);
    }

    @Override
    public void update() {
        prog.dispose();
    }

    private boolean autoDetectPaths() {
        if (OS.WINDOWS) {
            List<Path> progFiles = fileSearch.findWindowsProgramFilesDir();
            List<Path> sofficeFiles = FileUtil.find(OpenOfficePreferences.WINDOWS_EXECUTABLE, progFiles);
            if (fileSearchCanceled) {
                return false;
            }
            if (sofficeFiles.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                        Localization
                                .lang("Unable to autodetect OpenOffice/LibreOffice installation. Please choose the installation directory manually."),
                        Localization.lang("Could not find OpenOffice/LibreOffice installation"),
                        JOptionPane.INFORMATION_MESSAGE);
                JFileChooser fileChooser = new JFileChooser(new File(System.getenv("ProgramFiles")));
                fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return Localization.lang("Directories");
                    }
                });
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.showOpenDialog(parent);
                if (fileChooser.getSelectedFile() != null) {
                    sofficeFiles.add(fileChooser.getSelectedFile().toPath());
                }
            }
            Optional<File> actualFile = checkAndSelectAmongMultipleInstalls(sofficeFiles);
            if (actualFile.isPresent()) {
                return setupPreferencesForOO(actualFile.get().getParentFile(), actualFile.get(),
                        OpenOfficePreferences.WINDOWS_EXECUTABLE);
            } else {
                return false;
            }
        } else if (OS.OS_X) {
            List<Path> dirList = fileSearch.findOSXProgramFilesDir();
            List<Path> sofficeFiles = FileUtil.find(OpenOfficePreferences.OSX_EXECUTABLE,dirList);

            if (fileSearchCanceled) {
                return false;
            }
            Optional<File> actualFile = checkAndSelectAmongMultipleInstalls(sofficeFiles);
            if (actualFile.isPresent()) {
                for (Path rootdir : dirList) {
                    if (actualFile.get().getPath().startsWith(rootdir.toString())) {
                        return setupPreferencesForOO(rootdir.toFile(), actualFile.get(), OpenOfficePreferences.OSX_EXECUTABLE);
                    }
                }
            }
            return false;
        } else {
            // Linux:
            String usrRoot = "/usr/lib";
            Optional<Path> inUsr = FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, Paths.get(usrRoot));
            if (fileSearchCanceled) {
                return false;
            }
            if (!inUsr.isPresent()) {
                inUsr = FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, Paths.get("/usr/lib64"));
                if (inUsr.isPresent()) {
                    usrRoot = "/usr/lib64";
                }
            }

            if (fileSearchCanceled) {
                return false;
            }
            Optional<Path> inOpt = FileUtil.find(OpenOfficePreferences.LINUX_EXECUTABLE, Paths.get("/opt"));
            if (fileSearchCanceled) {
                return false;
            }
            if ((inUsr.isPresent()) && (!inOpt.isPresent())) {
                return setupPreferencesForOO(usrRoot, inUsr.get().toFile(), OpenOfficePreferences.LINUX_EXECUTABLE);
            } else if (inOpt.isPresent()) {
                if (!inUsr.isPresent()) {
                    return setupPreferencesForOO("/opt", inOpt.get().toFile(), OpenOfficePreferences.LINUX_EXECUTABLE);
                } else { // Found both
                    JRadioButton optRB = new JRadioButton(inOpt.get().toString(), true);
                    JRadioButton usrRB = new JRadioButton(inUsr.get().toString(), false);
                    ButtonGroup bg = new ButtonGroup();
                    bg.add(optRB);
                    bg.add(usrRB);
                    FormBuilder b = FormBuilder.create()
                            .layout(new FormLayout("left:pref", "pref, 2dlu, pref, 2dlu, pref "));
                    b.add(Localization
                            .lang("Found more than one OpenOffice/LibreOffice executable.") + " "
                            + Localization.lang("Please choose which one to connect to:"))
                            .xy(1, 1);
                    b.add(optRB).xy(1, 3);
                    b.add(usrRB).xy(1, 5);
                    int answer = JOptionPane.showConfirmDialog(null, b.getPanel(),
                            Localization.lang("Choose OpenOffice/LibreOffice executable"),
                            JOptionPane.OK_CANCEL_OPTION);
                    if (answer == JOptionPane.CANCEL_OPTION) {
                        return false;
                    }
                    if (optRB.isSelected()) {
                        return setupPreferencesForOO("/opt", inOpt.get().toFile(), OpenOfficePreferences.LINUX_EXECUTABLE);
                    } else {
                        return setupPreferencesForOO(usrRoot, inUsr.get().toFile(), OpenOfficePreferences.LINUX_EXECUTABLE);
                    }
                }
            }
        }
        return false;
    }

    private boolean setupPreferencesForOO(String usrRoot, File inUsr, String sofficeName) {
        return setupPreferencesForOO(new File(usrRoot), inUsr, sofficeName);
    }

    private boolean setupPreferencesForOO(File rootDir, File inUsr, String sofficeName) {
        preferences.setExecutablePath(new File(inUsr, sofficeName).getPath());
        Optional<Path> jurt = FileUtil.find("jurt.jar", rootDir.toPath());
        if (fileSearchCanceled) {
            return false;
        }
        if (jurt.isPresent()) {
            preferences.setJarsPath(jurt.get().toString());
            return true;
        } else {
            return false;
        }
    }

    private Optional<File> checkAndSelectAmongMultipleInstalls(List<Path> sofficeFiles) {
        if (sofficeFiles.isEmpty()) {
            return Optional.empty();
        } else if (sofficeFiles.size() == 1) {
            return Optional.of(sofficeFiles.get(0).toFile());
        }
        // Otherwise more than one file found, select among them
        DefaultListModel<File> mod = new DefaultListModel<>();
        for (Path tmpfile : sofficeFiles) {
            mod.addElement(tmpfile.toFile());
        }
        JList<File> fileList = new JList<>(mod);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setSelectedIndex(0);
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref", "pref, 2dlu, pref, 4dlu, pref"));
        builder.add(Localization.lang("Found more than one OpenOffice/LibreOffice executable.")).xy(1, 1);
        builder.add(Localization.lang("Please choose which one to connect to:")).xy(1, 3);
        builder.add(fileList).xy(1, 5);
        int answer = JOptionPane.showConfirmDialog(null, builder.getPanel(),
                Localization.lang("Choose OpenOffice/LibreOffice executable"), JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.CANCEL_OPTION) {
            return Optional.empty();
        } else {
            return Optional.of(fileList.getSelectedValue());
        }
    }

    public JDialog showProgressDialog(JDialog progressParent, String title, String message, boolean includeCancelButton) {
        fileSearchCanceled = false;
        JProgressBar bar = new JProgressBar(SwingConstants.HORIZONTAL);
        final JDialog progressDialog = new JDialog(progressParent, title, false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bar.setIndeterminate(true);
        if (includeCancelButton) {
            JButton cancel = new JButton(Localization.lang("Cancel"));
            cancel.addActionListener(event -> {
                fileSearchCanceled = true;
                ((JButton) event.getSource()).setEnabled(false);
            });
            progressDialog.add(cancel, BorderLayout.SOUTH);
        }
        progressDialog.add(new JLabel(message), BorderLayout.NORTH);
        progressDialog.add(bar, BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setVisible(true);
        return progressDialog;
    }
}
