package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.l10n.Encodings;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class LibraryPropertiesDialogViewModel {

    private final BooleanProperty encodingDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<Charset> encodingsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(Encodings.getCharsets()));
    private final ObjectProperty<Charset> selectedEncodingPropety = new SimpleObjectProperty<>(Encodings.getCharsets().get(0));
    private final ListProperty<BibDatabaseMode> databaseModesProperty = new SimpleListProperty<>(FXCollections.observableArrayList(BibDatabaseMode.values()));
    private final SimpleObjectProperty<BibDatabaseMode> selectedDatabaseModeProperty = new SimpleObjectProperty<>(BibDatabaseMode.BIBLATEX);
    private final StringProperty generalFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty userSpecificFileDirectoryProperty = new SimpleStringProperty("");
    private final StringProperty laTexFileDirectoryProperty = new SimpleStringProperty("");
    private final BooleanProperty protectDisableProperty = new SimpleBooleanProperty();
    private final BooleanProperty libraryProtectedProperty = new SimpleBooleanProperty();

    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> cleanupsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final DialogService dialogService;

    private final DirectoryDialogConfiguration directoryDialogConfiguration;
    private final MetaData initialMetaData;

    public LibraryPropertiesDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferences) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialMetaData = databaseContext.getMetaData();

        this.directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir()).build();
    }

    void setValues() {
        boolean isShared = (databaseContext.getLocation() == DatabaseLocation.SHARED);
        encodingDisableProperty.setValue(isShared); // the encoding of shared database is always UTF-8
        protectDisableProperty.setValue(isShared);

        selectedEncodingPropety.setValue(initialMetaData.getEncoding().orElse(preferences.getDefaultEncoding()));
        selectedDatabaseModeProperty.setValue(initialMetaData.getMode().orElse(BibDatabaseMode.BIBLATEX));
        generalFileDirectoryProperty.setValue(initialMetaData.getDefaultFileDirectory().orElse("").trim());
        userSpecificFileDirectoryProperty.setValue(initialMetaData.getUserFileDirectory(preferences.getUser()).orElse("").trim());
        laTexFileDirectoryProperty.setValue(initialMetaData.getLatexFileDirectory(preferences.getUser()).map(Path::toString).orElse(""));
        libraryProtectedProperty.setValue(initialMetaData.isProtected());

        Optional<FieldFormatterCleanups> saveActions = initialMetaData.getSaveActions();
        saveActions.ifPresentOrElse(value -> {
            cleanupsDisableProperty().setValue(!value.isEnabled());
            cleanupsProperty().setValue(FXCollections.observableArrayList(value.getConfiguredActions()));
        }, () -> {
            initialMetaData.setSaveActions(Cleanups.DEFAULT_SAVE_ACTIONS);
           cleanupsDisableProperty().setValue(!Cleanups.DEFAULT_SAVE_ACTIONS.isEnabled());
           cleanupsProperty().setValue(FXCollections.observableArrayList(Cleanups.DEFAULT_SAVE_ACTIONS.getConfiguredActions()));
        });
    }

    void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();
        newMetaData.setEncoding(selectedEncodingProperty().getValue());
        newMetaData.setMode(selectedDatabaseModeProperty().getValue());

        String generalFileDirectory = generalFileDirectoryProperty.getValue().trim();
        if (generalFileDirectory.isEmpty()) {
            newMetaData.clearDefaultFileDirectory();
        } else {
            newMetaData.setDefaultFileDirectory(generalFileDirectory);
        }

        String userSpecificFileDirectory = userSpecificFileDirectoryProperty.getValue();
        if (userSpecificFileDirectory.isEmpty()) {
            newMetaData.clearUserFileDirectory(preferences.getUser());
        } else {
            newMetaData.setUserFileDirectory(preferences.getUser(), userSpecificFileDirectory);
        }

        String latexFileDirectory = laTexFileDirectoryProperty.getValue();
        if (latexFileDirectory.isEmpty()) {
            newMetaData.clearLatexFileDirectory(preferences.getUser());
        } else {
            newMetaData.setLatexFileDirectory(preferences.getUser(), Paths.get(latexFileDirectory));
        }

        if (libraryProtectedProperty.getValue()) {
            newMetaData.markAsProtected();
        } else {
            newMetaData.markAsNotProtected();
        }

        FieldFormatterCleanups fieldFormatterCleanups = new FieldFormatterCleanups(
                !cleanupsDisableProperty().getValue(),
                cleanupsProperty());

        if (Cleanups.DEFAULT_SAVE_ACTIONS.equals(fieldFormatterCleanups)) {
            newMetaData.clearSaveActions();
        } else {
            // if all actions have been removed, remove the save actions from the MetaData
            if (fieldFormatterCleanups.getConfiguredActions().isEmpty()) {
                newMetaData.clearSaveActions();
            } else {
                newMetaData.setSaveActions(fieldFormatterCleanups);
            }
        }

        databaseContext.setMetaData(newMetaData);

        // ToDo: After untangeling BasePanel and UndoManager
        /* if (!initialMetaData.equals(newMetaData)) {
            panel.markNonUndoableBaseChanged();
        } */
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
        return selectedEncodingPropety;
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

    public BooleanProperty protectDisableProperty() {
        return protectDisableProperty;
    }

    public BooleanProperty libraryProtectedProperty() {
        return libraryProtectedProperty;
    }

    public BooleanProperty cleanupsDisableProperty() { return cleanupsDisableProperty; }

    public ListProperty<FieldFormatterCleanup> cleanupsProperty() { return cleanupsProperty; }
}
