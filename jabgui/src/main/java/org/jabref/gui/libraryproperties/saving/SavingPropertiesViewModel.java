package org.jabref.gui.libraryproperties.saving;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;

public class SavingPropertiesViewModel implements PropertiesTabViewModel {

    private static final SaveOrder UI_DEFAULT_SAVE_ORDER = new SaveOrder(SaveOrder.OrderType.ORIGINAL, List.of(
            new SaveOrder.SortCriterion(StandardField.AUTHOR),
            new SaveOrder.SortCriterion(StandardField.YEAR),
            new SaveOrder.SortCriterion(StandardField.TITLE),
            // Pro users generate their citation keys well. They can just delete the above three proposals and get a well-sorted library.
            new SaveOrder.SortCriterion(InternalField.KEY_FIELD)
    ));

    private final BooleanProperty protectDisableProperty = new SimpleBooleanProperty();
    private final BooleanProperty libraryProtectedProperty = new SimpleBooleanProperty();

    // SaveOrderConfigPanel
    private final BooleanProperty saveInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty saveInSpecifiedOrderProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));

    private final BibDatabaseContext databaseContext;
    private final MetaData initialMetaData;
    private final SaveOrder saveOrder;

    public SavingPropertiesViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.initialMetaData = databaseContext.getMetaData();
        this.saveOrder = initialMetaData.getSaveOrder().orElse(UI_DEFAULT_SAVE_ORDER);
    }

    @Override
    public void setValues() {
        libraryProtectedProperty.setValue(initialMetaData.isProtected());

        // SaveOrderConfigPanel, included via <?import ...> in FXML

        switch (saveOrder.getOrderType()) {
            case SPECIFIED ->
                    saveInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL ->
                    saveInOriginalProperty.setValue(true);
            case TABLE ->
                    saveInTableOrderProperty.setValue(true);
        }

        sortableFieldsProperty.clear();

        Set<Field> fields = FieldFactory.getAllFieldsWithOutInternal();
        fields.add(InternalField.INTERNAL_ALL_FIELD);
        fields.add(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD);
        fields.add(InternalField.KEY_FIELD);
        fields.add(InternalField.TYPE_HEADER);

        sortableFieldsProperty.addAll(FieldFactory.getStandardFieldsWithCitationKey());
        sortCriteriaProperty.clear();
        sortCriteriaProperty.addAll(saveOrder.getSortCriteria().stream()
                                             .map(SortCriterionViewModel::new)
                                             .toList());
    }

    @Override
    public void storeSettings() {
        MetaData newMetaData = databaseContext.getMetaData();

        if (libraryProtectedProperty.getValue()) {
            newMetaData.markAsProtected();
        } else {
            newMetaData.markAsNotProtected();
        }

        SaveOrder newSaveOrder = new SaveOrder(
                SaveOrder.OrderType.fromBooleans(saveInSpecifiedOrderProperty.getValue(), saveInOriginalProperty.getValue()),
                sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList());

        if (!newSaveOrder.equals(saveOrder)) {
            if (newSaveOrder.equals(SaveOrder.getDefaultSaveOrder())) {
                newMetaData.clearSaveOrder();
            } else {
                newMetaData.setSaveOrder(newSaveOrder);
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
}
