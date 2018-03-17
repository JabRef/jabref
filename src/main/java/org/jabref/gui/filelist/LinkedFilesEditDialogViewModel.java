package org.jabref.gui.filelist;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

public class LinkedFilesEditDialogViewModel extends AbstractViewModel {

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");
    private final StringProperty linkProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final ListProperty<ExternalFileType> externalfilesTypes = new SimpleListProperty<>(FXCollections.emptyObservableList());
    private final ObjectProperty<ExternalFileType> selectedExternalFileType = new SimpleObjectProperty<>();
    private final BibDatabaseContext database;
    private final DialogService dialogService;

    public LinkedFilesEditDialogViewModel(LinkedFile linkedFile, BibDatabaseContext database, DialogService dialogService) {
        this.database = database;
        this.dialogService = dialogService;
        externalfilesTypes.set(FXCollections.observableArrayList(ExternalFileTypes.getInstance().getExternalFileTypeSelection()));
        setValues(linkedFile);
    }

    private void checkExtension() {

        if (!linkProperty.getValueSafe().isEmpty()) {

            // Check if this looks like a remote link:
            if (REMOTE_LINK_PATTERN.matcher(linkProperty.get()).matches()) {
                ExternalFileTypes.getInstance().getExternalFileTypeByExt("html").ifPresent(selectedExternalFileType::setValue);
            }

            // Try to guess the file type:
            String theLink = linkProperty.get().trim();
            ExternalFileTypes.getInstance().getExternalFileTypeForName(theLink).ifPresent(selectedExternalFileType::setValue);
        }
    }

    public void openBrowseDialog() {
        String fileText = linkProperty().get();

        Optional<Path> file = FileHelper.expandFilename(database, fileText, Globals.prefs.getFileDirectoryPreferences());

        Path workingDir = file.orElse(Paths.get(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)));
        String fileName = Paths.get(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .withInitialDirectory(workingDir)
                                                                                               .withInitialFileName(fileName)
                                                                                               .build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         // Store the directory for next time:
                         Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, path.toString());

                         // If the file is below the file directory, make the path relative:
                         List<Path> fileDirectories = database.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
                         path = FileUtil.shortenFileName(path, fileDirectories);

                         linkProperty().set(path.toString());
                         checkExtension();
                     });
    }

    //
    public void setValues(LinkedFile entry) {
        descriptionProperty.set(entry.getDescription());
        linkProperty.set(entry.getLink());

        selectedExternalFileType.setValue(null);

        // See what is a reasonable selection for the type combobox:
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().fromLinkedFile(entry, false);
        if (fileType.isPresent() && !(fileType.get() instanceof UnknownExternalFileType)) {
            selectedExternalFileType.setValue(fileType.get());
        } else if ((entry.getLink() != null) && (!entry.getLink().isEmpty())) {
            checkExtension();
        }
    }

    public StringProperty linkProperty() {
        return linkProperty;
    }

    public StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public ListProperty<ExternalFileType> externalFileTypeProperty() {
        return externalfilesTypes;
    }

    public ObjectProperty<ExternalFileType> getSelectedExternalFileType() {
        return selectedExternalFileType;
    }

    public LinkedFile getNewLinkedFile() {
        return new LinkedFile(descriptionProperty.getValue(), linkProperty.getValue(), selectedExternalFileType.getValue().toString());

    }

}
