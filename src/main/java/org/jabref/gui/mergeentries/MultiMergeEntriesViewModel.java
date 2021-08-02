package org.jabref.gui.mergeentries;

import java.util.Map;
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
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class MultiMergeEntriesViewModel extends AbstractViewModel {

    private final ListProperty<Entry> entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<BibEntry> mergedEntry = new SimpleObjectProperty<>(new BibEntry());

    public void addSource(Entry entryColumn) {
        if (!entryColumn.isLoading.getValue()) {
            updateFields(entryColumn.entry.get());
        } else {
            entryColumn.isLoading.addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    updateFields(entryColumn.entry.get());
                }
            });
        }
        entries.add(entryColumn);
    }

    public void updateFields(BibEntry entry) {
        for (Map.Entry<Field, String> fieldEntry : entry.getFieldMap().entrySet()) {
            // make sure there is a row for the field
            if (!mergedEntry.get().getFieldsObservable().containsKey(fieldEntry.getKey())) {
                mergedEntry.get().setField(fieldEntry.getKey(), fieldEntry.getValue());
            }
        }
    }

    public BibEntry resultConverter(ButtonType button) {
        if (button == ButtonType.CANCEL) {
            return null;
        }
        return mergedEntry.get();
    }

    public ListProperty<Entry> entriesProperty() {
        return entries;
    }

    public ObjectProperty<BibEntry> mergedEntryProperty() {
        return mergedEntry;
    }

    public static class Entry {
        private final StringProperty title = new SimpleStringProperty("");
        private final ObjectProperty<BibEntry> entry = new SimpleObjectProperty<>();
        private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

        public Entry(String title, Supplier<BibEntry> entrySupplier, TaskExecutor taskExecutor) {
            this.title.set(title);
            isLoading.set(true);

            BackgroundTask.wrap(entrySupplier::get)
                          .onSuccess(value -> {
                              entry.set(value);
                              isLoading.set(false);
                          })
                          .executeWith(taskExecutor);
        }

        public Entry(String title, BibEntry entry) {
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
