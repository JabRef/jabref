package org.jabref.gui.collab;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.ThreeWayMergeView;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.newmergedialog.toolbar.ThreeWayMergeToolbar;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private final BibEntry oldEntry;
    private final BibEntry newEntry;
    private ThreeWayMergeView threeWayMergeView;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry) {
        super();

        this.oldEntry = entry;
        this.newEntry = newEntry;

        name = entry.getCitationKey()
                    .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                    .orElse(Localization.lang("Modified entry"));
    }

    /**
     * We override this here to select the radio buttons accordingly
     */
    @Override
    public void setAccepted(boolean accepted) {
        super.setAccepted(accepted);
        if (accepted) {
            threeWayMergeView.selectRightEntryValues();
        } else {
            threeWayMergeView.selectLeftEntryValues();
        }
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        this.description(); // Init dialog to prevent NPE
        database.getDatabase().removeEntry(oldEntry);
        BibEntry mergedEntry = threeWayMergeView.getMergedEntry();
        mergedEntry.setId(oldEntry.getId()); // Keep ID
        database.getDatabase().insertEntry(mergedEntry);
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), mergedEntry));
    }

    @Override
    public Node description() {
        threeWayMergeView = new ThreeWayMergeView(oldEntry, newEntry, Localization.lang("In JabRef"), Localization.lang("On disk"));
        threeWayMergeView.selectLeftEntryValues();
        threeWayMergeView.showDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, DiffHighlighter.DiffMethod.WORDS));
        VBox container = new VBox(10);
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);
        container.getChildren().add(threeWayMergeView);
        VBox.setMargin(threeWayMergeView, new Insets(5, 5, 5, 5));
        return container;
    }
}
