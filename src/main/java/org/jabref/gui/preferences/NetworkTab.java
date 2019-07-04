package org.jabref.gui.preferences;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.preferences.JabRefPreferences;

public class NetworkTab extends Pane implements PrefsTab {

    private final CheckBox useProxyCheckBox;
    private final TextField hostnameTextField;
    private final TextField portTextField;
    private final CheckBox useAuthenticationCheckBox;
    private final TextField usernameTextField;
    private final PasswordField passwordTextField;
    private final JabRefPreferences preferences;
    private ProxyPreferences oldProxyPreferences;
    private final DialogService dialogService;
    private final GridPane builder = new GridPane();

    public NetworkTab(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        builder.setVgap(8);

        useProxyCheckBox = new CheckBox(Localization.lang("Use custom proxy configuration"));
        hostnameTextField = new TextField();
        hostnameTextField.setDisable(true);
        portTextField = new TextField();
        portTextField.setDisable(true);

        useAuthenticationCheckBox = new CheckBox(Localization.lang("Proxy requires authentication"));
        useAuthenticationCheckBox.setDisable(true);

        usernameTextField = new TextField();
        usernameTextField.setDisable(true);
        passwordTextField = new PasswordField();
        passwordTextField.setDisable(true);
        Label passwordWarningLabel = new Label(Localization.lang("Attention: Password is stored in plain text!"));
        passwordWarningLabel.setDisable(true);
        passwordWarningLabel.setTextFill(Paint.valueOf("Red"));

        // Listener on useProxyCheckBox to enable and disable the proxy related settings;
        useProxyCheckBox.setOnAction(event -> {
            hostnameTextField.setDisable(!useProxyCheckBox.isSelected());
            portTextField.setDisable(!useProxyCheckBox.isSelected());
            useAuthenticationCheckBox.setDisable(!useProxyCheckBox.isSelected());
        });

        useAuthenticationCheckBox.setOnAction(event -> {
            usernameTextField.setDisable(!useProxyCheckBox.isSelected() || !useAuthenticationCheckBox.isSelected());
            passwordTextField.setDisable(!useProxyCheckBox.isSelected() || !useAuthenticationCheckBox.isSelected());
            passwordWarningLabel.setDisable(!useProxyCheckBox.isSelected() || !useAuthenticationCheckBox.isSelected());
        });

        Label network = new Label(Localization.lang("Network"));
        network.getStyleClass().add("sectionHeader");
        builder.add(network, 1, 1);
        builder.add(useProxyCheckBox, 1, 4);

        // Hostname configuration
        HBox hostnameBox = new HBox();
        hostnameBox.setSpacing(10);
        hostnameBox.setAlignment(Pos.CENTER_LEFT);
        Label hostname = new Label(Localization.lang("Hostname") + ':');
        hostnameBox.getChildren().setAll(hostname, hostnameTextField);
        builder.add(hostnameBox, 1, 5);

        // Port configuration
        HBox portBox = new HBox();
        portBox.setSpacing(50);
        portBox.setAlignment(Pos.CENTER_LEFT);
        Label port = new Label(Localization.lang("Port") + ':');
        portBox.getChildren().setAll(port, portTextField, useAuthenticationCheckBox);
        builder.add(portBox, 1, 7);

        builder.add(new Separator(), 1, 12);

        // Username configuration
        HBox usernameBox = new HBox();
        usernameBox.setSpacing(10);
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        Label username = new Label(Localization.lang("Username") + ':');
        usernameBox.getChildren().setAll(username, usernameTextField);
        builder.add(usernameBox, 1, 15);

        // Password configuration
        HBox passwordBox = new HBox();
        passwordBox.setSpacing(15);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Label password = new Label(Localization.lang("Password") + ':');
        passwordBox.getChildren().setAll(password, passwordTextField, passwordWarningLabel);
        builder.add(passwordBox, 1, 16);
    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        ProxyPreferences proxyPreferences = preferences.getProxyPreferences();
        useProxyCheckBox.setSelected(proxyPreferences.isUseProxy());
        hostnameTextField.setText(proxyPreferences.getHostname());
        portTextField.setText(proxyPreferences.getPort());

        useAuthenticationCheckBox.setSelected(proxyPreferences.isUseAuthentication());
        usernameTextField.setText(proxyPreferences.getUsername());
        passwordTextField.setText(proxyPreferences.getPassword());

        oldProxyPreferences = proxyPreferences;
    }

    @Override
    public void storeSettings() {
        Boolean useProxy = useProxyCheckBox.isSelected();
        String hostname = hostnameTextField.getText().trim();
        String port = portTextField.getText().trim();
        Boolean useAuthentication = useAuthenticationCheckBox.isSelected();
        String username = usernameTextField.getText().trim();
        String password = passwordTextField.getText();
        ProxyPreferences proxyPreferences = new ProxyPreferences(useProxy, hostname, port, useAuthentication, username,
                password);
        if (!proxyPreferences.equals(oldProxyPreferences)) {
            ProxyRegisterer.register(proxyPreferences);
        }
        preferences.storeProxyPreferences(proxyPreferences);
    }

    @Override
    public boolean validateSettings() {
        boolean validSetting;
        boolean validAuthenticationSetting = false;
        if (useProxyCheckBox.isSelected()) {
            String host = hostnameTextField.getText();
            String port = portTextField.getText();
            if ((host == null) || host.trim().isEmpty() || (port == null) || port.trim().isEmpty()) {
                validSetting = false;
            } else {
                Integer p;
                try {
                    p = Integer.parseInt(port);
                    validSetting = p > 0;
                } catch (NumberFormatException e) {
                    validSetting = false;
                }
            }
            if (useAuthenticationCheckBox.isSelected()) {
                String userName = usernameTextField.getText();
                char[] password = passwordTextField.getText().toCharArray();
                // no empty proxy passwords currently supported (they make no sense in this case anyway)
                if ((userName == null) || userName.trim().isEmpty()  || (password.length == 0)) {
                    validAuthenticationSetting = false;
                    validSetting = false;
                } else {
                    validAuthenticationSetting = true;
                }
            }
        } else {
            validSetting = true;
        }
        if (!validSetting) {
            if (validAuthenticationSetting) {

                DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Invalid setting"),
                        Localization.lang("Please specify both hostname and port")));
            } else {
                DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Invalid setting"),
                        Localization.lang("Please specify both username and password")));

            }
        }
        return validSetting;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Network");
    }
}
