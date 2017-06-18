package org.jabref.gui.fieldeditors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.entry.BibEntry;

public class AbstractEditorViewModel extends AbstractViewModel {
    protected StringProperty text = new SimpleStringProperty("");
    protected BibEntry entry;

    public StringProperty textProperty() {
        return text;
    }

    public void bindToEntry(String fieldName, BibEntry entry) {
        this.entry = entry;
        BindingsHelper.bindBidirectional(
                this.textProperty(),
                entry.getFieldBinding(fieldName),
                newValue -> {
                    if (newValue != null) {
                        entry.setField(fieldName, newValue);
                    }
                });
    }
}
