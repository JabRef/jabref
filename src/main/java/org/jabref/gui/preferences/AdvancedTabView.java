package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomPasswordField;

public class AdvancedTabView extends AbstractPreferenceTabView implements PreferencesTab {
    @FXML private Label remoteLabel;
    @FXML private CheckBox remoteServer;
    @FXML private TextField remotePort;
    @FXML private Button remoteHelp;

    @FXML private CheckBox useIEEELatexAbbreviations;
    @FXML private CheckBox useCaseKeeper;
    @FXML private CheckBox useUnitFormatter;

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

    private String proxyPasswordText = "";
    private int proxyPasswordCaretPosition = 0;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public AdvancedTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("Advanced"); }

    public void initialize() {
        AdvancedTabViewModel advancedTabViewModel = new AdvancedTabViewModel(dialogService, preferences);
        this.viewModel = advancedTabViewModel;

        remoteLabel.setVisible(preferences.getBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS));
        remoteServer.selectedProperty().bindBidirectional(advancedTabViewModel.remoteServerProperty());
        remotePort.textProperty().bindBidirectional(advancedTabViewModel.remotePortProperty());
        remotePort.disableProperty().bind(remoteServer.selectedProperty().not());

        useIEEELatexAbbreviations.selectedProperty().bindBidirectional(advancedTabViewModel.useIEEELatexAbbreviationsProperty());

        useCaseKeeper.selectedProperty().bindBidirectional(advancedTabViewModel.useCaseKeeperProperty());
        useUnitFormatter.selectedProperty().bindBidirectional(advancedTabViewModel.useUnitFormatterProperty());

        proxyUse.selectedProperty().bindBidirectional(advancedTabViewModel.proxyUseProperty());
        proxyHostnameLabel.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyHostname.textProperty().bindBidirectional(advancedTabViewModel.proxyHostnameProperty());
        proxyHostname.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyPortLabel.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyPort.textProperty().bindBidirectional(advancedTabViewModel.proxyPortProperty());
        proxyPort.disableProperty().bind(proxyUse.selectedProperty().not());
        proxyUseAuthentication.selectedProperty().bindBidirectional(advancedTabViewModel.proxyUseAuthenticationProperty());
        proxyUseAuthentication.disableProperty().bind(proxyUse.selectedProperty().not());

        BooleanBinding proxyCustomAndAuthentication = proxyUse.selectedProperty().and(proxyUseAuthentication.selectedProperty());
        proxyUsernameLabel.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyUsername.textProperty().bindBidirectional(advancedTabViewModel.proxyUsernameProperty());
        proxyUsername.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyPasswordLabel.disableProperty().bind(proxyCustomAndAuthentication.not());
        proxyPassword.textProperty().bindBidirectional(advancedTabViewModel.proxyPasswordProperty());
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
            validationVisualizer.initVisualization(advancedTabViewModel.remotePortValidationStatus(), remotePort);
            validationVisualizer.initVisualization(advancedTabViewModel.proxyHostnameValidationStatus(), proxyHostname);
            validationVisualizer.initVisualization(advancedTabViewModel.proxyPortValidationStatus(), proxyPort);
            validationVisualizer.initVisualization(advancedTabViewModel.proxyUsernameValidationStatus(), proxyUsername);
            validationVisualizer.initVisualization(advancedTabViewModel.proxyPasswordValidationStatus(), proxyPassword);
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
}
