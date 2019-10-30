package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

import org.fxmisc.easybind.EasyBind;

public class BibEntryTableViewModel {
    private final BibEntry entry;

    public BibEntryTableViewModel(BibEntry entry) {
        this.entry = entry;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public Optional<String> getResolvedFieldOrAlias(Field field, BibDatabase database) {
        return entry.getResolvedFieldOrAlias(field, database);
    }

    public ObjectBinding<String> getField(Field field) {
        return entry.getFieldBinding(field);
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        return EasyBind.map(getField(field), value -> field.parseValue(value).map(SpecialFieldValueViewModel::new));
    }

    public ObservableValue<List<LinkedFile>> getLinkedFiles() {
        return EasyBind.map(getField(StandardField.FILE), FileFieldParser::parse);
    }

    public ObservableValue<Map<Field,String>> getLinkedIdentifiers() {
        SimpleObjectProperty<Map<Field,String>> linkedIdentifiers = new SimpleObjectProperty<>(new HashMap<>());

        entry.getField(StandardField.URL).ifPresent(value -> linkedIdentifiers.getValue().put(StandardField.URL, value));
        entry.getField(StandardField.DOI).ifPresent(value -> linkedIdentifiers.getValue().put(StandardField.DOI, value));
        entry.getField(StandardField.URI).ifPresent(value -> linkedIdentifiers.getValue().put(StandardField.URI, value));
        entry.getField(StandardField.EPRINT).ifPresent(value -> linkedIdentifiers.getValue().put(StandardField.EPRINT, value));

        return linkedIdentifiers;
    }

    public ObservableValue<List<AbstractGroup>> getMatchedGroups(BibDatabaseContext database) {
        SimpleObjectProperty<List<AbstractGroup>> matchedGroups = new SimpleObjectProperty<>(Collections.emptyList());

        Optional<GroupTreeNode> root = database.getMetaData()
                                               .getGroups();
        if (root.isPresent()) {
            List<AbstractGroup> groups = root.get().getMatchingGroups(entry)
                                             .stream()
                                             .map(GroupTreeNode::getGroup)
                                             .collect(Collectors.toList());
            groups.remove(root.get().getGroup());
            matchedGroups.setValue(groups);
        }

        return matchedGroups;
    }
}
