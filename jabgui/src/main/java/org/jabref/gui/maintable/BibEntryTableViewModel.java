package org.jabref.gui.maintable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.search.MatchCategory;
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
import org.jspecify.annotations.Nullable;

public class BibEntryTableViewModel {
    private final BibEntry entry;
    private final ObservableValue<MainTableFieldValueFormatter> fieldValueFormatter;
    @Nullable private Map<OrFields, ObservableValue<String>> fieldValues;
    @Nullable private Map<SpecialField, OptionalBinding<SpecialFieldValueViewModel>> specialFieldValues;
    private final BibDatabaseContext bibDatabaseContext;
    private boolean hasFullTextResultsValue;
    private boolean isMatchedBySearchValue = true;
    private boolean isVisibleBySearchValue = true;
    private boolean isMatchedByGroupValue = true;
    private boolean isVisibleByGroupValue = true;
    @Nullable private BooleanProperty hasFullTextResults;
    @Nullable private BooleanProperty isMatchedBySearch;
    @Nullable private BooleanProperty isVisibleBySearch;
    @Nullable private BooleanProperty isMatchedByGroup;
    @Nullable private BooleanProperty isVisibleByGroup;
    private final ObjectProperty<MatchCategory> matchCategory = new SimpleObjectProperty<>(MatchCategory.MATCHING_SEARCH_AND_GROUPS);
    private EasyBinding<List<LinkedFile>> linkedFiles;
    private EasyBinding<Map<Field, String>> linkedIdentifiers;
    private Binding<List<AbstractGroup>> matchedGroups;
    private Observable[] fieldValueDependencies;

    public BibEntryTableViewModel(BibEntry entry, BibDatabaseContext bibDatabaseContext, ObservableValue<MainTableFieldValueFormatter> fieldValueFormatter) {
        this.entry = entry;
        this.bibDatabaseContext = bibDatabaseContext;
        this.fieldValueFormatter = fieldValueFormatter;
    }

    private static EasyBinding<Map<Field, String>> createLinkedIdentifiersBinding(BibEntry entry) {
        return EasyBind.combine(
                entry.getFieldBinding(StandardField.URL),
                entry.getFieldBinding(StandardField.DOI),
                entry.getFieldBinding(StandardField.URI),
                entry.getFieldBinding(StandardField.EPRINT),
                entry.getFieldBinding(StandardField.ISBN),
                (url, doi, uri, eprint, isbn) -> {
                    Map<Field, String> identifiers = new HashMap<>();
                    url.ifPresent(value -> identifiers.put(StandardField.URL, value));
                    doi.ifPresent(value -> identifiers.put(StandardField.DOI, value));
                    uri.ifPresent(value -> identifiers.put(StandardField.URI, value));
                    eprint.ifPresent(value -> identifiers.put(StandardField.EPRINT, value));
                    isbn.ifPresent(value -> identifiers.put(StandardField.ISBN, value));
                    return identifiers;
                });
    }

    public BibEntry getEntry() {
        return entry;
    }

    private static Binding<List<AbstractGroup>> createMatchedGroupsBinding(BibDatabaseContext database, BibEntry entry) {
        return new UiThreadBinding<>(EasyBind.combine(entry.getFieldBinding(StandardField.GROUPS), database.getMetaData().groupsBinding(),
                (_, _) ->
                        database.getMetaData().getGroups().map(groupTreeNode ->
                                        groupTreeNode.getMatchingGroups(entry).stream()
                                                     .map(GroupTreeNode::getGroup)
                                                     .filter(Predicate.not(Predicate.isEqual(groupTreeNode.getGroup())))
                                                     .collect(Collectors.toList()))
                                .orElse(List.of())));
    }

    public OptionalBinding<String> getField(Field field) {
        return entry.getFieldBinding(field);
    }

    public ObservableValue<List<LinkedFile>> getLinkedFiles() {
        if (linkedFiles == null) {
            linkedFiles = getField(StandardField.FILE).mapOpt(FileFieldParser::parse).orElseOpt(List.of());
        }
        return linkedFiles;
    }

    public ObservableValue<Map<Field, String>> getLinkedIdentifiers() {
        if (linkedIdentifiers == null) {
            linkedIdentifiers = createLinkedIdentifiersBinding(entry);
        }
        return linkedIdentifiers;
    }

    public ObservableValue<List<AbstractGroup>> getMatchedGroups() {
        if (matchedGroups == null) {
            matchedGroups = createMatchedGroupsBinding(bibDatabaseContext, entry);
        }
        return matchedGroups;
    }

    public ObservableValue<Optional<SpecialFieldValueViewModel>> getSpecialField(SpecialField field) {
        OptionalBinding<SpecialFieldValueViewModel> value = getSpecialFieldValues().get(field);
        // Fetch possibly updated value from BibEntry entry
        Optional<String> currentValue = this.entry.getField(field);
        if (value != null) {
            if (currentValue.isEmpty() && value.getValue().isEmpty()) {
                OptionalBinding<SpecialFieldValueViewModel> zeroValue = getField(field).flatMapOpt(_ -> field.parseValue("CLEAR_RANK").map(SpecialFieldValueViewModel::new));
                getSpecialFieldValues().put(field, zeroValue);
                return zeroValue;
            } else if (value.getValue().isEmpty() || !value.getValue().get().getValue().getFieldValue().equals(currentValue)) {
                // specialFieldValues value and BibEntry value differ => Set specialFieldValues value to BibEntry value
                value = getField(field).flatMapOpt(fieldValue -> field.parseValue(fieldValue).map(SpecialFieldValueViewModel::new));
                getSpecialFieldValues().put(field, value);
                return value;
            }
        } else {
            value = getField(field).flatMapOpt(fieldValue -> field.parseValue(fieldValue).map(SpecialFieldValueViewModel::new));
            getSpecialFieldValues().put(field, value);
        }
        return value;
    }

    public ObservableValue<String> getFields(OrFields fields) {
        ObservableValue<String> value = getFieldValues().get(fields);
        if (value != null) {
            return value;
        }

        value = Bindings.createStringBinding(() ->
                        fieldValueFormatter.getValue().formatFieldsValues(fields, entry),
                getFieldValueDependencies());
        getFieldValues().put(fields, value);
        return value;
    }

    private Map<OrFields, ObservableValue<String>> getFieldValues() {
        if (fieldValues == null) {
            fieldValues = new HashMap<>();
        }
        return fieldValues;
    }

    private Map<SpecialField, OptionalBinding<SpecialFieldValueViewModel>> getSpecialFieldValues() {
        if (specialFieldValues == null) {
            specialFieldValues = new HashMap<>();
        }
        return specialFieldValues;
    }

    /// Cache the dependency array so each field binding can reuse it instead of rebuilding the same observable list.
    private Observable[] getFieldValueDependencies() {
        if (fieldValueDependencies == null) {
            Observable[] entryObservables = entry.getObservables();
            fieldValueDependencies = Arrays.copyOf(entryObservables, entryObservables.length + 1);
            fieldValueDependencies[entryObservables.length] = fieldValueFormatter;
        }
        return fieldValueDependencies;
    }

    public StringProperty bibDatabasePathProperty() {
        return new ReadOnlyStringWrapper(bibDatabaseContext.getDatabasePath().map(Path::toString).orElse(""));
    }

    public BibDatabaseContext getBibDatabaseContext() {
        return bibDatabaseContext;
    }

    public BooleanProperty hasFullTextResultsProperty() {
        if (hasFullTextResults == null) {
            hasFullTextResults = new SimpleBooleanProperty(hasFullTextResultsValue);
        }
        return hasFullTextResults;
    }

    public BooleanProperty isMatchedBySearch() {
        if (isMatchedBySearch == null) {
            isMatchedBySearch = new SimpleBooleanProperty(isMatchedBySearchValue);
        }
        return isMatchedBySearch;
    }

    public BooleanProperty isVisibleBySearch() {
        if (isVisibleBySearch == null) {
            isVisibleBySearch = new SimpleBooleanProperty(isVisibleBySearchValue);
        }
        return isVisibleBySearch;
    }

    public BooleanProperty isMatchedByGroup() {
        if (isMatchedByGroup == null) {
            isMatchedByGroup = new SimpleBooleanProperty(isMatchedByGroupValue);
        }
        return isMatchedByGroup;
    }

    public BooleanProperty isVisibleByGroup() {
        if (isVisibleByGroup == null) {
            isVisibleByGroup = new SimpleBooleanProperty(isVisibleByGroupValue);
        }
        return isVisibleByGroup;
    }

    public void setHasFullTextResults(boolean hasFullTextResults) {
        if (this.hasFullTextResults == null) {
            hasFullTextResultsValue = hasFullTextResults;
        } else {
            this.hasFullTextResults.set(hasFullTextResults);
        }
    }

    public void setMatchedBySearch(boolean isMatchedBySearch) {
        if (this.isMatchedBySearch == null) {
            isMatchedBySearchValue = isMatchedBySearch;
        } else {
            this.isMatchedBySearch.set(isMatchedBySearch);
        }
    }

    public void setVisibleBySearch(boolean isVisibleBySearch) {
        if (this.isVisibleBySearch == null) {
            isVisibleBySearchValue = isVisibleBySearch;
        } else {
            this.isVisibleBySearch.set(isVisibleBySearch);
        }
    }

    public void setMatchedByGroup(boolean isMatchedByGroup) {
        if (this.isMatchedByGroup == null) {
            isMatchedByGroupValue = isMatchedByGroup;
        } else {
            this.isMatchedByGroup.set(isMatchedByGroup);
        }
    }

    public void setVisibleByGroup(boolean isVisibleByGroup) {
        if (this.isVisibleByGroup == null) {
            isVisibleByGroupValue = isVisibleByGroup;
        } else {
            this.isVisibleByGroup.set(isVisibleByGroup);
        }
    }

    public ObjectProperty<MatchCategory> matchCategory() {
        return matchCategory;
    }

    public boolean isVisible() {
        return getVisibleBySearch() && getVisibleByGroup();
    }

    public void updateMatchCategory() {
        MatchCategory category = MatchCategory.NOT_MATCHING_SEARCH_AND_GROUPS;

        if (getMatchedBySearch() && getMatchedByGroup()) {
            category = MatchCategory.MATCHING_SEARCH_AND_GROUPS;
        } else if (getMatchedBySearch()) {
            category = MatchCategory.MATCHING_SEARCH_NOT_GROUPS;
        } else if (getMatchedByGroup()) {
            category = MatchCategory.MATCHING_GROUPS_NOT_SEARCH;
        }

        matchCategory.set(category);
    }

    boolean getMatchedBySearch() {
        return isMatchedBySearch == null ? isMatchedBySearchValue : isMatchedBySearch.get();
    }

    private boolean getVisibleBySearch() {
        return isVisibleBySearch == null ? isVisibleBySearchValue : isVisibleBySearch.get();
    }

    private boolean getMatchedByGroup() {
        return isMatchedByGroup == null ? isMatchedByGroupValue : isMatchedByGroup.get();
    }

    private boolean getVisibleByGroup() {
        return isVisibleByGroup == null ? isVisibleByGroupValue : isVisibleByGroup.get();
    }
}
