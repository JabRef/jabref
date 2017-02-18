package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ProxyRegisterer;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class NetworkTab extends JPanel implements PrefsTab {

    private final JCheckBox useProxyCheckBox;
    private final JTextField hostnameTextField;
    private final JTextField portTextField;
    private final JCheckBox useAuthenticationCheckBox;
    private final JTextField usernameTextField;
    private final JPasswordField passwordTextField;
    private final JabRefPreferences preferences;
    private ProxyPreferences oldProxyPreferences;

    public NetworkTab(JabRefPreferences preferences) {
        this.preferences = preferences;

        setLayout(new BorderLayout());

        useProxyCheckBox = new JCheckBox(Localization.lang("Use custom proxy configuration"));

        hostnameTextField = new JTextField();
        hostnameTextField.setEnabled(false);
        portTextField = new JTextField();
        portTextField.setEnabled(false);

        useAuthenticationCheckBox = new JCheckBox(Localization.lang("Proxy requires authentication"));
        useAuthenticationCheckBox.setEnabled(false);

        usernameTextField = new JTextField();
        usernameTextField.setEnabled(false);
        passwordTextField = new JPasswordField();
        passwordTextField.setEnabled(false);
        JLabel passwordWarningLabel = new JLabel(Localization.lang("Attention: Password is stored in plain text!"));
        passwordWarningLabel.setEnabled(false);
        passwordWarningLabel.setForeground(Color.RED);

        Insets margin = new Insets(0, 12, 3, 0);
        useProxyCheckBox.setMargin(margin);
        portTextField.setMargin(margin);
        useAuthenticationCheckBox.setMargin(margin);

        // Listener on useProxyCheckBox to enable and disable the proxy related settings;
        useProxyCheckBox.addChangeListener(event -> {
            hostnameTextField.setEnabled(useProxyCheckBox.isSelected());
            portTextField.setEnabled(useProxyCheckBox.isSelected());
            useAuthenticationCheckBox.setEnabled(useProxyCheckBox.isSelected());
        });

        useAuthenticationCheckBox.addChangeListener(event -> {
            usernameTextField.setEnabled(useProxyCheckBox.isSelected() && useAuthenticationCheckBox.isSelected());
            passwordTextField.setEnabled(useProxyCheckBox.isSelected() && useAuthenticationCheckBox.isSelected());
            passwordWarningLabel.setEnabled(useProxyCheckBox.isSelected() && useAuthenticationCheckBox.isSelected());
        });

        FormLayout layout = new FormLayout("8dlu, left:pref, 4dlu, left:pref, 4dlu, fill:150dlu",
                "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, p");
        FormBuilder builder = FormBuilder.create().layout(layout);

        builder.addSeparator(Localization.lang("Network")).xyw(1, 1, 6);
        builder.add(useProxyCheckBox).xyw(2, 3, 5);
        builder.add(Localization.lang("Hostname") + ':').xy(2, 5);
        builder.add(hostnameTextField).xyw(4, 5, 3);
        builder.add(Localization.lang("Port") + ':').xy(2, 7);
        builder.add(portTextField).xyw(4, 7, 3);
        builder.add(useAuthenticationCheckBox).xyw(4, 9, 3);
        builder.add(Localization.lang("Username") + ':').xy(4, 11);
        builder.add(usernameTextField).xy(6, 11);
        builder.add(Localization.lang("Password") + ':').xy(4, 13);
        builder.add(passwordTextField).xy(6, 13);
        builder.add(passwordWarningLabel).xy(6, 14);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
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
        String password = new String(passwordTextField.getPassword());
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
                char[] password = passwordTextField.getPassword();
                // no empty proxy passwords currently supported (they make no sense in this case anyway)
                if ((userName == null) || userName.trim().isEmpty() || (password == null) || (password.length == 0)) {
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
                JOptionPane.showMessageDialog(null, Localization.lang("Please specify both hostname and port"),
                        Localization.lang("Invalid setting"), JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, Localization.lang("Please specify both username and password"),
                        Localization.lang("Invalid setting"), JOptionPane.ERROR_MESSAGE);
            }
        }
        return validSetting;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Network");
    }
}
