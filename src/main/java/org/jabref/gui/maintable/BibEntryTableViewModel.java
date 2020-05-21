package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
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
    private final BibDatabase database;
    private final MainTableNameFormatter nameFormatter;
    private final Map<OrFields, ObservableValue<String>> fieldValues = new HashMap<>();
    private final Map<SpecialField, OptionalBinding<SpecialFieldValueViewModel>> specialFieldValues = new HashMap<>();
    private final EasyBinding<List<LinkedFile>> linkedFiles;
    private final EasyBinding<Map<Field, String>> linkedIdentifiers;
    private final ObservableValue<List<AbstractGroup>> matchedGroups;

    public BibEntryTableViewModel(BibEntry entry, BibDatabaseContext database, MainTableNameFormatter nameFormatter) {
        this.entry = entry;
        this.database = database.getDatabase();
        this.nameFormatter = nameFormatter;

        this.linkedFiles = getField(StandardField.FILE).map(FileFieldParser::parse).orElse(Collections.emptyList());
        this.linkedIdentifiers = createLinkedIdentifiersBinding(entry);
        this.matchedGroups = createMatchedGroupsBinding(database, entry);
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

    private static ObservableValue<List<AbstractGroup>> createMatchedGroupsBinding(BibDatabaseContext database, BibEntry entry) {
        Optional<GroupTreeNode> root = database.getMetaData().getGroups();
        if (root.isPresent()) {
            return EasyBind.map(entry.getFieldBinding(StandardField.GROUPS), field -> {
                List<AbstractGroup> groups = root.get().getMatchingGroups(entry)
                                                 .stream()
                                                 .map(GroupTreeNode::getGroup)
                                                 .collect(Collectors.toList());
                groups.remove(root.get().getGroup());
                return groups;
            });
        }
        return new SimpleObjectProperty<>(Collections.emptyList());
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
        } else {
            value = Bindings.createStringBinding(() -> {
                boolean isName = false;

                Optional<String> content = Optional.empty();
                for (Field field : fields) {
                    content = entry.getResolvedFieldOrAliasLatexFree(field, database);
                    if (content.isPresent()) {
                        isName = field.getProperties().contains(FieldProperty.PERSON_NAMES);
                        break;
                    }
                }

                String result = content.orElse(null);
                if (isName) {
                    return nameFormatter.formatName(result);
                } else {
                    return result;
                }
            }, entry.getObservables());
            fieldValues.put(fields, value);
            return value;
        }
    }
}
