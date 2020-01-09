package org.jabref.gui.customentrytypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.StringConverter;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.fxmisc.easybind.EasyBind;

public class CustomEntryTypeDialogViewModel {

    public static final StringConverter<Field> fieldStringConverter = new StringConverter<Field>() {

        @Override
        public String toString(Field object) {
            return object != null ? object.getDisplayName() : "";
        }

        @Override
        public Field fromString(String string) {
            return new UnknownField(string);
        }
    };

    private ListProperty<BibEntryType> entryTypesProperty;
    private ListProperty<Field> fieldsProperty;
    private ObjectProperty<BibEntryType> selectedEntryTypesProperty = new SimpleObjectProperty<>();
    private ListProperty<FieldViewModel> fieldsForTypeProperty;
    private ObjectProperty<Field> selectedFieldToAddProperty = new SimpleObjectProperty<>();
    private StringProperty entryTypeToAddProperty = new SimpleStringProperty("");
    private ObservableList<BibEntryType> entryTypes;
    private ObservableList<FieldViewModel> existingFieldsForType = FXCollections.observableArrayList(extractor -> new Observable[] {extractor.fieldNameProperty(), extractor.fieldTypeProperty()});
    private ObjectProperty<Field> newFieldToAddProperty = new SimpleObjectProperty<>();
    private BibDatabaseMode mode;
    private Map<BibEntryType, List<FieldViewModel>> typesWithField = new HashMap<>();

    public CustomEntryTypeDialogViewModel(BibDatabaseMode mode) {
        this.mode = mode;

        List<BibEntryType> alllTypes = mode == mode.BIBLATEX ? BiblatexEntryTypeDefinitions.ALL : BibtexEntryTypeDefinitions.ALL;
        entryTypes = FXCollections.observableArrayList(alllTypes);
        entryTypesProperty = new SimpleListProperty<>(entryTypes);

        fieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList(FieldFactory.getAllFields()));

        for (BibEntryType entryType : alllTypes) {
            List<FieldViewModel> fields = entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), entryType)).collect(Collectors.toList());
            typesWithField.put(entryType, fields);
        }

        this.fieldsForTypeProperty = new SimpleListProperty<>(existingFieldsForType);

        EasyBind.subscribe(selectedEntryTypesProperty, type -> {
            if (type != null) {
                List<FieldViewModel> typesForField = typesWithField.get(type);
                existingFieldsForType.setAll(typesForField);
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

        @Override
        public String toString() {
            return this.name;
        }
    }

    public void addNewField() {

        Field field = newFieldToAddProperty.getValue();
        FieldViewModel model = new FieldViewModel(field, true, selectedEntryTypesProperty.getValue());
        typesWithField.computeIfAbsent(selectedEntryTypesProperty.getValue(), key -> new ArrayList<>()).add(model);
        existingFieldsForType.add(model);
        //TODO: How should I add the field to the type?

        //BibEntryType type = new BibEntryType(type, fields, requiredFields)
        //   Globals.entryTypesManager.addCustomOrModifiedType(entryType, mode);

    }

    public void addNewCustomEntryType() {
        EntryType newentryType = new UnknownEntryType(entryTypeToAddProperty.getValue());
        BibEntryType type = new BibEntryType(newentryType, new ArrayList<>(), Collections.emptyList());
        this.entryTypes.add(type);

        this.typesWithField.put(type, new ArrayList<>());

        // entryTypesManager.addCustomOrModifiedType(overwrittenStandardType ?
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

    public ObjectProperty<Field> newFieldToAddProperty() {
        return this.newFieldToAddProperty;
    }

    public void removeEntryType(BibEntryType focusedItem) {
    }

    public void removeField(FieldViewModel focusedItem) {
        // TODO Auto-generated method stub
    }
}
