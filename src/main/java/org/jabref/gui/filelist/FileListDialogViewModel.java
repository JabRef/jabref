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

public class FileListDialogViewModel extends AbstractViewModel {

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");
    private final StringProperty linkProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final ListProperty<ExternalFileType> externalfilesTypes = new SimpleListProperty<>(FXCollections.emptyObservableList());
    private final ObjectProperty<ExternalFileType> selectedExternalFileType = new SimpleObjectProperty<>();
    private final BibDatabaseContext bibDatabaseContext;
    private final DialogService dialogService;

    private boolean showSaveDialog;

    private boolean okPressed;

    public boolean isOkPressed() {
        return okPressed;
    }

    public void setOkPressed() {
        okPressed = true;
    }

    public FileListDialogViewModel(BibDatabaseContext bibDatabaseContext, DialogService dialogService) {

        this.bibDatabaseContext = bibDatabaseContext;
        this.dialogService = dialogService;
        externalfilesTypes.set(FXCollections.observableArrayList(ExternalFileTypes.getInstance().getExternalFileTypeSelection()));
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

    public void browseFileDialog() {
        String fileText = linkProperty().get();

        Optional<Path> file = FileHelper.expandFilename(bibDatabaseContext, fileText,
                Globals.prefs.getFileDirectoryPreferences());

        Path workingDir = file.orElse(Paths.get(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)));
        String fileName = Paths.get(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDir)
                .withInitialFileName(fileName).build();
        Optional<Path> path;

        if (showSaveDialog) {
            path = dialogService.showFileSaveDialog(fileDialogConfiguration);
        } else {
            path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        }
        path.ifPresent(newFile -> {
            // Store the directory for next time:
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, newFile.toString());

            // If the file is below the file directory, make the path relative:
            List<Path> fileDirectories = bibDatabaseContext
                    .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
            newFile = FileUtil.shortenFileName(newFile, fileDirectories);

            linkProperty().set(newFile.toString());
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
}
