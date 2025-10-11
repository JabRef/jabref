package org.jabref.gui.libraryproperties.general;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
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
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class GeneralPropertiesViewModel implements PropertiesTabViewModel {

    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(OS.ENCODINGS));
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>(OS.ENCODINGS.getFirst());
    private final ListProperty<BibDatabaseMode> databaseModesProperty = new SimpleListProperty<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
    private final SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty = new SimpleObjectProperty<>(BibDatabaseMode.BIBLATEX);
    private final StringProperty librarySpecificDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");

    private final Validator librarySpecificFileDirectoryValidator;
    private final Validator userSpecificFileDirectoryValidator;
    private final Validator laTexFileDirectoryValidator;

    private final DialogService dialogService;
    private final CliPreferences preferences;

    private final BibDatabaseContext databaseContext;
    private final MetaData metaData;

    GeneralPropertiesViewModel(BibDatabaseContext databaseContext, DialogService dialogService, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();

        librarySpecificFileDirectoryValidator = new FunctionBasedValidator<>(
                librarySpecificDirectoryProperty,
                mainDirectoryPath -> validateDirectory(mainDirectoryPath, "Library-specific")
        );

        userSpecificFileDirectoryValidator = new FunctionBasedValidator<>(
                userSpecificFileDirectoryProperty,
                mainDirectoryPath -> validateDirectory(mainDirectoryPath, "User-specific")
        );

        laTexFileDirectoryValidator = new FunctionBasedValidator<>(
                laTexFileDirectoryProperty,
                mainDirectoryPath -> validateDirectory(mainDirectoryPath, "LaTeX")
        );
    }

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
        MetaData newMetaData = databaseContext.getMetaData();

        newMetaData.setEncoding(selectedEncodingProperty.getValue());
        newMetaData.setMode(selectedDatabaseModeProperty.getValue());

        String librarySpecificFileDirectory = librarySpecificDirectoryProperty.getValue().trim();
        if (librarySpecificFileDirectory.isEmpty()) {
            newMetaData.clearLibrarySpecificFileDirectory();
        } else if (librarySpecificFileDirectoryStatus().isValid()) {
            newMetaData.setLibrarySpecificFileDirectory(librarySpecificFileDirectory);
        }

        String userSpecificFileDirectory = userSpecificFileDirectoryProperty.getValue();
        if (userSpecificFileDirectory.isEmpty()) {
            newMetaData.clearUserFileDirectory(preferences.getFilePreferences().getUserAndHost());
        } else if (userSpecificFileDirectoryStatus().isValid()) {
            newMetaData.setUserFileDirectory(preferences.getFilePreferences().getUserAndHost(), userSpecificFileDirectory);
        }

        String latexFileDirectory = laTexFileDirectoryProperty.getValue();
        if (latexFileDirectory.isEmpty()) {
            newMetaData.clearLatexFileDirectory(preferences.getFilePreferences().getUserAndHost());
        } else if (laTexFileDirectoryStatus().isValid()) {
            newMetaData.setLatexFileDirectory(preferences.getFilePreferences().getUserAndHost(), latexFileDirectory);
        }

        databaseContext.setMetaData(newMetaData);
    }

    ValidationStatus librarySpecificFileDirectoryStatus() {
        return librarySpecificFileDirectoryValidator.getValidationStatus();
    }

    ValidationStatus userSpecificFileDirectoryStatus() {
        return userSpecificFileDirectoryValidator.getValidationStatus();
    }

    ValidationStatus laTexFileDirectoryStatus() {
        return laTexFileDirectoryValidator.getValidationStatus();
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus librarySpecificFileDirectoryStatus = librarySpecificFileDirectoryStatus();
        ValidationStatus userSpecificFileDirectoryStatus = userSpecificFileDirectoryStatus();
        ValidationStatus laTexFileDirectoryStatus = laTexFileDirectoryStatus();

        return promptUserToConfirmAction(librarySpecificFileDirectoryStatus) &&
                promptUserToConfirmAction(userSpecificFileDirectoryStatus) &&
                promptUserToConfirmAction(laTexFileDirectoryStatus);
    }

    public void browseLibrarySpecificDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(librarySpecificDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> setDirectory(librarySpecificDirectoryProperty, dir));
    }

    public void browseUserDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(userSpecificFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> setDirectory(userSpecificFileDirectoryProperty, dir));
    }

    public void browseLatexDir() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(getBrowseDirectory(laTexFileDirectoryProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> setDirectory(laTexFileDirectoryProperty, dir));
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

    public StringProperty librarySpecificDirectoryProperty() {
        return this.librarySpecificDirectoryProperty;
    }

    public StringProperty userSpecificFileDirectoryProperty() {
        return this.userSpecificFileDirectoryProperty;
    }

    public StringProperty laTexFileDirectoryProperty() {
        return this.laTexFileDirectoryProperty;
    }

    private Path getBrowseDirectory(String configuredDir) {
        Optional<Path> libPath = this.databaseContext.getDatabasePath();
        Path workingDir = preferences.getFilePreferences().getWorkingDirectory();

        if (libPath.isEmpty()) {
            Path potentialAbsolutePath = Path.of(configuredDir);
            return Files.isDirectory(potentialAbsolutePath) ? potentialAbsolutePath : workingDir;
        }
        if (configuredDir.isEmpty()) {
            return workingDir;
        }

        Path configuredPath = libPath.get().getParent().resolve(configuredDir).normalize();

        // configuredDir can be input manually, which may lead it to being invalid
        if (!Files.isDirectory(configuredPath)) {
            dialogService.notify(Localization.lang("Path %0 could not be resolved. Using working directory.", configuredDir));
            return workingDir;
        }

        return configuredPath;
    }

    private ValidationMessage validateDirectory(String directoryPath, String messageKey) {
        Optional<Path> libPath = this.databaseContext.getDatabasePath();
        Path potentialAbsolutePath = Path.of(directoryPath);

        // check absolute path separately in case of unsaved libraries
        if (libPath.isEmpty() && Files.isDirectory(potentialAbsolutePath)) {
            return null;
        }
        try {
            if (!libPath.map(p -> p.getParent().resolve(directoryPath).normalize())
                        .map(Files::isDirectory)
                        .orElse(false)) {
                return ValidationMessage.error(
                        Localization.lang("The file directory '%0' for the %1 file path is not found or is inaccessible.", directoryPath, messageKey)
                );
            }
        } catch (InvalidPathException ex) {
            return ValidationMessage.error(
                    Localization.lang("Invalid path: '%0'.\nCheck \"%1\".", directoryPath, messageKey)
            );
        }
        // Directory is valid
        return null;
    }

    private boolean promptUserToConfirmAction(ValidationStatus status) {
        if (!status.isValid()) {
            return status.getHighestMessage()
                         .map(message -> dialogService.showConfirmationDialogAndWait(
                                 Localization.lang("Action required: override default file directories"),
                                 message.getMessage() + "\n" + Localization.lang("Would you like to save your other preferences?"),
                                 Localization.lang("Save"),
                                 Localization.lang("Return to Properties")))
                         .orElse(false);
        }
        return true;
    }

    public void togglePath(StringProperty fileDirectory) {
        Optional<Path> libPath = this.databaseContext.getDatabasePath();

        if (libPath.isEmpty() || fileDirectory.get().isEmpty()) {
            return;
        }

        try {
            Path parentPath = libPath.get().getParent();
            Path currPath = Path.of(fileDirectory.get());
            String newPath;

            if (!currPath.isAbsolute()) {
                newPath = parentPath.resolve(fileDirectory.get()).toAbsolutePath().toString();
            } else if (currPath.isAbsolute()) {
                Path rel = parentPath.relativize(currPath);
                newPath = rel.toString().isEmpty() ? "." : rel.toString();
            } else {
                // case: convert to relative path and currPath is relative
                return;
            }

            fileDirectory.setValue(newPath);
        } catch (InvalidPathException ex) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error occurred %0", ex.getMessage()));
        }
    }

    /**
     * For a saved library, any directory relative to the library path will be set as relative; otherwise, it will be set as absolute.
     *
     * @param fileDirectory   file directory to be updated (lib/user/laTex)
     * @param selectedDirPath path of directory (selected by user)
     */
    private void setDirectory(StringProperty fileDirectory, Path selectedDirPath) {
        Optional<Path> libPath = this.databaseContext.getDatabasePath();

        if (libPath.isEmpty() || !selectedDirPath.startsWith(libPath.get().getParent())) {
            // set absolute path
            fileDirectory.setValue(selectedDirPath.toAbsolutePath().toString());
            return;
        }

        // set relative path
        fileDirectory.setValue(libPath.get()
                                      .getParent()
                                      .relativize(selectedDirPath).toString());
    }
}
