package org.jabref.gui.genfields;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class GenFieldsCustomizerDialogView extends BaseDialog<Void> {

    @FXML private ButtonType resetButton;
    @FXML private ButtonType helpButton;
    @FXML private ButtonType okButton; // Double checkthis
    @FXML private ButtonType cancelButton; //Double check this
    @FXML private TextArea fieldsTextArea; //generic?

    //dependency injections here - checing github, probably dialogService

    //private GenFieldsDialogViewModel viewModel;

    public void GenFieldsCustomizerView() {
        this.setTitle(Localization.lang("Set General Fields"));
        this.setResizable(true);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        //Yo ushould test at this point
    }

}