package org.jabref.gui.mergeentries;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.mergeentries.newmergedialog.ThreeWayMergeView;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class MergeEntriesDialog extends BaseDialog<BibEntry> {
    private final ThreeWayMergeView threeWayMergeView;

    public MergeEntriesDialog(BibEntry one, BibEntry two) {
        threeWayMergeView = new ThreeWayMergeView(one, two);

        init();
    }

    /**
     * Sets up the dialog
     */
    private void init() {
        this.getDialogPane().setContent(threeWayMergeView);

        // Create buttons
        ButtonType replaceEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, replaceEntries);
        this.setResultConverter(buttonType -> {
            if (buttonType.equals(replaceEntries)) {
                return threeWayMergeView.getMergedEntry();
            } else {
                return null;
            }
        });
    }

    public void setLeftHeaderText(String leftHeaderText) {
        // mergeEntries.setLeftHeaderText(leftHeaderText);
    }

    public void setRightHeaderText(String rightHeaderText) {
        // mergeEntries.setRightHeaderText(rightHeaderText);
    }
}
