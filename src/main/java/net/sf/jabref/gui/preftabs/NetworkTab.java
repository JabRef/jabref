/*  Copyright (C) 2013 JabRef contributors.
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;

public class NetworkTab extends JPanel implements PrefsTab {

    private final JCheckBox useProxy;
    private final JTextField defProxyHostname;
    private final JTextField defProxyPort;
    private final JCheckBox useProxyAuthentication;
    private final JTextField defProxyUsername;
    private final JPasswordField defProxyPassword;
    private final JLabel lblPasswordWarning;
    private final JabRefPreferences prefs;
    private int oldProxyConfigHash;
    private boolean oldUseProxy, oldUseProxyAuth;


    public NetworkTab(JabRefPreferences prefs) {
        this.prefs = prefs;

        setLayout(new BorderLayout());

        useProxy = new JCheckBox(Localization.lang("Use custom proxy configuration"));

        defProxyHostname = new JTextField();
        defProxyHostname.setEnabled(false);
        defProxyPort = new JTextField();
        defProxyPort.setEnabled(false);

        useProxyAuthentication = new JCheckBox(Localization.lang("Proxy requires authentication"));
        useProxyAuthentication.setEnabled(false);

        defProxyUsername = new JTextField();
        defProxyUsername.setEnabled(false);
        defProxyPassword = new JPasswordField();
        defProxyPassword.setEnabled(false);
        lblPasswordWarning = new JLabel(Localization.lang("Attention: Password is stored in plain text!"));
        lblPasswordWarning.setEnabled(false);
        lblPasswordWarning.setForeground(Color.RED);

        Insets marg = new Insets(0, 12, 3, 0);
        useProxy.setMargin(marg);
        defProxyPort.setMargin(marg);
        useProxyAuthentication.setMargin(marg);

        // We need a listener on useProxy to enable and disable the
        // proxy related settings;
        useProxy.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                defProxyHostname.setEnabled(useProxy.isSelected());
                defProxyPort.setEnabled(useProxy.isSelected());
                useProxyAuthentication.setEnabled(useProxy.isSelected());
            }
        });

        useProxyAuthentication.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                defProxyUsername.setEnabled(useProxy.isSelected() && useProxyAuthentication.isSelected());
                defProxyPassword.setEnabled(useProxy.isSelected() && useProxyAuthentication.isSelected());
                lblPasswordWarning.setEnabled(useProxy.isSelected() && useProxyAuthentication.isSelected());
            }
        });

        FormLayout layout = new FormLayout("8dlu, left:pref, 4dlu, left:pref, 4dlu, fill:150dlu",
                "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, p");
        FormBuilder builder = FormBuilder.create().layout(layout);

        builder.addSeparator(Localization.lang("Network")).xyw(1, 1, 6);
        builder.add(useProxy).xyw(2, 3, 5);
        builder.add(Localization.lang("Host") + ':').xy(2, 5);
        builder.add(defProxyHostname).xyw(4, 5, 3);
        builder.add(Localization.lang("Port") + ':').xy(2, 7);
        builder.add(defProxyPort).xyw(4, 7, 3);
        builder.add(useProxyAuthentication).xyw(4, 9, 3);
        builder.add(Localization.lang("Username:")).xy(4, 11);
        builder.add(defProxyUsername).xy(6, 11);
        builder.add(Localization.lang("Password:")).xy(4, 13);
        builder.add(defProxyPassword).xy(6, 13);
        builder.add(lblPasswordWarning).xy(6, 14);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    private int getProxyConfigHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(prefs.get(JabRefPreferences.PROXY_USERNAME)).append(':')
                .append(prefs.get(JabRefPreferences.PROXY_PASSWORD));
        sb.append('@').append(prefs.get(JabRefPreferences.PROXY_HOSTNAME)).append(':')
                .append(prefs.get(JabRefPreferences.PROXY_PORT));
        return sb.toString().hashCode();
    }

    @Override
    public void setValues() {
        useProxy.setSelected(prefs.getBoolean(JabRefPreferences.USE_PROXY));
        defProxyHostname.setText(prefs.get(JabRefPreferences.PROXY_HOSTNAME));
        defProxyPort.setText(prefs.get(JabRefPreferences.PROXY_PORT));

        useProxyAuthentication.setSelected(prefs.getBoolean(JabRefPreferences.USE_PROXY_AUTHENTICATION));
        defProxyUsername.setText(prefs.get(JabRefPreferences.PROXY_USERNAME));
        defProxyPassword.setText(prefs.get(JabRefPreferences.PROXY_PASSWORD));

        oldUseProxy = prefs.getBoolean(JabRefPreferences.USE_PROXY);
        oldUseProxyAuth = prefs.getBoolean(JabRefPreferences.USE_PROXY_AUTHENTICATION);
        oldProxyConfigHash = getProxyConfigHash();

    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.USE_PROXY, useProxy.isSelected());
        prefs.put(JabRefPreferences.PROXY_HOSTNAME, defProxyHostname.getText().trim());
        prefs.put(JabRefPreferences.PROXY_PORT, defProxyPort.getText().trim());
        prefs.putBoolean(JabRefPreferences.USE_PROXY_AUTHENTICATION, useProxyAuthentication.isSelected());
        prefs.put(JabRefPreferences.PROXY_USERNAME, defProxyUsername.getText().trim());
        prefs.put(JabRefPreferences.PROXY_PASSWORD, new String(defProxyPassword.getPassword()));
        if ((oldUseProxy != useProxy.isSelected()) || (oldUseProxyAuth != useProxyAuthentication.isSelected())
                || (getProxyConfigHash() != oldProxyConfigHash)) {
            JOptionPane.showMessageDialog(null, Localization.lang("You have changed the proxy settings.").concat(" ")
                    .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                    Localization.lang("Changed proxy settings"), JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public boolean validateSettings() {
        boolean validSetting, validAuthenticationSetting = false;
        if (useProxy.isSelected()) {
            String host = defProxyHostname.getText();
            String port = defProxyPort.getText();
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
            if (useProxyAuthentication.isSelected()) {
                String userName = defProxyUsername.getText();
                char[] password = defProxyPassword.getPassword();
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
            if (!validAuthenticationSetting) {
                JOptionPane.showMessageDialog(null, Localization.lang("Please specify both username and password"),
                        Localization.lang("Invalid setting"), JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, Localization.lang("Please specify both hostname and port"),
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
