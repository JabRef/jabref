package org.jabref.gui.customentrytypes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.BibEntryType;

public class EntryTypeViewModel {

    private final ObjectProperty<BibEntryType> entryType = new SimpleObjectProperty<>();
    private final ObservableList<FieldViewModel> fields;

    public EntryTypeViewModel(BibEntryType entryType) {
        this.entryType.set(entryType);

        List<FieldViewModel> allFieldsForType = entryType.getAllBibFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority())).collect(Collectors.toList());
        fields = FXCollections.observableArrayList((allFieldsForType));
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryType, fields);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntryTypeViewModel)) {
            return false;
        }
        EntryTypeViewModel other = (EntryTypeViewModel) obj;
        return Objects.equals(entryType, other.entryType) && Objects.equals(fields, other.fields);
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
