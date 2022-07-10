package org.jabref.gui.mergeentries;

import javax.swing.undo.CompoundEdit;

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
        this.setX(20);
        this.setY(20);

        this.getDialogPane().setContent(threeWayMergeView);

        // Create buttons
        ButtonType replaceEntries = new ButtonType(Localization.lang("Merge entries"), ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, replaceEntries);
        this.setResultConverter(buttonType -> {
            if (buttonType.equals(replaceEntries)) {
                return threeWayMergeView.getMergedEntry();
            } else {
                threeWayMergeView.cancelGroupsMerge();
                return null;
            }
        });
    }

    public void setLeftHeaderText(String leftHeaderText) {
        threeWayMergeView.setLeftHeader(leftHeaderText);
    }

    public void setRightHeaderText(String rightHeaderText) {
        threeWayMergeView.setRightHeader(rightHeaderText);
    }

    public CompoundEdit getMergeGroupsEdit() {
        return threeWayMergeView.getMergeGroupsEdit();
    }
}
