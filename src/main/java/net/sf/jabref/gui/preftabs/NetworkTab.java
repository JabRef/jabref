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
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

public class NetworkTab extends JPanel implements PrefsTab {

    private final JCheckBox useProxy;
    private final JTextField defProxyHostname;
    private final JTextField defProxyPort;
    private final JabRefPreferences prefs;

    public NetworkTab(JabRefPreferences prefs) {
        this.prefs = prefs;

        setLayout(new BorderLayout());

        useProxy = new JCheckBox(Localization.lang("Use custom proxy configuration"));

        defProxyHostname = new JTextField();
        defProxyHostname.setEnabled(false);
        defProxyPort = new JTextField();
        defProxyPort.setEnabled(false);

        Insets marg = new Insets(0, 12, 3, 0);
        useProxy.setMargin(marg);
        defProxyPort.setMargin(marg);

        // We need a listener on useImportInspector to enable and disable the
        // import inspector related choices;
        useProxy.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                defProxyHostname.setEnabled(useProxy.isSelected());
                defProxyPort.setEnabled(useProxy.isSelected());
            }
        });

        FormLayout layout = new FormLayout
                ("8dlu, left:pref, 4dlu, fill:150dlu", "p, 2dlu, p, 2dlu, p, 2dlu, p");
        FormBuilder builder = FormBuilder.create().layout(layout);

        builder.addSeparator(Localization.lang("Network")).xyw(1, 1, 4);
        builder.add(useProxy).xyw(2, 3, 3);
        builder.add(Localization.lang("Host") + ':').xy(2, 5);
        builder.add(defProxyHostname).xy(4, 5);
        builder.add(Localization.lang("Port") + ':').xy(2, 7);
        builder.add(defProxyPort).xy(4, 7);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {

        useProxy.setSelected(prefs.getBoolean(JabRefPreferences.USE_PROXY));
        defProxyHostname.setText(prefs.get(JabRefPreferences.PROXY_HOSTNAME));
        defProxyPort.setText(prefs.get(JabRefPreferences.PROXY_PORT));

    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.USE_PROXY, useProxy.isSelected());
        prefs.put(JabRefPreferences.PROXY_HOSTNAME, defProxyHostname.getText().trim());
        prefs.put(JabRefPreferences.PROXY_PORT, defProxyPort.getText().trim());
    }

    @Override
    public boolean validateSettings() {
        boolean validSetting;
        if (useProxy.isSelected()) {
            String host = defProxyHostname.getText();
            String port = defProxyPort.getText();
            if ((host == null) || host.trim().isEmpty() ||
                    (port == null) || port.trim().isEmpty()) {
                validSetting = false;
            } else {
                Integer p;
                try {
                    p = Util.intValueOf(port);
                    validSetting = p > 0;
                } catch (NumberFormatException e) {
                    validSetting = false;
                }
            }
        } else {
            validSetting = true;
        }
        if (!validSetting) {
            JOptionPane.showMessageDialog
                    (null, Localization.lang("Please specify both hostname and port"),
                            Localization.lang("Invalid setting"),
                            JOptionPane.ERROR_MESSAGE);
        }
        return validSetting;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Network");
    }
}
