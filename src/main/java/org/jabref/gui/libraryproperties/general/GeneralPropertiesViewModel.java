package org.jabref.gui.libraryproperties.general;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;
import org.jetbrains.annotations.NotNull;

public class GeneralPropertiesViewModel implements PropertiesTabViewModel {

    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>(Encodings.getCharsets().getFirst());
    private final ListProperty<BibDatabaseMode> databaseModesProperty = new SimpleListProperty<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
    private final SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty = new SimpleObjectProperty<>(BibDatabaseMode.BIBLATEX);
    private final StringProperty librarySpecificDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final CliPreferences preferences;

    private final BibDatabaseContext databaseContext;
    private final MetaData metaData;



    private final StringProperty librarySpecificDirectoryError = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryError = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryError = new SimpleStringProperty("");



    GeneralPropertiesViewModel(BibDatabaseContext databaseContext, DialogService dialogService, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();
    }




    /**
     * Validates all paths and returns true if all paths are valid.
     * If a path is invalid, an error message is stored in the corresponding error property.
     *
     * @return True if all paths are valid, false otherwise.
     */
    public boolean validatePaths() {
        boolean isValid = true;

        // Validate library-specific directory
        String libraryPath = librarySpecificDirectoryProperty.getValue().trim();
        if (!libraryPath.isEmpty() && !validatePath(libraryPath)) {
            librarySpecificDirectoryError.setValue("Invalid path: " + libraryPath);
            isValid = false;
        } else {
            librarySpecificDirectoryError.setValue("");
        }

        // Validate user-specific directory
        String userPath = userSpecificFileDirectoryProperty.getValue().trim();
        if (!userPath.isEmpty() && !validatePath(userPath)) {
            userSpecificFileDirectoryError.setValue("Invalid path: " + userPath);
            isValid = false;
        } else {
            userSpecificFileDirectoryError.setValue("");
        }

        // Validate LaTeX directory
        String latexPath = laTexFileDirectoryProperty.getValue().trim();
        if (!latexPath.isEmpty() && !validatePath(latexPath)) {
            laTexFileDirectoryError.setValue("Invalid path: " + latexPath);
            isValid = false;
        } else {
            laTexFileDirectoryError.setValue("");
        }

        return isValid;
    }



    /**
     * Validates if the given path exists.
     *
     * @param path The path to validate.
     * @return True if the path exists or is empty, false otherwise.
     */
    public boolean validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return true; // Empty path is considered valid (falls back to default behavior)
        }

        Path resolvedPath;
        if (Path.of(path).isAbsolute()) {
            resolvedPath = Path.of(path);
        } else {
            resolvedPath = databaseContext.getDatabasePath()
                    .map(dbPath -> dbPath.getParent().resolve(path))
                    .orElse(Path.of(path)); // Fallback if database path is not set
        }

        return Files.exists(resolvedPath); // The path must exist to be valid
    }


/*
public boolean validatePath(String path) {
    if (path == null || path.trim().isEmpty()) {
        return true; // Empty path is considered valid (falls back to default behavior)
    }

    Path resolvedPath;
    if (Path.of(path).isAbsolute()) {
        resolvedPath = Path.of(path);
    } else {
        resolvedPath = databaseContext.getDatabasePath()
                .map(dbPath -> dbPath.getParent().resolve(path))
                .orElse(Path.of(path)); // Fallback if database path is not set
    }

    return Files.exists(resolvedPath); // The path must exist to be valid
}
*/





    @Override
    public void setValues() {
        boolean isShared = databaseContext.getLocation() == DatabaseLocation.SHARED;
        encodingDisableProperty.setValue(isShared); // the encoding of shared database is always UTF-8

        selectedEncodingProperty.setValue(metaData.getEncoding().orElse(StandardCharsets.UTF_8));
        selectedDatabaseModeProperty.setValue(metaData.getMode().orElse(BibDatabaseMode.BIBLATEX));
        librarySpecificDirectoryProperty.setValue(metaData.getLibrarySpecificFileDirectory().orElse("").trim());
        userSpecificFileDirectoryProperty.setValue(metaData.getUserFileDirectory(preferences.getFilePreferences().getUserAndHost()).orElse("").trim());
        laTexFileDirectoryProperty.setValue(metaData.getLatexFileDirectory(preferences.getFilePreferences().getUserAndHost()).map(Path::toString).orElse(""));
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = getMetaData();

        String userSpecificFileDirectory = userSpecificFileDirectoryProperty.getValue();
        if (userSpecificFileDirectory.isEmpty()) {
            newMetaData.clearUserFileDirectory(preferences.getFilePreferences().getUserAndHost());
        } else {
            newMetaData.setUserFileDirectory(preferences.getFilePreferences().getUserAndHost(), userSpecificFileDirectory);
        }

        String latexFileDirectory = laTexFileDirectoryProperty.getValue();
        if (latexFileDirectory.isEmpty()) {
            newMetaData.clearLatexFileDirectory(preferences.getFilePreferences().getUserAndHost());
        } else {
            newMetaData.setLatexFileDirectory(preferences.getFilePreferences().getUserAndHost(), Path.of(latexFileDirectory));
        }

        databaseContext.setMetaData(newMetaData);
    }

    private @NotNull MetaData getMetaData() {
        MetaData newMetaData = databaseContext.getMetaData();

        newMetaData.setEncoding(selectedEncodingProperty.getValue());
        newMetaData.setMode(selectedDatabaseModeProperty.getValue());

        String librarySpecificFileDirectory = librarySpecificDirectoryProperty.getValue().trim();
        if (librarySpecificFileDirectory.isEmpty()) {
            newMetaData.clearLibrarySpecificFileDirectory();
        } else {
            newMetaData.setLibrarySpecificFileDirectory(librarySpecificFileDirectory);
        }
        return newMetaData;
    }

    public void browseLibrarySpecificDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(librarySpecificDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> librarySpecificDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseUserDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(userSpecificFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> userSpecificFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseLatexDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(laTexFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> laTexFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public BooleanProperty encodingDisableProperty() {
        return encodingDisableProperty;
    }

    public ListProperty<Charset> encodingsProperty() {
        return this.encodingsProperty;
    }

    public ObjectProperty<Charset> selectedEncodingProperty() {
        return selectedEncodingProperty;
    }

    public ListProperty<BibDatabaseMode> databaseModesProperty() {
        return databaseModesProperty;
    }

    public SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty() {
        return selectedDatabaseModeProperty;
    }

    public StringProperty librarySpecificDirectoryPropertyProperty() {
        return this.librarySpecificDirectoryProperty;
    }

    public StringProperty userSpecificFileDirectoryProperty() {
        return this.userSpecificFileDirectoryProperty;
    }

    public StringProperty laTexFileDirectoryProperty() {
        return this.laTexFileDirectoryProperty;
    }

    private Path getBrowseDirectory(String configuredDir) {
        if (configuredDir.isEmpty()) {
            return preferences.getFilePreferences().getWorkingDirectory();
        }
        Optional<Path> foundPath = this.databaseContext.getFileDirectories(preferences.getFilePreferences()).stream()
                                                       .filter(path -> path.toString().endsWith(configuredDir))
                                                       .filter(Files::exists).findFirst();

        if (foundPath.isEmpty()) {
            dialogService.notify(Localization.lang("Path %0 could not be resolved. Using working dir.", configuredDir));
            return preferences.getFilePreferences().getWorkingDirectory();
        }
        return foundPath.get();
    }
}

