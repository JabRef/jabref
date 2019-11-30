package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    private final BibEntry firstEntry;

    private final BibEntry secondEntry;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry) {
        super();

        this.firstEntry = entry;
        this.secondEntry = newEntry;

        name = entry.getCiteKeyOptional()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));

        /*name = entry.getCiteKeyOptional()
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
        }*/
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        /* for (DatabaseChangeViewModel c : fieldChanges) {
            if (c.isAccepted()) {
                c.makeChange(database, undoEdit);
            }
        }*/
    }

    public BibEntry getFirst() {
        return this.firstEntry;
    }

    public BibEntry getSecond() {
        return this.secondEntry;
    }

    @Override
    public Node description() {
        VBox container = new VBox(10);
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);
        return container;
    }

}
