package org.jabref.gui.linkedfile;

import java.nio.file.Path;
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
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;

public class LinkedFilesEditDialogViewModel extends AbstractViewModel {

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");
    private final StringProperty link = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final ListProperty<ExternalFileType> allExternalFileTypes = new SimpleListProperty<>(FXCollections.emptyObservableList());
    private final ObjectProperty<ExternalFileType> selectedExternalFileType = new SimpleObjectProperty<>();
    private final ObservableOptionalValue<ExternalFileType> monadicSelectedExternalFileType;
    private final BibDatabaseContext database;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final ExternalFileTypes externalFileTypes;

    public LinkedFilesEditDialogViewModel(LinkedFile linkedFile, BibDatabaseContext database, DialogService dialogService, PreferencesService preferences, ExternalFileTypes externalFileTypes) {
        this.database = database;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.externalFileTypes = externalFileTypes;
        allExternalFileTypes.set(FXCollections.observableArrayList(externalFileTypes.getExternalFileTypeSelection()));

        monadicSelectedExternalFileType = EasyBind.wrapNullable(selectedExternalFileType);
        setValues(linkedFile);
    }

    private void setExternalFileTypeByExtension(String link) {
        if (!link.isEmpty()) {

            // Check if this looks like a remote link:
            if (REMOTE_LINK_PATTERN.matcher(link).matches()) {
                externalFileTypes.getExternalFileTypeByExt("html").ifPresent(selectedExternalFileType::setValue);
            }

            // Try to guess the file type:
            String theLink = link.trim();
            externalFileTypes.getExternalFileTypeForName(theLink).ifPresent(selectedExternalFileType::setValue);
        }
    }

    public void openBrowseDialog() {
        String fileText = link.get();

        Optional<Path> file = FileHelper.find(database, fileText, preferences.getFilePreferences());

        Path workingDir = file.orElse(preferences.getWorkingDir());
        String fileName = Path.of(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDir)
                .withInitialFileName(fileName)
                .build();

        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> {
            // Store the directory for next time:
            preferences.setWorkingDirectory(path);
            link.set(relativize(path));

            setExternalFileTypeByExtension(link.getValueSafe());
        });
    }

    public void setValues(LinkedFile linkedFile) {
        description.set(linkedFile.getDescription());

        if (linkedFile.isOnlineLink()) {
            link.setValue(linkedFile.getLink()); // Might be an URL
        } else {
            link.setValue(relativize(Path.of(linkedFile.getLink())));
        }

        selectedExternalFileType.setValue(null);

        // See what is a reasonable selection for the type combobox:
        Optional<ExternalFileType> fileType = externalFileTypes.fromLinkedFile(linkedFile, false);
        if (fileType.isPresent() && !(fileType.get() instanceof UnknownExternalFileType)) {
            selectedExternalFileType.setValue(fileType.get());
        } else if ((linkedFile.getLink() != null) && (!linkedFile.getLink().isEmpty())) {
            setExternalFileTypeByExtension(linkedFile.getLink());
        }
    }

    public StringProperty linkProperty() {
        return link;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ListProperty<ExternalFileType> externalFileTypeProperty() {
        return allExternalFileTypes;
    }

    public ObjectProperty<ExternalFileType> selectedExternalFileTypeProperty() {
        return selectedExternalFileType;
    }

    public LinkedFile getNewLinkedFile() {
        return new LinkedFile(description.getValue(), Path.of(link.getValue()), monadicSelectedExternalFileType.getValue().map(ExternalFileType::toString).orElse(""));
    }

    private String relativize(Path filePath) {
        List<Path> fileDirectories = database.getFileDirectories(preferences.getFilePreferences());
        return FileUtil.relativize(filePath, fileDirectories).toString();
    }
}
