package net.sf.jabref.gui.journals;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import net.sf.jabref.gui.FXDialog;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

/**
 * This class controls the user interface of the journal abbreviations dialog.
 * The ui elements and their layout are defined in the fxml file in the resource folder.
 */
public class ManageJournalAbbreviationsView extends FXMLView {

    public ManageJournalAbbreviationsView() {
        super();
        bundle = Localization.getMessages();
    }

    public void showAndWait() {
        FXDialog journalAbbreviationsDialog = new FXDialog(AlertType.INFORMATION,
                Localization.lang("Journal abbreviations"));
        journalAbbreviationsDialog.setResizable(true);
        journalAbbreviationsDialog.setDialogPane((DialogPane) this.getView());
        ((Stage) this.getView().getScene().getWindow()).setMinHeight(400);
        ((Stage) this.getView().getScene().getWindow()).setMinWidth(600);
        journalAbbreviationsDialog.showAndWait();
    }

}
