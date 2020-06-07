package org.jabref.gui.customentrytypes;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.BibEntryType;

public class CustomEntryTypeViewModel {

    private final ObjectProperty<BibEntryType> entryType = new SimpleObjectProperty<>();
    private final ObservableList<FieldViewModel> fields;

    public CustomEntryTypeViewModel(BibEntryType entryType) {
        this.entryType.set(entryType);

        List<FieldViewModel> allFieldsForType = entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority())).collect(Collectors.toList());
        fields = FXCollections.observableArrayList(allFieldsForType);
    }

    public void addField(FieldViewModel field) {
        this.fields.add(field);
    }

    public ObservableList<FieldViewModel> fields() {
        return this.fields;
    }

    public ObjectProperty<BibEntryType> entryType() {
        return this.entryType;
    }

    public void removeField(FieldViewModel focusedItem) {
        this.fields.remove(focusedItem);
    }

    @Override
    public String toString() {
        return "CustomEntryTypeViewModel [entryType=" + entryType + ", fields=" + fields + "]";
    }


}
