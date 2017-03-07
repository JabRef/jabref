package org.jabref.gui.journals;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.AbstractDialogView;
import org.jabref.gui.FXDialog;
import org.jabref.logic.l10n.Localization;

/**
 * This class controls the user interface of the journal abbreviations dialog.
 * The ui elements and their layout are defined in the fxml file in the resource folder.
 */
public class ManageJournalAbbreviationsView extends AbstractDialogView {

    @Override
    public void show() {
        FXDialog journalAbbreviationsDialog = new FXDialog(AlertType.INFORMATION, Localization.lang("Journal abbreviations"));
        journalAbbreviationsDialog.setResizable(true);
        journalAbbreviationsDialog.setDialogPane((DialogPane) this.getView());
        journalAbbreviationsDialog.show();
    }
}
