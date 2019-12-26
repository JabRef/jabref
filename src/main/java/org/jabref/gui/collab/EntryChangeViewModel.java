package org.jabref.gui.collab;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.MergeEntries;
import org.jabref.gui.mergeentries.MergeEntries.DefaultRadioButtonSelectionMode;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    private final BibEntry oldEntry;
    private final BibEntry newEntry;
    private MergeEntries mergePanel;

    private final BibDatabaseContext database;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry, BibDatabaseContext database) {
        super();

        this.oldEntry = entry;
        this.newEntry = newEntry;
        this.database = database;

        name = entry.getCiteKeyOptional()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));

    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.getDatabase().removeEntry(oldEntry);
        database.getDatabase().insertEntry(mergePanel.getMergeEntry());
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntry(database.getDatabase(), mergePanel.getMergeEntry()));
    }

    @Override
    public Node description() {

        mergePanel = new MergeEntries(oldEntry, newEntry, Localization.lang("In JabRef"), Localization.lang("On Disk"), database.getMode(), DefaultRadioButtonSelectionMode.RIGHT);

        VBox container = new VBox(10);
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);
        container.getChildren().add(mergePanel);
        container.setMargin(mergePanel, new Insets(5, 5, 5, 5));
        return container;
    }
}
