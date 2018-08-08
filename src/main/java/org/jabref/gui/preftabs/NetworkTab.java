package org.jabref.gui.preftabs;

import java.awt.BorderLayout;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Paint;


import javax.swing.JPanel;

import org.jabref.gui.DialogService;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.preferences.JabRefPreferences;

public class NetworkTab extends JPanel implements PrefsTab {

    private final CheckBox useProxyCheckBox;
    private final TextField hostnameTextField;
    private final TextField portTextField;
    private final CheckBox useAuthenticationCheckBox;
    private final TextField usernameTextField;
    private final PasswordField passwordTextField;
    private final JabRefPreferences preferences;
    private ProxyPreferences oldProxyPreferences;
    private final DialogService dialogService;

    public NetworkTab(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;

        setLayout(new BorderLayout());

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

        GridPane builder = new GridPane();

        builder.add(new Label(Localization.lang("Network")),1,1);
        builder.add(new Separator(),2,1);
        builder.add(useProxyCheckBox,2,2);
        builder.add(new Label(Localization.lang("    Hostname") + ':'),1,3);
        builder.add(hostnameTextField,2,3);
        builder.add(new Label(Localization.lang("    Port") + ':'),1,4);
        builder.add(portTextField,2,4);
        builder.add(useAuthenticationCheckBox,2,5);
        builder.add(new Label(Localization.lang("Username") + ':'),2,6);
        builder.add(usernameTextField,3,6);
        builder.add(new Label(Localization.lang("Password") + ':'),2,7);
        builder.add(passwordTextField,3,7);
        builder.add(passwordWarningLabel,3,8);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
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
