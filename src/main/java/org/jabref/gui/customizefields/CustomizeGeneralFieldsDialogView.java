package org.jabref.gui.customizefields;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class CustomizeGeneralFieldsDialogView extends BaseDialog<Void> {

    @FXML private ButtonType resetButton;
    @FXML private ButtonType helpButton;
    @FXML private ButtonType okButton;
    @FXML private TextArea fieldsTextArea;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferences;
    private CustomizeGeneralFieldsDialogViewModel viewModel;

    public CustomizeGeneralFieldsDialogView() {
        this.setTitle(Localization.lang("Set General Fields"));
        this.setResizable(true);
        this.getDialogPane().setPrefSize(300, 650);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(helpButton, getDialogPane(), event -> new HelpAction(HelpFile.GENERAL_FIELDS).execute());
        ControlHelper.setAction(resetButton, getDialogPane(), event -> resetFields());
        ControlHelper.setAction(okButton, getDialogPane(), event -> saveFieldsAndCloseDialog());
    }

    @FXML
    private void initialize() {
        viewModel = new CustomizeGeneralFieldsDialogViewModel(dialogService, preferences);
        fieldsTextArea.textProperty().bindBidirectional(viewModel.fieldsTextProperty());
    }

    @FXML
    private void closeDialog() {
        close();
    }

    @FXML
    private void saveFieldsAndCloseDialog() {
        viewModel.saveFields();
        closeDialog();
    }

    private void resetFields() {
        viewModel.resetFields();
    }
}
