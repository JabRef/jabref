package org.jabref.gui.customentrytypes;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntryType;

public class CustomEntryTypeViewModel {

    private final ObjectProperty<BibEntryType> entryType = new SimpleObjectProperty<>();
    private final ListProperty<FieldViewModel> fields;

    public CustomEntryTypeViewModel(BibEntryType entryType) {
        this.entryType.set(entryType);

        List<FieldViewModel> types = entryType.getAllFields().stream().map(bibField -> new FieldViewModel(bibField.getField(), entryType.isRequired(bibField.getField()), bibField.getPriority(), entryType)).collect(Collectors.toList());

        fields = new SimpleListProperty<>(FXCollections.observableArrayList(types));
    }

    public ListProperty<FieldViewModel> fields() {
        return this.fields;
    }

    public ObjectProperty<BibEntryType> entryType() {
        return this.entryType;
    }

}
