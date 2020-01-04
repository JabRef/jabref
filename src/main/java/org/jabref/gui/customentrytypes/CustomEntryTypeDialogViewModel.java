package org.jabref.gui.customentrytypes;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;

public class CustomEntryTypeDialogViewModel {

    private ListProperty<BibEntryType> entryTypesProperty;
    private ListProperty<Field> fieldsProperty;

    public CustomEntryTypeDialogViewModel() {

        entryTypesProperty = new SimpleListProperty<BibEntryType>(FXCollections.observableArrayList(BiblatexEntryTypeDefinitions.ALL));
        fieldsProperty = new SimpleListProperty<Field>(FXCollections.observableArrayList(FieldFactory.getAllFields()));
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
}
