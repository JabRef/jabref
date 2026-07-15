package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.logic.bibtex.comparator.ComparisonResult;
import org.jabref.logic.bibtex.comparator.plausibility.PlausibilityComparatorFactory;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.ISSN;

public class MultiMergeEntriesViewModel extends AbstractViewModel {

    private final ListProperty<EntrySource> entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<BibEntry> mergedEntry = new SimpleObjectProperty<>(new BibEntry());

    private final ListProperty<String> failedSuppliers = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final Map<Field, Set<String>> autoFetchedIdentifiers = new HashMap<>();

    public void addSource(EntrySource entrySource) {
        if (!entrySource.isLoading.getValue()) {
            updateFields(entrySource.entry.get());
        } else {
            entrySource.isLoading.addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    updateFields(entrySource.entry.get());
                    if (entrySource.entryProperty().get() == null) {
                        failedSuppliers.add(entrySource.titleProperty().get());
                    }
                }
            });
        }
        entries.add(entrySource);
    }

    public void updateFields(BibEntry entry) {
        if (entry == null) {
            return;
        }
        for (Map.Entry<Field, String> fieldEntry : entry.getFieldMap().entrySet()) {
            Field field = fieldEntry.getKey();
            String newValue = fieldEntry.getValue();

            if (!mergedEntry.get().getFieldsObservable().containsKey(field)) {
                mergedEntry.get().setField(field, newValue);
            } else {
                String currentValue = mergedEntry.get().getField(field).orElse("");
                PlausibilityComparatorFactory.INSTANCE
                        .getPlausibilityComparator(field)
                        .map(comparator -> comparator.compare(newValue, currentValue))
                        .ifPresent(result -> {
                            if (result == ComparisonResult.LEFT_BETTER) {
                                mergedEntry.get().setField(field, newValue);
                            }
                        });
            }
        }
    }

    public Map<Field, String> findNewFetchableIdentifiers(BibEntry entry) {
        Map<Field, String> result = new LinkedHashMap<>();
        for (Field field : FetchAndMergeEntry.SUPPORTED_FIELDS) {
            entry.getField(field)
                 .flatMap(rawValue -> normalizeIdentifier(field, rawValue))
                 .filter(value -> autoFetchedIdentifiers.computeIfAbsent(field, _ -> new HashSet<>())
                                                        .add(value.toLowerCase(Locale.ROOT)))
                 .ifPresent(value -> result.put(field, value));
        }
        return result;
    }

    private static Optional<String> normalizeIdentifier(Field field, String rawValue) {
        return switch (field) {
            case StandardField.DOI ->
                    DOI.parse(rawValue).map(DOI::asString);
            case StandardField.ISBN ->
                    ISBN.parse(rawValue).map(ISBN::asString);
            case StandardField.EPRINT ->
                    ArXivIdentifier.parse(rawValue).map(ArXivIdentifier::asString);
            case StandardField.ISSN ->
                    normalizeIssn(rawValue);
            default ->
                    Optional.empty();
        };
    }

    private static Optional<String> normalizeIssn(String rawValue) {
        ISSN candidate = new ISSN(rawValue);
        if (candidate.isCanBeCleaned()) {
            candidate = new ISSN(candidate.getCleanedISSN());
        }
        if (candidate.isValidFormat() && candidate.isValidChecksum()) {
            return Optional.of(candidate.asString());
        }
        return Optional.empty();
    }

    public BibEntry resultConverter(ButtonType button) {
        if (button == ButtonType.CANCEL) {
            return null;
        }
        return mergedEntry.get();
    }

    public ListProperty<EntrySource> entriesProperty() {
        return entries;
    }

    public ObjectProperty<BibEntry> mergedEntryProperty() {
        return mergedEntry;
    }

    public ListProperty<String> failedSuppliersProperty() {
        return failedSuppliers;
    }

    public static class EntrySource {
        private final StringProperty title = new SimpleStringProperty("");
        private final ObjectProperty<BibEntry> entry = new SimpleObjectProperty<>();
        private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

        public EntrySource(String title, Supplier<BibEntry> entrySupplier, TaskExecutor taskExecutor) {
            this.title.set(title);
            isLoading.set(true);

            BackgroundTask.wrap(entrySupplier::get)
                          .onSuccess(value -> {
                              entry.set(value);
                              isLoading.set(false);
                          })
                          .executeWith(taskExecutor);
        }

        public EntrySource(String title, BibEntry entry) {
            this.title.set(title);
            this.entry.set(entry);
        }

        public StringProperty titleProperty() {
            return title;
        }

        public ObjectProperty<BibEntry> entryProperty() {
            return entry;
        }

        public BooleanProperty isLoadingProperty() {
            return isLoading;
        }
    }
}
