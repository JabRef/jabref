package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    private final List<FieldChangeViewModel> fieldChanges = new ArrayList<>();

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry) {
        super();
        name = entry.getCiteKeyOptional()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));

        Set<Field> allFields = new TreeSet<>(Comparator.comparing(Field::getName));
        allFields.addAll(entry.getFields());
        allFields.addAll(newEntry.getFields());

        for (Field field : allFields) {
            Optional<String> value = entry.getField(field);
            Optional<String> newValue = newEntry.getField(field);

            if (value.isPresent() && newValue.isPresent()) {
                if (!value.equals(newValue)) {
                    // Modified externally.
                    fieldChanges.add(new FieldChangeViewModel(entry, field, value.get(), newValue.get()));
                }
            } else {
                // Added/removed externally.
                fieldChanges.add(new FieldChangeViewModel(entry, field, value.orElse(null), newValue.orElse(null)));
            }
        }
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        for (DatabaseChangeViewModel c : fieldChanges) {
            if (c.isAccepted()) {
                c.makeChange(database, undoEdit);
            }
        }
    }

    @Override
    public Node description() {
        VBox container = new VBox(10);
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        for (FieldChangeViewModel change : fieldChanges) {
            container.getChildren().add(change.description());
        }

        return container;
    }

    static class FieldChangeViewModel extends DatabaseChangeViewModel {

        private final BibEntry entry;
        private final Field field;
        private final String value;
        private final String newValue;

        public FieldChangeViewModel(BibEntry entry, Field field, String value, String newValue) {
            super(field.getName());
            this.entry = entry;
            this.field = field;
            this.value = value;
            this.newValue = newValue;
        }

        @Override
        public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
            Optional<FieldChange> change;
            if (StringUtil.isBlank(newValue)) {
                change = entry.clearField(field);
            } else {
                change = entry.setField(field, newValue);
            }

            change.map(UndoableFieldChange::new).ifPresent(undoEdit::addEdit);
        }

        @Override
        public Node description() {
            VBox container = new VBox();
            container.getChildren().add(new Label(Localization.lang("Modification of field") + " " + field.getDisplayName()));

            if (StringUtil.isNotBlank(newValue)) {
                container.getChildren().add(new Label(Localization.lang("Value set externally") + ": " + newValue));
            } else {
                container.getChildren().add(new Label(Localization.lang("Value cleared externally")));
            }

            if (StringUtil.isNotBlank(value)) {
                container.getChildren().add(new Label(Localization.lang("Current value") + ": " + value));
            } else {
                container.getChildren().add(new Label(Localization.lang("Current value") + ": " + Localization.lang("empty")));
            }

            return container;
        }

    }
}
