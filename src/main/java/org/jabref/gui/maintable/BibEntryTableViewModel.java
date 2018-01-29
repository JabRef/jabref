package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.specialfields.SpecialField;

import org.fxmisc.easybind.EasyBind;

public class BibEntryTableViewModel {
    private final BibEntry entry;

    public BibEntryTableViewModel(BibEntry entry) {
        this.entry = entry;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public Optional<String> getResolvedFieldOrAlias(String field, BibDatabase database) {
        return entry.getResolvedFieldOrAlias(field, database);
    }

    public ObjectBinding<String> getField(String fieldName) {
        return entry.getFieldBinding(fieldName);
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        return EasyBind.map(getField(field.getFieldName()),
                value -> field.parse(value).map(SpecialFieldValueViewModel::new));
    }

    public ObservableValue<List<LinkedFile>> getLinkedFiles() {
        return EasyBind.map(getField(FieldName.FILE), FileFieldParser::parse);
    }
}
