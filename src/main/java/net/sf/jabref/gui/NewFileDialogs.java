package net.sf.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

public class NewFileDialogs {

    /**
     * Custom confirmation dialog
     * http://stackoverflow.com/a/3729157
     */
    private final JFileChooser fileChooser = new JFileChooser() {

        @Override
        public void approveSelection() {
            File file = getSelectedFile();
            if (file.exists() && (getDialogType() == SAVE_DIALOG)) {
                int result = JOptionPane.showConfirmDialog(this,
                        Localization.lang("'%0' exists. Overwrite file?", file.getName()),
                        Localization.lang("Existing file"), JOptionPane.YES_NO_CANCEL_OPTION);
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
    private Collection<FileExtensions> extensions = EnumSet.noneOf(FileExtensions.class);


    public NewFileDialogs(JFrame owner) {
        this(owner, getWorkingDir());
    }

    public NewFileDialogs(JFrame owner, String dir) {
        this.parent = owner;
        this.directory = dir;
        fileChooser.setCurrentDirectory(Paths.get(dir).toFile());
    }

    public NewFileDialogs dirsOnly() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return this;
    }

    public NewFileDialogs withExtension(FileExtensions singleExt) {
        withExtensions(EnumSet.of(singleExt));
        return this;
    }

    public NewFileDialogs withExtensions(Collection<FileExtensions> fileExtensions) {
        this.extensions = fileExtensions;

        for (FileExtensions ext : fileExtensions) {
            extFilter = new FileNameExtensionFilter(ext.getDescription(), ext.getExtensions());
            fileChooser.addChoosableFileFilter(extFilter);
        }

        return this;
    }

    public NewFileDialogs updateWorkingDirectory() {
        Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, this.directory);
        return this;
    }

    public List<String> showDlgAndGetMultipleFiles() {
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setMultiSelectionEnabled(true);

        if (showDialogAndIsAccepted()) {
            List<String> files = Arrays.stream(fileChooser.getSelectedFiles()).map(File::toString)
                    .collect(Collectors.toList());

            return files;
        }

        return Collections.emptyList();

    }

    public Optional<Path> openDlgAndGetSelectedFile() {
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

        if (showDialogAndIsAccepted()) {
            return Optional.of(fileChooser.getSelectedFile().toPath());
        }
        return Optional.empty();
    }

    public Optional<Path> saveNewFile() {
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        if (showDialogAndIsAccepted()) {
            File file = fileChooser.getSelectedFile();
            if (!extensions.isEmpty() && !fileChooser.accept(file)) {

                return Optional.of(Paths.get(file.getPath() + extensions.iterator().next().getFirstExtensionWithDot()));
            }
            return Optional.of(file.toPath());
        }
        return Optional.empty();
    }

    private boolean showDialogAndIsAccepted() {
        return fileChooser.showDialog(parent, null) == JFileChooser.APPROVE_OPTION;
    }

    private static String getWorkingDir() {
        return JabRefPreferences.getInstance().get(JabRefPreferences.WORKING_DIRECTORY);
    }

}
