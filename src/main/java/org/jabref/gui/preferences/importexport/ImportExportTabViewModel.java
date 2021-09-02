package org.jabref.gui.preferences.importexport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

public class ImportExportTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    // SaveOrderConfigPanel
    private final BooleanProperty exportInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInSpecifiedOrderProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));

    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidURLProperty = new SimpleStringProperty("");

    private final PreferencesService preferencesService;
    private final DOIPreferences doiPreferences;
    private final ImporterPreferences importerPreferences;
    private final SaveOrderConfig initialExportOrder;

    public ImportExportTabViewModel(PreferencesService preferencesService, DOIPreferences doiPreferences) {
        this.preferencesService = preferencesService;
        this.importerPreferences = preferencesService.getImporterPreferences();
        this.doiPreferences = doiPreferences;
        this.initialExportOrder = preferencesService.getExportSaveOrder();
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(importerPreferences.isGenerateNewKeyOnImport());
        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        switch (initialExportOrder.getOrderType()) {
            case SPECIFIED -> exportInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL -> exportInOriginalProperty.setValue(true);
            case TABLE -> exportInTableOrderProperty.setValue(true);
        }

        List<Field> fieldNames = new ArrayList<>(FieldFactory.getCommonFields());
        fieldNames.sort(Comparator.comparing(Field::getDisplayName));

        sortableFieldsProperty.addAll(fieldNames);
        sortCriteriaProperty.addAll(initialExportOrder.getSortCriteria().stream()
                                                      .map(SortCriterionViewModel::new)
                                                      .collect(Collectors.toList()));

        grobidEnabledProperty.setValue(importerPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(importerPreferences.getGrobidURL());
    }

    @Override
    public void storeSettings() {
        importerPreferences.setGenerateNewKeyOnImport(generateKeyOnImportProperty.getValue());
        importerPreferences.setGrobidEnabled(grobidEnabledProperty.getValue());
        importerPreferences.setGrobidOptOut(importerPreferences.isGrobidOptOut());
        importerPreferences.setGrobidURL(grobidURLProperty.getValue());

        doiPreferences.setUseCustom(useCustomDOIProperty.get());
        doiPreferences.setDefaultBaseURI(useCustomDOINameProperty.getValue().trim());

        SaveOrderConfig newSaveOrderConfig = new SaveOrderConfig(
                SaveOrderConfig.OrderType.fromBooleans(exportInSpecifiedOrderProperty.getValue(), exportInTableOrderProperty.getValue()),
                sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList());
        preferencesService.storeExportSaveOrder(newSaveOrderConfig);
    }

    public BooleanProperty generateKeyOnImportProperty() {
        return generateKeyOnImportProperty;
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    // SaveOrderConfigPanel

    public BooleanProperty saveInOriginalProperty() {
        return exportInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return exportInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return exportInSpecifiedOrderProperty;
    }

    public ListProperty<Field> sortableFieldsProperty() {
        return sortableFieldsProperty;
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return sortCriteriaProperty;
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public StringProperty grobidURLProperty() {
        return grobidURLProperty;
    }
}
