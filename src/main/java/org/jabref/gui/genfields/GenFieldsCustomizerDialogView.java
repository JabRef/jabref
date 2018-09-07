package org.jabref.gui.genfields;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class GenFieldsCustomizerDialogView extends BaseDialog<Void> {

    @FXML private ButtonType resetButton;
    @FXML private ButtonType helpButton;
    @FXML private ButtonType okButton; // Double checkthis
    @FXML private ButtonType cancelButton; //Double check this
    @FXML private TextArea fieldsTextArea; //generic?

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;
    private GenFieldsCustomizerDialogViewModel viewModel;

    public GenFieldsCustomizerDialogView() {
        this.setTitle(Localization.lang("Set General Fields"));
        this.setResizable(true);
        this.getDialogPane().setPrefSize(300, 650);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new GenFieldsCustomizerDialogViewModel(dialogService, preferences);

        viewModel.initialFieldTextProperty().bind(//see Line 32 in the sample model
                          //stuff here using EasyBind  )

    }

    @FXML
    private void closeDialog() {
        close();
    }

}