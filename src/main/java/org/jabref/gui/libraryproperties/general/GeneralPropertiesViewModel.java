package org.jabref.gui.libraryproperties.general;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class GeneralPropertiesViewModel implements PropertiesTabViewModel {

    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>(Encodings.getCharsets().get(0));
    private final ListProperty<BibDatabaseMode> databaseModesProperty = new SimpleListProperty<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
    private final SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty = new SimpleObjectProperty<>(BibDatabaseMode.BIBLATEX);
    private final StringProperty generalFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    private final BibDatabaseContext databaseContext;
    private final MetaData metaData;
    private final DirectoryDialogConfiguration directoryDialogConfiguration;

    GeneralPropertiesViewModel(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();

        this.directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferencesService.getFilePreferences().getWorkingDirectory()).build();
    }

    @Override
    public void setValues() {
        boolean isShared = databaseContext.getLocation() == DatabaseLocation.SHARED;
        encodingDisableProperty.setValue(isShared); // the encoding of shared database is always UTF-8

        selectedEncodingProperty.setValue(metaData.getEncoding().orElse(StandardCharsets.UTF_8));
        selectedDatabaseModeProperty.setValue(metaData.getMode().orElse(BibDatabaseMode.BIBLATEX));
        generalFileDirectoryProperty.setValue(metaData.getDefaultFileDirectory().orElse("").trim());
        userSpecificFileDirectoryProperty.setValue(metaData.getUserFileDirectory(preferencesService.getFilePreferences().getUserAndHost()).orElse("").trim());
        laTexFileDirectoryProperty.setValue(metaData.getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost()).map(Path::toString).orElse(""));
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();

        newMetaData.setEncoding(selectedEncodingProperty.getValue());
        newMetaData.setMode(selectedDatabaseModeProperty.getValue());

        String generalFileDirectory = generalFileDirectoryProperty.getValue().trim();
        if (generalFileDirectory.isEmpty()) {
            newMetaData.clearDefaultFileDirectory();
        } else {
            newMetaData.setDefaultFileDirectory(generalFileDirectory);
        }

        String userSpecificFileDirectory = userSpecificFileDirectoryProperty.getValue();
        if (userSpecificFileDirectory.isEmpty()) {
            newMetaData.clearUserFileDirectory(preferencesService.getFilePreferences().getUserAndHost());
        } else {
            newMetaData.setUserFileDirectory(preferencesService.getFilePreferences().getUserAndHost(), userSpecificFileDirectory);
        }

        String latexFileDirectory = laTexFileDirectoryProperty.getValue();
        if (latexFileDirectory.isEmpty()) {
            newMetaData.clearLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost());
        } else {
            newMetaData.setLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost(), Path.of(latexFileDirectory));
        }

        databaseContext.setMetaData(newMetaData);
    }

    public void browseGeneralDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> generalFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseUserDir() {
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(dir -> userSpecificFileDirectoryProperty.setValue(dir.toAbsolutePath().toString()));
    }

    public void browseLatexDir() {
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

    public StringProperty generalFileDirectoryPropertyProperty() {
        return this.generalFileDirectoryProperty;
    }

    public StringProperty userSpecificFileDirectoryProperty() {
        return this.userSpecificFileDirectoryProperty;
    }

    public StringProperty laTexFileDirectoryProperty() {
        return this.laTexFileDirectoryProperty;
    }
}
