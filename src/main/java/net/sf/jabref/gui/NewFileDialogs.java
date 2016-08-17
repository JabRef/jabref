package net.sf.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;
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
                default:
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


    /**
     * Creates a new filedialog showing the current working dir {@link JabRefPreferences#WORKING_DIRECTORY}
     * @param parent The parent frame associated with this dialog
     */
    public NewFileDialogs(JFrame parent) {
        this(parent, getWorkingDir());
    }

    /**
     * Creates a new dialog in the given directory
     * @param parent The parent frame associated with this dialog
     * @param dir The starting directory to show in the dialog
     */
    public NewFileDialogs(JFrame parent, String dir) {
        Objects.requireNonNull(dir, "Directory must not be null");

        this.parent = parent;
        this.directory = dir;
        fileChooser.setCurrentDirectory(Paths.get(dir).toFile());
    }

    /**
     * Show only directories instead of files and folders
     * @return NewFileDialogs
     */
    public NewFileDialogs dirsOnly() {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return this;
    }

    /**
     * Add a single extension as filter, the single extension filter is preselected
     * @param singleExt The extension
     * @return NewFileDialogs
     */
    public NewFileDialogs withExtension(FileExtensions singleExt) {
        this.extensions = EnumSet.of(singleExt);
        extFilter = new FileNameExtensionFilter(singleExt.getDescription(), singleExt.getExtensions());
        fileChooser.setFileFilter(extFilter);

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

    /**
     * Updates the working directory preference
     * @return NewFileDialogs
     */
    public NewFileDialogs updateWorkingDirPref() {
        Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, this.directory);
        return this;
    }

    /**
     * Shows an {@link JFileChooser#OPEN_DIALOG} and allows to select multiple files
     * @return List containing the paths of all files or an empty list if dialog is canceled
     */
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

    /**
     * Shows an {@link JFileChooser#OPEN_DIALOG} and allows to select a single file/folder
     * @return The path of the selected file/folder or {@link Optional#empty()} if dialog is aborted
     */
    public Optional<Path> openDlgAndGetSelectedFile() {
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

        if (showDialogAndIsAccepted()) {
            return Optional.of(fileChooser.getSelectedFile().toPath());
        }
        return Optional.empty();
    }

    /**
     *
     * Shows an {@link JFileChooser#SAVE_DIALOG} and allows to save a new file <br>
     * If an extension is provided, adds the extension to the file <br>
     * Selecting an existing file will show an overwrite dialog
     * @return The path of the new file, or {@link Optional#empty()} if dialog is aborted
     */
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
        return Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY);
    }

}
