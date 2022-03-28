package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.util.uithreadaware.UiThreadBinding;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupTreeNode;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import com.tobiasdiez.easybind.optional.OptionalBinding;

public class BibEntryTableViewModel {

    private final BibEntry entry;
    private final ObservableValue<MainTableFieldValueFormatter> fieldValueFormatter;
    private final Map<OrFields, ObservableValue<String>> fieldValues = new HashMap<>();
    private final Map<SpecialField, OptionalBinding<SpecialFieldValueViewModel>> specialFieldValues = new HashMap<>();
    private final EasyBinding<List<LinkedFile>> linkedFiles;
    private final EasyBinding<Map<Field, String>> linkedIdentifiers;
    private final Binding<List<AbstractGroup>> matchedGroups;
    private final BibDatabaseContext bibDatabaseContext;

    public BibEntryTableViewModel(BibEntry entry, BibDatabaseContext bibDatabaseContext, ObservableValue<MainTableFieldValueFormatter> fieldValueFormatter) {
        this.entry = entry;
        this.fieldValueFormatter = fieldValueFormatter;

        this.linkedFiles = getField(StandardField.FILE).map(FileFieldParser::parse).orElse(Collections.emptyList());
        this.linkedIdentifiers = createLinkedIdentifiersBinding(entry);
        this.matchedGroups = createMatchedGroupsBinding(bibDatabaseContext, entry);
        this.bibDatabaseContext = bibDatabaseContext;
    }

    private static EasyBinding<Map<Field, String>> createLinkedIdentifiersBinding(BibEntry entry) {
        return EasyBind.combine(
                entry.getFieldBinding(StandardField.URL),
                entry.getFieldBinding(StandardField.DOI),
                entry.getFieldBinding(StandardField.URI),
                entry.getFieldBinding(StandardField.EPRINT),
                (url, doi, uri, eprint) -> {
                    Map<Field, String> identifiers = new HashMap<>();
                    url.ifPresent(value -> identifiers.put(StandardField.URL, value));
                    doi.ifPresent(value -> identifiers.put(StandardField.DOI, value));
                    uri.ifPresent(value -> identifiers.put(StandardField.URI, value));
                    eprint.ifPresent(value -> identifiers.put(StandardField.EPRINT, value));
                    return identifiers;
                });
    }

    public BibEntry getEntry() {
        return entry;
    }

    private static Binding<List<AbstractGroup>> createMatchedGroupsBinding(BibDatabaseContext database, BibEntry entry) {
        return new UiThreadBinding<>(EasyBind.combine(entry.getFieldBinding(StandardField.GROUPS), database.getMetaData().groupsBinding(),
                (a, b) ->
                        database.getMetaData().getGroups().map(groupTreeNode ->
                                groupTreeNode.getMatchingGroups(entry).stream()
                                             .map(GroupTreeNode::getGroup)
                                             .filter(Predicate.not(Predicate.isEqual(groupTreeNode.getGroup())))
                                             .collect(Collectors.toList()))
                                .orElse(Collections.emptyList())));
    }

    public OptionalBinding<String> getField(Field field) {
        return entry.getFieldBinding(field);
    }

    public ObservableValue<List<LinkedFile>> getLinkedFiles() {
        return linkedFiles;
    }

    public ObservableValue<Map<Field, String>> getLinkedIdentifiers() {
        return linkedIdentifiers;
    }

    public ObservableValue<List<AbstractGroup>> getMatchedGroups() {
        return matchedGroups;
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        OptionalBinding<SpecialFieldValueViewModel> value = specialFieldValues.get(field);
        if (value != null) {
            return value;
        } else {
            value = getField(field).flatMap(fieldValue -> field.parseValue(fieldValue).map(SpecialFieldValueViewModel::new));
            specialFieldValues.put(field, value);
            return value;
        }
    }

    public ObservableValue<String> getFields(OrFields fields) {
        ObservableValue<String> value = fieldValues.get(fields);
        if (value != null) {
            return value;
        }

        ArrayList<Observable> observables = new ArrayList<>(List.of(entry.getObservables()));
        observables.add(fieldValueFormatter);

        value = Bindings.createStringBinding(() ->
                        fieldValueFormatter.getValue().formatFieldsValues(fields, entry),
                observables.toArray(Observable[]::new));
        fieldValues.put(fields, value);
        return value;
    }

    public StringProperty bibDatabaseContextProperty() {
        return new ReadOnlyStringWrapper(bibDatabaseContext.getDatabasePath().map(Path::toString).orElse(""));
    }
}
