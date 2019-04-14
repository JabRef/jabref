package org.jabref.gui.filelist;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.PreferencesService;

import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicObservableValue;

public class LinkedFilesRenameDialogViewModel extends AbstractViewModel {

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");
    private final StringProperty link = new SimpleStringProperty("");
    private final ObjectProperty<ExternalFileType> selectedExternalFileType = new SimpleObjectProperty<>();
    private final MonadicObservableValue<ExternalFileType> monadicSelectedExternalFileType;
    private final BibDatabaseContext database;
    private final DialogService dialogService;
    private final PreferencesService preferences;

    public LinkedFilesRenameDialogViewModel(LinkedFile linkedFile, BibDatabaseContext database, DialogService dialogService, PreferencesService preferences, ExternalFileTypes externalFileTypes) {
        this.database = database;
        this.dialogService = dialogService;
        this.preferences = preferences;
        monadicSelectedExternalFileType = EasyBind.monadic(selectedExternalFileType);
        setValues(linkedFile);
    }

    public void openBrowseDialog() {
        String fileText = link.get();

        Optional<Path> file = FileHelper.expandFilename(database, fileText, preferences.getFilePreferences());

        Path workingDir = file.orElse(preferences.getWorkingDir());
        String fileName = Paths.get(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDir)
                .withInitialFileName(fileName)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> {
            // Store the directory for next time:
            preferences.setWorkingDir(path);
            link.set(relativize(path));
        });
    }

    public void setValues(LinkedFile linkedFile) {
        Path linkPath = Paths.get(linkedFile.getLink());
        link.set(relativize(linkPath));
    }

    public StringProperty linkProperty() {
        return link;
    }

    public LinkedFile getNewLinkedFile() {
        return new LinkedFile("", link.getValue(), monadicSelectedExternalFileType.map(ExternalFileType::toString).getOrElse(""));
    }

    private String relativize(Path filePath) {
        List<Path> fileDirectories = database.getFileDirectoriesAsPaths(preferences.getFilePreferences());
        return FileUtil.relativize(filePath, fileDirectories).toString();
    }

}
