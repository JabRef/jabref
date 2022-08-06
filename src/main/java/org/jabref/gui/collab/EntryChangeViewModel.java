package org.jabref.gui.collab;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.MergeEntriesDialog;
import org.jabref.gui.mergeentries.MergeTwoEntriesAction;
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

    private final DialogService dialogService;

    public EntryChangeViewModel(BibEntry entry, BibEntry newEntry, DialogService dialogService) {
        super(entry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                   .orElse(Localization.lang("Modified entry")));

        this.oldEntry = entry;
        this.newEntry = newEntry;
        this.dialogService = dialogService;
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
        database.getDatabase().removeEntry(oldEntry);
        database.getDatabase().insertEntry(newEntry);
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntries(database.getDatabase(), newEntry));
    }

    @Override
    public Node description() {
        threeWayMergeView = new ThreeWayMergeView(oldEntry, newEntry, Localization.lang("In JabRef"), Localization.lang("On disk"));
        threeWayMergeView.selectLeftEntryValues();
        threeWayMergeView.showDiff(new ShowDiffConfig(ThreeWayMergeToolbar.DiffView.SPLIT, DiffHighlighter.DiffMethod.WORDS));
        VBox container = new VBox(10);
        Label header = new Label(getName());
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);
        container.getChildren().add(threeWayMergeView);
        VBox.setMargin(threeWayMergeView, new Insets(5, 5, 5, 5));
        return container;
    }

    @Override
    public boolean hasAdvancedMergeDialog() {
        return true;
    }

    @Override
    public Optional<SimpleCommand> openAdvancedMergeDialog() {
        MergeEntriesDialog mergeEntriesDialog = new MergeEntriesDialog(oldEntry, newEntry);
        return dialogService.showCustomDialogAndWait(mergeEntriesDialog)
                            .map(res -> new MergeTwoEntriesAction(res, null, null));
    }
}
