package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibEntryTableViewModel {
    static private final Logger LOGGER = LoggerFactory.getLogger(BibEntryTableViewModel.class);
    static private final LatexToUnicodeFormatter LATEXFORMATTER = new LatexToUnicodeFormatter();
    private final BibEntry entry;
    private final BibDatabase database;
    private final MainTableNameFormatter nameFormatter;
    private final Map<OrFields, ObservableValue<String>> fieldValues = new HashMap<>();
    private final Map<SpecialField, ObservableValue<Optional<SpecialFieldValueViewModel>>> specialFieldValues = new HashMap<>();
    private final EasyBinding<List<LinkedFile>> linkedFiles;
    private final ObjectBinding<Map<Field, String>> linkedIdentifiers;
    private final ObservableValue<List<AbstractGroup>> matchedGroups;

    public BibEntryTableViewModel(BibEntry entry, BibDatabaseContext database, MainTableNameFormatter nameFormatter) {
        this.entry = entry;
        this.database = database.getDatabase();
        this.nameFormatter = nameFormatter;

        this.linkedFiles = EasyBind.map(getField(StandardField.FILE), FileFieldParser::parse);
        this.linkedIdentifiers = createLinkedIdentifiersBinding(entry);
        this.matchedGroups = createMatchedGroupsBinding(database);
    }

    private ObjectBinding<Map<Field, String>> createLinkedIdentifiersBinding(BibEntry entry) {
        return Bindings.createObjectBinding(() -> {
                    Map<Field, String> identifiers = new HashMap<>();
                    entry.getField(StandardField.URL).ifPresent(value -> identifiers.put(StandardField.URL, value));
                    entry.getField(StandardField.DOI).ifPresent(value -> identifiers.put(StandardField.DOI, value));
                    entry.getField(StandardField.URI).ifPresent(value -> identifiers.put(StandardField.URI, value));
                    entry.getField(StandardField.EPRINT).ifPresent(value -> identifiers.put(StandardField.EPRINT, value));
                    return identifiers;
                },
                getEntry().getFieldBinding(StandardField.URL),
                getEntry().getFieldBinding(StandardField.DOI),
                getEntry().getFieldBinding(StandardField.URI),
                getEntry().getFieldBinding(StandardField.EPRINT));
    }

    public BibEntry getEntry() {
        return entry;
    }

    public ObjectBinding<String> getField(Field field) {
        return entry.getFieldBinding(field);
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        ObservableValue<Optional<SpecialFieldValueViewModel>> value = specialFieldValues.get(field);
        if (value != null) {
            return value;
        } else {
            value = EasyBind.map(getField(field), fieldValue -> field.parseValue(fieldValue).map(SpecialFieldValueViewModel::new));
            specialFieldValues.put(field, value);
            return value;
        }
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

    private ObservableValue<List<AbstractGroup>> createMatchedGroupsBinding(BibDatabaseContext database) {
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

    public ObservableValue<String> getFields(OrFields fields) {
        ObservableValue<String> value = fieldValues.get(fields);
        if (value != null) {
            return value;
        }

        value = Bindings.createStringBinding(() -> {
            boolean isName = false;

            Optional<String> content = Optional.empty();
            for (Field field : fields) {
                content = entry.getResolvedFieldOrAlias(field, database);
                if (content.isPresent()) {
                    isName = field.getProperties().contains(FieldProperty.PERSON_NAMES);
                    break;
                }
            }

            String result = content.orElse("");
            if (isName) {
                result = nameFormatter.formatName(result);
                result = LATEXFORMATTER.format(result);
            }
            return result;
        }, entry.getObservables());
        fieldValues.put(fields, value);
        return value;
    }
}
