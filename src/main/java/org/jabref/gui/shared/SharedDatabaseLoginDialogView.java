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
import org.jabref.logic.shared.DBMSType;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class SharedDatabaseLoginDialogView extends BaseDialog<Void> {

    private final JabRefFrame frame;

    @FXML private ComboBox<DBMSType> databaseType;
    @FXML private TextField host;
    @FXML private TextField database;
    @FXML private TextField port;
    @FXML private TextField user;
    @FXML private PasswordField password;
    @FXML private CheckBox rememberPassword;
    @FXML private TextField folder;
    @FXML private Button browseButton;
    @FXML private CheckBox autosave;
    @FXML private ButtonType connectButton;
    @FXML private CheckBox useSSL;
    @FXML private TextField fileKeystore;
    @FXML private PasswordField passwordKeystore;
    @FXML private Button browseKeystore;
    @FXML private TextField serverTimezone;

    @Inject private DialogService dialogService;
    @Inject private PreferencesService preferencesService;

    private SharedDatabaseLoginDialogViewModel viewModel;
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public SharedDatabaseLoginDialogView(JabRefFrame frame) {
        this.frame = frame;
        this.setTitle(Localization.lang("Connect to shared database"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(connectButton, this.getDialogPane(), event -> openDatabase());
        Button btnConnect = (Button) this.getDialogPane().lookupButton(connectButton);
        // must be set here, because in initialize the button is still null
        btnConnect.disableProperty().bind(viewModel.formValidation().validProperty().not());
        btnConnect.textProperty().bind(EasyBind.map(viewModel.loadingProperty(), loading -> (loading) ? Localization.lang("Connecting...") : Localization.lang("Connect")));
    }

    @FXML
    private void openDatabase() {
        boolean connected = viewModel.openDatabase();

        if (connected) {
            this.close();
        }
    }

    @FXML
    private void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        viewModel = new SharedDatabaseLoginDialogViewModel(frame, dialogService, preferencesService);
        databaseType.getItems().addAll(DBMSType.values());
        databaseType.getSelectionModel().select(0);

        database.textProperty().bindBidirectional(viewModel.databaseproperty());
        host.textProperty().bindBidirectional(viewModel.hostProperty());
        user.textProperty().bindBidirectional(viewModel.userProperty());
        password.textProperty().bindBidirectional(viewModel.passwordProperty());
        port.textProperty().bindBidirectional(viewModel.portProperty());
        serverTimezone.textProperty().bindBidirectional(viewModel.serverTimezoneProperty());
        databaseType.valueProperty().bindBidirectional(viewModel.selectedDbmstypeProperty());

        folder.textProperty().bindBidirectional(viewModel.folderProperty());
        browseButton.disableProperty().bind(viewModel.autosaveProperty().not());
        folder.disableProperty().bind(viewModel.autosaveProperty().not());
        autosave.selectedProperty().bindBidirectional(viewModel.autosaveProperty());

        useSSL.selectedProperty().bindBidirectional(viewModel.useSSLProperty());

        fileKeystore.textProperty().bindBidirectional(viewModel.keyStoreProperty());

        browseKeystore.disableProperty().bind(viewModel.useSSLProperty().not());
        passwordKeystore.disableProperty().bind(viewModel.useSSLProperty().not());
        passwordKeystore.textProperty().bindBidirectional(viewModel.keyStorePasswordProperty());
        rememberPassword.selectedProperty().bindBidirectional(viewModel.rememberPasswordProperty());

        // Must be executed after the initialization of the view, otherwise it doesn't work
        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.dbValidation(), database, true);
            visualizer.initVisualization(viewModel.hostValidation(), host, true);
            visualizer.initVisualization(viewModel.portValidation(), port, true);
            visualizer.initVisualization(viewModel.userValidation(), user, true);

            EasyBind.subscribe(autosave.selectedProperty(), selected ->
                    visualizer.initVisualization(viewModel.folderValidation(), folder, true));

            EasyBind.subscribe(useSSL.selectedProperty(), selected ->
                    visualizer.initVisualization(viewModel.keystoreValidation(), fileKeystore, true));
        });
    }

    @FXML
    private void showSaveDbToFileDialog(ActionEvent event) {
        viewModel.showSaveDbToFileDialog();
    }

    @FXML
    private void showOpenKeystoreFileDialog(ActionEvent event) {
        viewModel.showOpenKeystoreFileDialog();
    }
}
