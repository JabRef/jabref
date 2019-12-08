package org.jabref.gui.collab;

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

    private final MergeEntries mergePanel;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry, BibDatabaseContext database) {
        super();

        this.firstEntry = entry;
        this.secondEntry = newEntry;

        name = entry.getCiteKeyOptional()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));

        mergePanel = new MergeEntries(firstEntry, null, database.getMode());
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().insertEntry(mergePanel.getMergeEntry());
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), mergePanel.getMergeEntry()));
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
        container.getChildren().add(mergePanel);
        return container;
    }
}
