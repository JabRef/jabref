package org.jabref.gui.libraryproperties;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
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

    // SaveOrderConfigPanel
    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();
    // ToDo: The single criterions should really be a map or a list.
    private final ListProperty<Field> primarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> secondarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<Field> tertiarySortFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty savePrimaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveSecondaryDescPropertySelected = new SimpleBooleanProperty();
    private final BooleanProperty saveTertiaryDescPropertySelected = new SimpleBooleanProperty();
    private final ObjectProperty<Field> savePrimarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveSecondarySortSelectedValueProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Field> saveTertiarySortSelectedValueProperty = new SimpleObjectProperty<>(null);

    // FieldFormatterCleanupsPanel
    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> cleanupsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final DialogService dialogService;

    private final DirectoryDialogConfiguration directoryDialogConfiguration;
    private final MetaData initialMetaData;
    private final SaveOrderConfig initialSaveOrderConfig;

    public LibraryPropertiesDialogViewModel(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferences) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialMetaData = databaseContext.getMetaData();
        this.initialSaveOrderConfig = initialMetaData.getSaveOrderConfig().orElseGet(preferences::loadExportSaveOrder);

        this.directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getWorkingDir()).build();

        setValues();
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

        // SaveOrderConfigPanel

        if (initialSaveOrderConfig.saveInOriginalOrder()) {
            saveInOriginalProperty.setValue(true);
        } else if (initialSaveOrderConfig.saveInSpecifiedOrder()) {
            saveInSpecifiedOrderProperty.setValue(true);
        } else {
            saveInTableOrderProperty.setValue(true);
        }

        List<Field> fieldNames = new ArrayList<>(FieldFactory.getCommonFields());
        // allow entrytype field as sort criterion
        fieldNames.add(InternalField.TYPE_HEADER);
        fieldNames.sort(Comparator.comparing(Field::getDisplayName));
        primarySortFieldsProperty.addAll(fieldNames);
        secondarySortFieldsProperty.addAll(fieldNames);
        tertiarySortFieldsProperty.addAll(fieldNames);

        savePrimarySortSelectedValueProperty.setValue(initialSaveOrderConfig.getSortCriteria().get(0).field);
        saveSecondarySortSelectedValueProperty.setValue(initialSaveOrderConfig.getSortCriteria().get(1).field);
        saveTertiarySortSelectedValueProperty.setValue(initialSaveOrderConfig.getSortCriteria().get(2).field);

        savePrimaryDescPropertySelected.setValue(initialSaveOrderConfig.getSortCriteria().get(0).descending);
        saveSecondaryDescPropertySelected.setValue(initialSaveOrderConfig.getSortCriteria().get(1).descending);
        saveTertiaryDescPropertySelected.setValue(initialSaveOrderConfig.getSortCriteria().get(2).descending);

        // FieldFormatterCleanupsPanel

        Optional<FieldFormatterCleanups> saveActions = initialMetaData.getSaveActions();
        saveActions.ifPresentOrElse(value -> {
            cleanupsDisableProperty().setValue(!value.isEnabled());
            cleanupsProperty().setValue(FXCollections.observableArrayList(value.getConfiguredActions()));
        }, () -> {
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
            newMetaData.setLatexFileDirectory(preferences.getUser(), Path.of(latexFileDirectory));
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

        SaveOrderConfig newSaveOrderConfig = new SaveOrderConfig(
                saveInOriginalProperty.getValue(),
                saveInSpecifiedOrderProperty.getValue(),
                new SaveOrderConfig.SortCriterion(
                        savePrimarySortSelectedValueProperty.get(),
                        savePrimaryDescPropertySelected.getValue()),
                new SaveOrderConfig.SortCriterion(
                        saveSecondarySortSelectedValueProperty.get(),
                        saveSecondaryDescPropertySelected.getValue()),
                new SaveOrderConfig.SortCriterion(
                        saveTertiarySortSelectedValueProperty.get(),
                        saveTertiaryDescPropertySelected.getValue()));

        if (!newSaveOrderConfig.equals(initialSaveOrderConfig)) {
            if (newSaveOrderConfig.equals(SaveOrderConfig.getDefaultSaveOrder())) {
                newMetaData.clearSaveOrderConfig();
            } else {
                newMetaData.setSaveOrderConfig(newSaveOrderConfig);
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

    // SaveOrderConfigPanel

    public BooleanProperty saveInOriginalProperty() {
        return saveInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return saveInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return saveInSpecifiedOrderProperty;
    }

    public ListProperty<Field> primarySortFieldsProperty() {
        return primarySortFieldsProperty;
    }

    public ListProperty<Field> secondarySortFieldsProperty() {
        return secondarySortFieldsProperty;
    }

    public ListProperty<Field> tertiarySortFieldsProperty() {
        return tertiarySortFieldsProperty;
    }

    public ObjectProperty<Field> savePrimarySortSelectedValueProperty() {
        return savePrimarySortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveSecondarySortSelectedValueProperty() {
        return saveSecondarySortSelectedValueProperty;
    }

    public ObjectProperty<Field> saveTertiarySortSelectedValueProperty() {
        return saveTertiarySortSelectedValueProperty;
    }

    public BooleanProperty savePrimaryDescPropertySelected() {
        return savePrimaryDescPropertySelected;
    }

    public BooleanProperty saveSecondaryDescPropertySelected() {
        return saveSecondaryDescPropertySelected;
    }

    public BooleanProperty saveTertiaryDescPropertySelected() {
        return saveTertiaryDescPropertySelected;
    }

    // FieldFormatterCleanupsPanel

    public BooleanProperty cleanupsDisableProperty() {
        return cleanupsDisableProperty;
    }

    public ListProperty<FieldFormatterCleanup> cleanupsProperty() {
        return cleanupsProperty;
    }
}
