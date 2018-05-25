package org.jabref.gui.shared;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.shared.DBMSType;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.fxmisc.easybind.EasyBind;

public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {

    private final JabRefFrame frame;

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

    private final Button btnConnect;

    private SharedDatabaseLoginDialogViewModel viewModel;
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public SharedDatabaseLoginDialogView(JabRefFrame frame) {
        this.frame = frame;
        this.setTitle(Localization.lang("Connect to shared database"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(connectButton, this.getDialogPane(), event -> openDatabase());
        btnConnect = (Button) this.getDialogPane().lookupButton(connectButton);
        setLoadingConnectButtonText();
        btnConnect.disableProperty().bind(viewModel.formValidation().validProperty().not());

    }

    @FXML
    private void openDatabase() {
        viewModel.openDatabase();
    }

    @FXML
    private void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        viewModel = new SharedDatabaseLoginDialogViewModel(frame, dialogService);
        cmbDbType.itemsProperty().bind(viewModel.dbmstypeProperty());
        cmbDbType.getSelectionModel().select(0);

        tbDb.textProperty().bindBidirectional(viewModel.dbProperty());
        tbHost.textProperty().bindBidirectional(viewModel.hostProperty());
        tbUser.textProperty().bindBidirectional(viewModel.userProperty());
        tbPwd.textProperty().bindBidirectional(viewModel.passwordProperty());
        tbPort.textProperty().bindBidirectional(viewModel.portProperty());
        cmbDbType.valueProperty().bindBidirectional(viewModel.selectedDbmstypeProperty());

        tbFolder.textProperty().bindBidirectional(viewModel.folderProperty());
        btnBrowse.disableProperty().bind(chkAutosave.selectedProperty().not());
        tbFolder.disableProperty().bind(chkAutosave.selectedProperty().not());
        chkAutosave.selectedProperty().bindBidirectional(viewModel.autosaveProperty());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.dbValidation(), tbDb, true);
            visualizer.initVisualization(viewModel.hostValidation(), tbHost, true);
            visualizer.initVisualization(viewModel.portValidation(), tbPort, true);
            visualizer.initVisualization(viewModel.userValidation(), tbUser, true);

            EasyBind.subscribe(chkAutosave.selectedProperty(), selected -> {
                visualizer.initVisualization(viewModel.folderValidation(), tbFolder, true);
            });
        });

        viewModel.applyPreferences();

    }

    @FXML
    void openFileDialog(ActionEvent event) {
        viewModel.openFileDialog();
    }

    private void setLoadingConnectButtonText() {

        if (viewModel.loadingProperty().get()) {
            btnConnect.setText(Localization.lang("Connecting..."));
            btnConnect.setDisable(true);
        } else {
            btnConnect.setText(Localization.lang("Connect"));
        }
    }

}
