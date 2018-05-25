package org.jabref.gui.shared;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.JabRefException;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.model.database.shared.DBMSType;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabaseLoginDialogView.class);

    @FXML private ComboBox<DBMSType> cmbDbType;
    @FXML private TextField tbHost;
    @FXML private TextField tbDb;
    @FXML private TextField tbPort;
    @FXML private TextField tbUser;
    @FXML private PasswordField tbPwd;
    @FXML private CheckBox chkRememberPassword;
    @FXML private TextField tbFolder;
    @FXML private Button btnBrowse;
    @FXML private CheckBox chkAutosave;
    @FXML private ButtonType connectButton;

    @Inject private DialogService dialogService;

    private final SharedDatabasePreferences prefs = new SharedDatabasePreferences();
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    private final JabRefFrame frame;

    private DBMSConnectionProperties connectionProperties;
    private SharedDatabaseLoginDialogViewModel viewModel;
    public SharedDatabaseLoginDialogView(JabRefFrame frame) {
        this.frame = frame;
        this.setTitle(Localization.lang("Connect to shared database"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(connectButton, this.getDialogPane(), event -> openDatabase());

    }

    @FXML
    private void openDatabase() {

        try {
            checkFields();
        } catch (JabRefException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        viewModel.openDatabase();

    }

    @FXML
    private void initialize() {

        viewModel = new SharedDatabaseLoginDialogViewModel(frame, dialogService);

        visualizer.setDecoration(new IconValidationDecorator());

        cmbDbType.itemsProperty().bind(viewModel.dbmstypeProperty());
        cmbDbType.getSelectionModel().select(0);
        tbHost.textProperty().bindBidirectional(viewModel.hostProperty());
        tbDb.textProperty().bindBidirectional(viewModel.dbProperty());
        tbUser.textProperty().bindBidirectional(viewModel.userProperty());
        tbPwd.textProperty().bindBidirectional(viewModel.passwordProperty());
        tbPort.textProperty().bindBidirectional(viewModel.portProperty());
        cmbDbType.valueProperty().bindBidirectional(viewModel.selectedDbmstypeProperty());

        tbFolder.textProperty().bindBidirectional(viewModel.folderProperty());
        btnBrowse.disableProperty().bind(chkAutosave.selectedProperty().not());
        tbFolder.disableProperty().bind(chkAutosave.selectedProperty().not());
        chkAutosave.selectedProperty().bindBidirectional(viewModel.autosaveProperty());

        viewModel.applyPreferences();
    }

    @FXML
    void openFileDialog(ActionEvent event) {
        viewModel.openFileDialog();
    }



    private void setLoadingConnectButtonText(boolean isLoading) {

        if (isLoading) {
            //    connectButton.setText(Localization.lang("Connecting..."));
        } else {
            //  connectButton.setText(Localization.lang("Connect"));
        }
    }


    private void checkFields() throws JabRefException {
        if (isEmptyField(tbHost)) {
            tbHost.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Host")));
        }
        if (isEmptyField(tbPort)) {
            tbPort.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Port")));
        }
        if (isEmptyField(tbDb)) {
            tbDb.requestFocus();
            throw new JabRefException(
                                      Localization.lang("Required field \"%0\" is empty.", Localization.lang("Library")));
        }
        if (isEmptyField(tbUser)) {
            tbUser.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("User")));
        }
        if (chkAutosave.isSelected() && isEmptyField(tbFolder)) {
            tbFolder.requestFocus();
            throw new JabRefException(Localization.lang("Please enter a valid file path."));
        }
    }

    private boolean isEmptyField(TextField field) {
        return field.getText().trim().length() == 0;
    }



}
