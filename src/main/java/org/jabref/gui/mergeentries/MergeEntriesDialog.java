package org.jabref.gui.mergeentries;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import org.jabref.gui.mergeentries.newmergedialog.ShowDiffConfig;
import org.jabref.gui.mergeentries.newmergedialog.ThreeWayMergeView;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class MergeEntriesDialog extends BaseDialog<EntriesMergeResult> {
    private final ThreeWayMergeView threeWayMergeView;
    private final BibEntry one;
    private final BibEntry two;

    public MergeEntriesDialog(BibEntry one, BibEntry two, PreferencesService preferencesService) {
        threeWayMergeView = new ThreeWayMergeView(one, two, preferencesService);
        this.one = one;
        this.two = two;

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
                return new EntriesMergeResult(one, two, threeWayMergeView.getLeftEntry(), threeWayMergeView.getRightEntry(), threeWayMergeView.getMergedEntry());
            } else {
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

    public void configureDiff(ShowDiffConfig diffConfig) {
        threeWayMergeView.showDiff(diffConfig);
    }
}
