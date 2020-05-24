package org.jabref.gui.customentrytypes;

import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntryType;

public class EntryTypeViewModel {

    private final ObjectProperty<BibEntryType> entryType = new SimpleObjectProperty<>();
    private final ListProperty<FieldViewModel> fields = new SimpleListProperty<>(FXCollections.emptyObservableList());

    public EntryTypeViewModel(BibEntryType entryType) {
        this.entryType.set(entryType);
        this.fields.setAll(entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority(), entryType)).collect(Collectors.toList()));

    }

    public ListProperty<FieldViewModel> fields() {
        return this.fields;
    }

    public ObjectProperty<BibEntryType> entryType() {
        return this.entryType;
    }

}
