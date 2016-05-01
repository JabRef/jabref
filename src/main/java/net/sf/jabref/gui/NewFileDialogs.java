package net.sf.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * WIP: Will replaced the FileDialogs class
 *
 *
 */
public class NewFileDialogs {

    /**
     * Custom confirmation dialog
     * http://stackoverflow.com/a/3729157
     */
    private final JFileChooser fileChooser = new JFileChooser() {

        @Override
        public void approveSelection() {
            File f = getSelectedFile();
            if (f.exists() && (getDialogType() == SAVE_DIALOG)) {
                int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result) {
                case JOptionPane.YES_OPTION:
                    super.approveSelection();
                    return;
                case JOptionPane.NO_OPTION:
                    return;
                case JOptionPane.CLOSED_OPTION:
                    return;
                case JOptionPane.CANCEL_OPTION:
                    cancelSelection();
                    return;
                }
            }
            super.approveSelection();
        }
    };

    private final JFrame parent;
    private final String directory;
    private FileNameExtensionFilter extFilter;
    private Set<FileExtensions> extensions = EnumSet.noneOf(FileExtensions.class);


    public NewFileDialogs(JFrame owner) {
        this(owner, getWorkingDir());
    }

    public NewFileDialogs(JFrame owner, String dir) {
        this.parent = owner;
        this.directory = dir;
        System.out.println("DIR in Constructor" + Paths.get(dir));
        fileChooser.setCurrentDirectory(Paths.get(dir).toFile());
    }

    private void saveFileDialog() {
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
    }

    private void openFileDialog() {
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

    }

    public NewFileDialogs dirsOnly() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return this;
    }

    public NewFileDialogs withExtension(FileExtensions singleExt) {
        withExtension(EnumSet.of(singleExt));
        return this;
    }

    public NewFileDialogs withExtension(Set<FileExtensions> fileExtensions) {
        this.extensions = fileExtensions;

        for (FileExtensions ext : fileExtensions) {
            extFilter = new FileNameExtensionFilter(ext.getDescription(), ext.getExtensions());
            fileChooser.addChoosableFileFilter(extFilter);

        }

        return this;
    }

    public NewFileDialogs updateWorkingDirectory() {
        updateWorkingDirectorySetting(this.directory);
        return this;
    }

    public List<String> getMultipleFileNames() {
        openFileDialog();
        fileChooser.setMultiSelectionEnabled(true);

        if (showDialogAndIsAccepted()) {
            List<String> files = Arrays.stream(fileChooser.getSelectedFiles()).map(f -> f.toString())
                    .collect(Collectors.toList());

            return files;
        }

        return Collections.emptyList();

    }

    public Path getSelectedFile() {
        openFileDialog();
        if (showDialogAndIsAccepted()) {
            return fileChooser.getSelectedFile().toPath();
        }
        return Paths.get("");
    }

    public Path saveNewFile() {
        saveFileDialog();
        if (showDialogAndIsAccepted()) {
            File f = fileChooser.getSelectedFile();
            if (!extensions.isEmpty() && !fileChooser.accept(f)) {

                System.out.println("Exts " + extensions);

                return new File(f.getPath() + extensions.iterator().next().getFirstExtensionWithDot()).toPath();
            }
            return f.toPath();
        }
        return Paths.get("");
    }

    private boolean showDialogAndIsAccepted() {
        return fileChooser.showDialog(parent, null) == JFileChooser.APPROVE_OPTION;
    }

    private static String getWorkingDir() {
        return Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY);
    }

    private static void updateWorkingDirectorySetting(String dir) {
        Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, dir);

    }

}
