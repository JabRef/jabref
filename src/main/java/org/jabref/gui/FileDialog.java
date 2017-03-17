package org.jabref.gui;

import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.jabref.Globals;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileExtensions;
import org.jabref.preferences.JabRefPreferences;

/**
 * @deprecated use {@link DialogService} instead.
 */
@Deprecated
public class FileDialog {


    private final FileChooser fileChooser;
    private FileDialogConfiguration.Builder configurationBuilder;

    /**
     * Creates a new filedialog showing the current working dir {@link JabRefPreferences#WORKING_DIRECTORY}
     * @param parent The parent frame associated with this dialog
     */
    public FileDialog(Component parent) {
        this(parent, getWorkingDir());
    }

    /**
     * Creates a new dialog in the given directory
     * @param parent The parent frame associated with this dialog
     * @param dir The starting directory to show in the dialog
     */
    public FileDialog(Component parent, String dir) {
        this(parent, Paths.get(dir));
    }

    public FileDialog(Component parent, Path dir) {
        Objects.requireNonNull(dir, "Directory must not be null");
        //Dir must be a folder, not a file
        if (!Files.isDirectory(dir)) {
            dir = dir.getParent();
        }
        //The lines above work also if the dir does not exist at all!
        //NULL is accepted by the filechooser as no inital path
        if (!Files.exists(dir)) {
            dir = null;
        }
        fileChooser = new FileChooser();
        configurationBuilder = new FileDialogConfiguration.Builder();
        configurationBuilder = configurationBuilder.withInitialDirectory(dir);

    }

    /**
     * Add a single extension as file filter
     * @param singleExt The extension
     * @return FileDialog
     */
    public FileDialog withExtension(FileExtensions singleExt) {
        configurationBuilder = configurationBuilder.addExtensionFilter(singleExt);
        return this;
    }

    /**
     * Add a multiple extensions as file filter
     * @param fileExtensions The extensions
     * @return FileDialog
     */
    public FileDialog withExtensions(Collection<FileExtensions> fileExtensions) {
        configurationBuilder = configurationBuilder.addExtensionFilters(fileExtensions);
        return this;
    }

    /**
     * Sets the default file filter extension for the file dialog.
     * If the desired extension is not found nothing is changed.
     *
     * @param extension the file extension
     */
    public void setDefaultExtension(FileExtensions extension) {
        configurationBuilder = configurationBuilder.withDefaultExtension(extension);
    }

    /**
     * Returns the currently selected file filter.
     *
     * @return FileFilter
     */
    public FileChooser.ExtensionFilter getFileFilter() {
        return fileChooser.getSelectedExtensionFilter();
    }

    /**
     * Sets the initial file name, useful for saving dialogs
     * @param fileName
     */
    public void setInitialFileName(String fileName) {
        fileChooser.setInitialFileName(fileName);
    }

    /**
     * Sets a custom file filter.
     * Only use when withExtension() does not suffice.
     *
     * @param filter the custom file filter
     */
    public void setFileFilter(FileChooser.ExtensionFilter filter) {
        fileChooser.getExtensionFilters().add(filter);
        fileChooser.setSelectedExtensionFilter(filter);

    }

    /**
     * Updates the working directory preference
     * @return FileDialog
     */
    public FileDialog updateWorkingDirPref() {
        Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, fileChooser.getInitialDirectory().getAbsolutePath());
        return this;
    }

    /**
     * Shows an {@link JFileChooser#OPEN_DIALOG} and allows to select a single folder
     * @return The path of the selected folder or {@link Optional#empty()} if dialog is aborted
     */
    public Optional<Path> showDialogAndGetSelectedDirectory() {
        FileDialogConfiguration configuration = configurationBuilder.build();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(Localization.lang("Select directory"));
        configuration.getInitialDirectory().map(Path::toFile).ifPresent(directoryChooser::setInitialDirectory);

        return DefaultTaskExecutor
                .runInJavaFXThread(() -> Optional.ofNullable(directoryChooser.showDialog(null)).map(File::toPath));
    }

    /**
     * Shows an {@link JFileChooser#OPEN_DIALOG} and allows to select multiple files
     * @return List containing the paths of all files or an empty list if dialog is canceled
     */
    public List<String> showDialogAndGetMultipleFiles() {
        configureFileChooser();
        return DefaultTaskExecutor.runInJavaFXThread(() -> {
            List<File> files = fileChooser.showOpenMultipleDialog(null);
            if (files == null) {
                return Collections.emptyList();
            } else {
                return files.stream().map(File::toString).collect(Collectors.toList());
            }
        });
    }

    private void configureFileChooser() {
        FileDialogConfiguration configuration = configurationBuilder.build();
        fileChooser.getExtensionFilters().addAll(configuration.getExtensionFilters());
        fileChooser.setSelectedExtensionFilter(configuration.getDefaultExtension());
        configuration.getInitialDirectory().map(Path::toFile).ifPresent(fileChooser::setInitialDirectory);
    }

    /**
     * Shows an {@link JFileChooser#OPEN_DIALOG} and allows to select a single file/folder
     * @return The path of the selected file/folder or {@link Optional#empty()} if dialog is aborted
     */
    public Optional<Path> showDialogAndGetSelectedFile() {
        configureFileChooser();
        return DefaultTaskExecutor
                .runInJavaFXThread(() -> Optional.ofNullable(fileChooser.showOpenDialog(null)).map(File::toPath));
    }

    /**
     * Shows an {@link JFileChooser#SAVE_DIALOG} and allows to save a new file <br>
     * If an extension is provided, adds the extension to the file <br>
     * Selecting an existing file will show an overwrite dialog
     * @return The path of the new file, or {@link Optional#empty()} if dialog is aborted
     */
    public Optional<Path> saveNewFile() {
        configureFileChooser();
        return DefaultTaskExecutor
                .runInJavaFXThread(() -> Optional.ofNullable(fileChooser.showSaveDialog(null)).map(File::toPath));
    }

    private static String getWorkingDir() {
        return Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY);
    }
}
