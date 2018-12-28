package org.jabref.gui.mergeentries;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public class MergeEntriesDialog extends BaseDialog<BibEntry> {

    private final MergeEntries mergeEntries;

    public MergeEntriesDialog(BibEntry one, BibEntry two, BibDatabaseMode databaseMode) {
        mergeEntries = new MergeEntries(one, two, databaseMode);

        init();
    }

    /**
     * Sets up the dialog
     *
     */
    private void init() {
        this.getDialogPane().setContent(mergeEntries);

        // Create buttons
        ButtonType replaceEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, replaceEntries);
        this.setResultConverter(buttonType -> {
            if (buttonType.equals(replaceEntries)) {
                return mergeEntries.getMergeEntry();
            } else {
                return null;
            }
        });
    }

    public void setLeftHeaderText(String leftHeaderText) {
        mergeEntries.setLeftHeaderText(leftHeaderText);
    }

    public void setRightHeaderText(String rightHeaderText) {
        mergeEntries.setRightHeaderText(rightHeaderText);
    }
}
