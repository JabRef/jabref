package org.jabref.gui.customentrytypes;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    private ObjectProperty<Field> selectedFieldToAddProperty = new SimpleObjectProperty<>();
    private StringProperty entryTypeToAddProperty = new SimpleStringProperty("");
    private ObservableList<BibEntryType> entryTypes;

    public CustomEntryTypeDialogViewModel() {

        entryTypes = FXCollections.observableArrayList(BiblatexEntryTypeDefinitions.ALL);
        entryTypesProperty = new SimpleListProperty<>(entryTypes);
        
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

        //Field fieldToAdd = new UnknownField(name)

    }

    public void addNewCustomEntryType() {

        // BibEntryTypeBuilder
        //new UnknownEntryType(null).

    }

    public ObjectProperty<BibEntryType> selectedEntryTypeProperty() {
        return this.selectedEntryTypesProperty;
    }

    public ListProperty<FieldViewModel> fieldsforTypesProperty() {
        return this.fieldsForTypeProperty;
    }

    public ObjectProperty<Field> selectedFieldToAddProperty() {
        return this.selectedFieldToAddProperty;
    }

    public StringProperty entryTypeToAddProperty() {
        return this.entryTypeToAddProperty;
    }
}
