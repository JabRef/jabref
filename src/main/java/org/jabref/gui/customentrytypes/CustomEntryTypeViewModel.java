package org.jabref.gui.customentrytypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import org.jabref.model.entry.BibEntryType;

public class CustomEntryTypeViewModel {

    private final ObjectProperty<BibEntryType> entryType = new SimpleObjectProperty<>();
    private final ListProperty<FieldViewModel> fields;
    private final List<FieldViewModel> fieldsToRemove = new ArrayList<>();

    public CustomEntryTypeViewModel(BibEntryType entryType) {
        this.entryType.set(entryType);

        List<FieldViewModel> allFieldsForType = entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority(), entryType)).collect(Collectors.toList());

        fields = new SimpleListProperty<>(FXCollections.observableArrayList(allFieldsForType));

        fields.addListener((ListChangeListener<? super FieldViewModel>) change -> {

            while (change.next()) {
                if (change.wasRemoved()) {
                    fieldsToRemove.addAll(change.getAddedSubList());
                }
            }
        });

    }

    public void addField(FieldViewModel field) {
        this.fields.add(field);
    }

    public ListProperty<FieldViewModel> fields() {
        return this.fields;
    }

    public ObjectProperty<BibEntryType> entryType() {
        return this.entryType;
    }

    public void removeField(FieldViewModel focusedItem) {
        this.fields.remove(focusedItem);

    }

}
