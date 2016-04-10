/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.openoffice;

import net.sf.jabref.gui.worker.AbstractWorker;

import javax.swing.*;

import java.io.File;
import java.awt.*;
import java.util.*;
import java.util.List;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.openoffice.OpenOfficeFileSearch;
import net.sf.jabref.logic.openoffice.OpenOfficePreferences;
import net.sf.jabref.logic.util.OS;

/**
 * Tools for automatically detecting jar and executable paths to OpenOffice and/or LibreOffice.
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
        fileSearch.resetFileSearch();
        if (OS.WINDOWS) {
            List<File> progFiles = fileSearch.findWindowsProgramFilesDir();
            List<File> sofficeFiles = new ArrayList<>(
                    fileSearch.findFileInDirs(progFiles, OpenOfficePreferences.WINDOWS_EXECUTABLE));
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
                    sofficeFiles.add(fileChooser.getSelectedFile());
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
            List<File> dirList = fileSearch.findOSXProgramFilesDir();
            List<File> sofficeFiles = new ArrayList<>(
                    fileSearch.findFileInDirs(dirList, OpenOfficePreferences.OSX_EXECUTABLE));

            if (fileSearchCanceled) {
                return false;
            }
            Optional<File> actualFile = checkAndSelectAmongMultipleInstalls(sofficeFiles);
            if (actualFile.isPresent()) {
                for (File rootdir : dirList) {
                    if (actualFile.get().getPath().startsWith(rootdir.getPath())) {
                        return setupPreferencesForOO(rootdir, actualFile.get(), OpenOfficePreferences.OSX_EXECUTABLE);
                    }
                }
            }
            return false;
        } else {
            // Linux:
            String usrRoot = "/usr/lib";
            File inUsr = fileSearch.findFileInDir(new File(usrRoot), OpenOfficePreferences.LINUX_EXECUTABLE);
            if (fileSearchCanceled) {
                return false;
            }
            if (inUsr == null) {
                inUsr = fileSearch.findFileInDir(new File("/usr/lib64"), OpenOfficePreferences.LINUX_EXECUTABLE);
                if (inUsr != null) {
                    usrRoot = "/usr/lib64";
                }
            }

            if (fileSearchCanceled) {
                return false;
            }
            File inOpt = fileSearch.findFileInDir(new File("/opt"), OpenOfficePreferences.LINUX_EXECUTABLE);
            if (fileSearchCanceled) {
                return false;
            }
            if ((inUsr != null) && (inOpt == null)) {
                return setupPreferencesForOO(usrRoot, inUsr, OpenOfficePreferences.LINUX_EXECUTABLE);
            } else if (inOpt != null) {
                if (inUsr == null) {
                    return setupPreferencesForOO("/opt", inOpt, OpenOfficePreferences.LINUX_EXECUTABLE);
                } else { // Found both
                    JRadioButton optRB = new JRadioButton(inOpt.getPath(), true);
                    JRadioButton usrRB = new JRadioButton(inUsr.getPath(), false);
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
                        return setupPreferencesForOO("/opt", inOpt, OpenOfficePreferences.LINUX_EXECUTABLE);
                    } else {
                        return setupPreferencesForOO(usrRoot, inUsr, OpenOfficePreferences.LINUX_EXECUTABLE);
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
        File jurt = fileSearch.findFileInDir(rootDir, "jurt.jar");
        if (fileSearchCanceled) {
            return false;
        }
        if (jurt == null) {
            return false;
        } else {
            preferences.setJarsPath(jurt.getPath());
            return true;
        }
    }

    private Optional<File> checkAndSelectAmongMultipleInstalls(List<File> sofficeFiles) {
        if (sofficeFiles.isEmpty()) {
            return Optional.empty();
        } else if (sofficeFiles.size() == 1) {
            return Optional.of(sofficeFiles.get(0));
        }
        // Otherwise more than one file found, select among them
        DefaultListModel<File> mod = new DefaultListModel<>();
        for (File tmpfile : sofficeFiles) {
            mod.addElement(tmpfile);
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
                fileSearch.cancelFileSearch();
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
