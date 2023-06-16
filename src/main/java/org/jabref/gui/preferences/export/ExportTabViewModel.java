package org.jabref.gui.preferences.export;

import java.util.ArrayList;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.preferences.ExportPreferences;

public class ExportTabViewModel implements PreferenceTabViewModel {

    // SaveOrderConfigPanel
    private final BooleanProperty exportInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInSpecifiedOrderProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));

    private final ExportPreferences exportPreferences;

    public ExportTabViewModel(ExportPreferences exportPreferences) {
        this.exportPreferences = exportPreferences;
    }

    @Override
    public void setValues() {
        SaveOrder exportSaveOrder = exportPreferences.getExportSaveOrder();
        switch (exportSaveOrder.getOrderType()) {
            case SPECIFIED -> exportInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL -> exportInOriginalProperty.setValue(true);
            case TABLE -> exportInTableOrderProperty.setValue(true);
        }
        sortCriteriaProperty.addAll(exportSaveOrder.getSortCriteria().stream()
                                                   .map(SortCriterionViewModel::new)
                                                   .toList());

        Set<Field> fields = FieldFactory.getAllFieldsWithOutInternal();
        fields.add(InternalField.INTERNAL_ALL_FIELD);
        fields.add(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);
        fields.add(InternalField.KEY_FIELD);
        fields.add(InternalField.TYPE_HEADER);
        sortableFieldsProperty.addAll(FieldFactory.getStandardFieldsWithCitationKey());
    }

    @Override
    public void storeSettings() {
        SaveOrder newSaveOrder = new SaveOrder(
                SaveOrder.OrderType.fromBooleans(exportInSpecifiedOrderProperty.getValue(), exportInOriginalProperty.getValue()),
                sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList());
        exportPreferences.setExportSaveOrder(newSaveOrder);
    }

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
}
