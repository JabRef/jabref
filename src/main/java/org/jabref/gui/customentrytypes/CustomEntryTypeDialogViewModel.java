package org.jabref.gui.customentrytypes;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;

import org.fxmisc.easybind.EasyBind;

public class CustomEntryTypeDialogViewModel {

    private ListProperty<BibEntryType> entryTypesProperty;
    private ListProperty<Field> fieldsProperty;
    private ObjectProperty<BibEntryType> selectedEntryTypesProperty = new SimpleObjectProperty<>();
    private ListProperty<FieldViewModel> fieldsForTypeProperty = new SimpleListProperty<>(FXCollections.observableArrayList());

    public CustomEntryTypeDialogViewModel() {

        entryTypesProperty = new SimpleListProperty<BibEntryType>(FXCollections.observableArrayList(BiblatexEntryTypeDefinitions.ALL));
        fieldsProperty = new SimpleListProperty<Field>(FXCollections.observableArrayList(FieldFactory.getAllFields()));

        EasyBind.subscribe(selectedEntryTypesProperty, type -> {
            if (type != null) {
                List<FieldViewModel> fields = type.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), type.isRequired(bibField.getField()))).collect(Collectors.toList());
                this.fieldsForTypeProperty.setValue(FXCollections.observableArrayList(fields));
            }
        });

    }

    public ListProperty<BibEntryType> entryTypesProperty() {
        return this.entryTypesProperty;
    }

    public ListProperty<Field> fieldsProperty() {
        return this.fieldsProperty;
    }

    public enum FieldType {

        REQUIRED("Required"),
        OTPIONAL("Optional");

        private String name;

        FieldType(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return this.name;
        }
    }

    public void addNewField() {
        //We need to add a new unknown field

    }

    public void addNewCustomEntryType() {
        // TODO Auto-generated method stub

    }

    public ObjectProperty<BibEntryType> selectedEntryTypeProperty() {
        return this.selectedEntryTypesProperty;
    }

    public ListProperty<FieldViewModel> fieldsforTypesProperty() {
        return this.fieldsForTypeProperty;
    }

}
