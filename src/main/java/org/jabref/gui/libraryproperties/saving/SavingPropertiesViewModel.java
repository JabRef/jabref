package org.jabref.gui.libraryproperties.saving;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

public class SavingPropertiesViewModel implements PropertiesTabViewModel {

    private final BooleanProperty protectDisableProperty = new SimpleBooleanProperty();
    private final BooleanProperty libraryProtectedProperty = new SimpleBooleanProperty();

    // SaveOrderConfigPanel
    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));

    // FieldFormatterCleanupsPanel
    private final BooleanProperty cleanupsDisableProperty = new SimpleBooleanProperty();
    private final ListProperty<FieldFormatterCleanup> cleanupsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    private final BibDatabaseContext databaseContext;
    private final MetaData initialMetaData;
    private final SaveOrderConfig initialSaveOrderConfig;

    public SavingPropertiesViewModel(BibDatabaseContext databaseContext, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.initialMetaData = databaseContext.getMetaData();
        this.initialSaveOrderConfig = initialMetaData.getSaveOrderConfig().orElseGet(preferencesService::getExportSaveOrder);
    }

    @Override
    public void setValues() {
        libraryProtectedProperty.setValue(initialMetaData.isProtected());

        // SaveOrderConfigPanel

        switch (initialSaveOrderConfig.getOrderType()) {
            case SPECIFIED -> saveInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL -> saveInOriginalProperty.setValue(true);
            case TABLE -> saveInTableOrderProperty.setValue(true);
        }

        List<Field> fieldNames = new ArrayList<>(FieldFactory.getCommonFields());
        fieldNames.add(InternalField.TYPE_HEADER); // allow entrytype field as sort criterion
        fieldNames.sort(Comparator.comparing(Field::getDisplayName));
        sortableFieldsProperty.addAll(fieldNames);
        sortCriteriaProperty.addAll(initialSaveOrderConfig.getSortCriteria().stream()
                                                          .map(SortCriterionViewModel::new)
                                                          .collect(Collectors.toList()));

        // FieldFormatterCleanupsPanel

        Optional<FieldFormatterCleanups> saveActions = initialMetaData.getSaveActions();
        saveActions.ifPresentOrElse(value -> {
            cleanupsDisableProperty.setValue(!value.isEnabled());
            cleanupsProperty.setValue(FXCollections.observableArrayList(value.getConfiguredActions()));
        }, () -> {
            cleanupsDisableProperty.setValue(!Cleanups.DEFAULT_SAVE_ACTIONS.isEnabled());
            cleanupsProperty.setValue(FXCollections.observableArrayList(Cleanups.DEFAULT_SAVE_ACTIONS.getConfiguredActions()));
        });
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();

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
                SaveOrderConfig.OrderType.fromBooleans(saveInSpecifiedOrderProperty.getValue(), saveInOriginalProperty.getValue()),
                sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList());

        if (!newSaveOrderConfig.equals(initialSaveOrderConfig)) {
            if (newSaveOrderConfig.equals(SaveOrderConfig.getDefaultSaveOrder())) {
                newMetaData.clearSaveOrderConfig();
            } else {
                newMetaData.setSaveOrderConfig(newSaveOrderConfig);
            }
        }

        databaseContext.setMetaData(newMetaData);
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

    public ListProperty<Field> sortableFieldsProperty() {
        return sortableFieldsProperty;
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return sortCriteriaProperty;
    }

    // FieldFormatterCleanupsPanel

    public BooleanProperty cleanupsDisableProperty() {
        return cleanupsDisableProperty;
    }

    public ListProperty<FieldFormatterCleanup> cleanupsProperty() {
        return cleanupsProperty;
    }
}
