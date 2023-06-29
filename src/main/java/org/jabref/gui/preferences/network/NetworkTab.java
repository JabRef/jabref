package org.jabref.gui.preferences.network;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;
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
    @FXML private Button checkConnectionButton;
    @FXML private CheckBox proxyPersistPassword;

    @FXML private TableView<CustomCertificateViewModel> customCertificatesTable;
    @FXML private TableColumn<CustomCertificateViewModel, String> certIssuer;
    @FXML private TableColumn<CustomCertificateViewModel, String> certSerialNumber;
    @FXML private TableColumn<CustomCertificateViewModel, String> certSignatureAlgorithm;
    @FXML private TableColumn<CustomCertificateViewModel, String> certValidFrom;
    @FXML private TableColumn<CustomCertificateViewModel, String> certValidTo;
    @FXML private TableColumn<CustomCertificateViewModel, String> certVersion;
    @FXML private TableColumn<CustomCertificateViewModel, String> actionsColumn;

    @Inject private FileUpdateMonitor fileUpdateMonitor;

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
        this.viewModel = new NetworkTabViewModel(dialogService, preferencesService, fileUpdateMonitor);

        remoteLabel.setVisible(preferencesService.getWorkspacePreferences().shouldShowAdvancedHints());
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
        proxyPersistPassword.selectedProperty().bindBidirectional(viewModel.proxyPersistPasswordProperty());
        proxyPersistPassword.disableProperty().bind(proxyCustomAndAuthentication.not());

        proxyPassword.setRight(IconTheme.JabRefIcons.PASSWORD_REVEALED.getGraphicNode());
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_PRESSED, this::proxyPasswordReveal);
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_RELEASED, this::proxyPasswordMask);
        proxyPassword.getRight().addEventFilter(MouseEvent.MOUSE_EXITED, this::proxyPasswordMask);

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.REMOTE, dialogService), remoteHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.remotePortValidationStatus(), remotePort);
            validationVisualizer.initVisualization(viewModel.proxyHostnameValidationStatus(), proxyHostname);
            validationVisualizer.initVisualization(viewModel.proxyPortValidationStatus(), proxyPort);
            validationVisualizer.initVisualization(viewModel.proxyUsernameValidationStatus(), proxyUsername);
            validationVisualizer.initVisualization(viewModel.proxyPasswordValidationStatus(), proxyPassword);
        });

        certSerialNumber.setCellValueFactory(data -> data.getValue().serialNumberProperty());
        certIssuer.setCellValueFactory(data -> data.getValue().issuerProperty());
        certSignatureAlgorithm.setCellValueFactory(data -> data.getValue().signatureAlgorithmProperty());
        certVersion.setCellValueFactory(data -> EasyBind.map(data.getValue().versionProperty(), this::formatVersion));

        certValidFrom.setCellValueFactory(data -> EasyBind.map(data.getValue().validFromProperty(), this::formatDate));
        certValidTo.setCellValueFactory(data -> EasyBind.map(data.getValue().validToProperty(), this::formatDate));

        customCertificatesTable.itemsProperty().set(viewModel.customCertificateListProperty());

        actionsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getThumbprint()));
        new ValueTableCellFactory<CustomCertificateViewModel, String>()
                .withGraphic(name -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove formatter '%0'", name))
                .withOnMouseClickedEvent(thumbprint -> evt -> viewModel.customCertificateListProperty().removeIf(cert -> cert.getThumbprint().equals(thumbprint)))
                .install(actionsColumn);
    }

    private String formatDate(LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
    }

    private String formatVersion(String version) {
        return String.format("V%s", version);
    }

    private void proxyPasswordReveal(MouseEvent event) {
        proxyPasswordText = proxyPassword.getText();
        proxyPasswordCaretPosition = proxyPassword.getCaretPosition();
        proxyPassword.clear();
        proxyPassword.setPromptText(proxyPasswordText);
    }

    private void proxyPasswordMask(MouseEvent event) {
        if (!"".equals(proxyPasswordText)) {
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

    @FXML
    void addCertificateFile() {
        viewModel.addCertificateFile();
    }
}
