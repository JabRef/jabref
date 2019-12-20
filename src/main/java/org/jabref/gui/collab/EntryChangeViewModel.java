package org.jabref.gui.collab;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    private final BibEntry firstEntry;
    private final BibEntry secondEntry;
    private MergeEntries mergePanel;

    private BibDatabaseContext database;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry, BibDatabaseContext database) {
        super();

        this.firstEntry = entry;
        this.secondEntry = newEntry;
        this.database = database;

        name = entry.getCiteKeyOptional()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));

    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().removeEntry(firstEntry);
        database.getDatabase().insertEntry(mergePanel.getMergeEntry());
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), firstEntry));
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), mergePanel.getMergeEntry()));
    }

    @Override
    public Node description() {

        mergePanel = new MergeEntries(firstEntry, secondEntry, database.getMode());

        VBox container = new VBox(10);
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);
        container.getChildren().add(mergePanel);
        container.setMargin(mergePanel, new Insets(5, 5, 5, 5));
        return container;
    }
}
