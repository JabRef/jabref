package org.jabref.gui.preferences.network;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomPasswordField;

public class NetworkTab extends AbstractPreferenceTabView<NetworkTabViewModel> implements PreferencesTab {
    @FXML private Label remoteLabel;
    @FXML private CheckBox remoteServer;
    @FXML private TextField remotePort;
    @FXML private Button remoteHelp;

    @FXML private CheckBox proxyUse;
    @FXML private Label proxyHostnameLabel;
    @FXML private TextField proxyHostname;
    @FXML private Label proxyPortLabel;
    @FXML private TextField proxyPort;
    @FXML private CheckBox proxyUseAuthentication;
    @FXML private Label proxyUsernameLabel;
    @FXML private TextField proxyUsername;
    @FXML private Label proxyPasswordLabel;
    @FXML private CustomPasswordField proxyPassword;
    @FXML private Label proxyAttentionLabel;
    @FXML private Button checkConnectionButton;

    private String proxyPasswordText = "";
    private int proxyPasswordCaretPosition = 0;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public NetworkTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Network");
    }

    public void initialize() {
        this.viewModel = new NetworkTabViewModel(dialogService, preferencesService);

        remoteLabel.setVisible(preferencesService.getGeneralPreferences().shouldShowAdvancedHints());
        remoteServer.selectedProperty().bindBidirectional(viewModel.remoteServerProperty());
        remotePort.textProperty().bindBidirectional(viewModel.remotePortProperty());
        remotePort.disableProperty().bind(remoteServer.selectedProperty().not());

        proxyUse.selectedProperty().bindBidirectional(viewModel.proxyUseProperty());
        proxyHostnameLabel.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyHostname.textProperty().bindBidirectional(viewModel.proxyHostnameProperty());
        proxyHostname.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyPortLabel.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyPort.textProperty().bindBidirectional(viewModel.proxyPortProperty());
        proxyPort.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyUseAuthentication.selectedProperty().bindBidirectional(viewModel.proxyUseAuthenticationProperty());
        proxyUseAuthentication.disableProperty().bind(proxyUse.selectedProperty().not());

        BooleanBinding proxyCustomAndAuthentication = proxyUse.selectedProperty().and(proxyUseAuthentication.selectedProperty());
        proxyUsernameLabel.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyUsername.textProperty().bindBidirectional(viewModel.proxyUsernameProperty());
        proxyUsername.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyPasswordLabel.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyPassword.textProperty().bindBidirectional(viewModel.proxyPasswordProperty());
        proxyPassword.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyAttentionLabel.disableProperty().bind(proxyCustomAndAuthentication.not());

        proxyPassword.setRight(IconTheme.JabRefIcons.PASSWORD_REVEALED.getGraphicNode());
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_PRESSED, this::proxyPasswordReveal);
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_RELEASED, this::proxyPasswordMask);
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_EXITED, this::proxyPasswordMask);

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.REMOTE), remoteHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.remotePortValidationStatus(), remotePort);
            validationVisualizer.initVisualization(viewModel.proxyHostnameValidationStatus(), proxyHostname);
            validationVisualizer.initVisualization(viewModel.proxyPortValidationStatus(), proxyPort);
            validationVisualizer.initVisualization(viewModel.proxyUsernameValidationStatus(), proxyUsername);
            validationVisualizer.initVisualization(viewModel.proxyPasswordValidationStatus(), proxyPassword);
        });
    }

    private void proxyPasswordReveal(MouseEvent event) {
        proxyPasswordText = proxyPassword.getText();
        proxyPasswordCaretPosition = proxyPassword.getCaretPosition();
        proxyPassword.clear();
        proxyPassword.setPromptText(proxyPasswordText);
    }

    private void proxyPasswordMask(MouseEvent event) {
        if (!proxyPasswordText.equals("")) {
            proxyPassword.setText(proxyPasswordText);
            proxyPassword.positionCaret(proxyPasswordCaretPosition);
            proxyPassword.setPromptText("");
            proxyPasswordText = "";
            proxyPasswordCaretPosition = 0;
        }
    }

    @FXML
    void checkConnection() {
        viewModel.checkConnection();
    }
}
