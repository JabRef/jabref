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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class NetworkTab extends JPanel implements PrefsTab {

    private final JCheckBox useProxy;
    private final JTextField defProxyHostname;
    private final JTextField defProxyPort;
    private final JabRefPreferences _prefs;


    //    private HelpAction ownerHelp, timeStampHelp;

    public NetworkTab(JabRefFrame frame, JabRefPreferences prefs) {
        _prefs = prefs;

        setLayout(new BorderLayout());

        useProxy = new JCheckBox(Globals.lang("Use custom proxy configuration"));

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
                //useProxy.setEnabled(useProxy.isSelected());
                defProxyHostname.setEnabled(useProxy.isSelected());
                defProxyPort.setEnabled(useProxy.isSelected());
            }
        });

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", "");
        //("right:pref, 10dlu, 50dlu, 5dlu, fill:60dlu", "");
        //("10dlu, left:50dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Globals.lang("Network"));
        builder.nextLine();
        builder.append(useProxy, 5);
        builder.nextLine();
        builder.append(new JPanel());
        JLabel lap = new JLabel(Globals.lang("Host") + ':');
        builder.append(lap);
        builder.append(defProxyHostname);
        builder.nextLine();
        builder.append(new JPanel());
        JLabel lap2 = new JLabel(Globals.lang("Port") + ':');
        builder.append(lap2);
        //builder.append(new JPanel());
        builder.append(defProxyPort);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {

        useProxy.setSelected(_prefs.getBoolean(JabRefPreferences.USE_PROXY));
        //_prefs.putBoolean("defaultAutoSort", defSorrrt.isSelected());
        defProxyHostname.setText(_prefs.get(JabRefPreferences.PROXY_HOSTNAME));
        defProxyPort.setText(_prefs.get(JabRefPreferences.PROXY_PORT));

    }

    @Override
    public void storeSettings() {
        _prefs.putBoolean(JabRefPreferences.USE_PROXY, useProxy.isSelected());
        //_prefs.putBoolean("defaultAutoSort", defSorrrt.isSelected());
        _prefs.put(JabRefPreferences.PROXY_HOSTNAME, defProxyHostname.getText().trim());
        _prefs.put(JabRefPreferences.PROXY_PORT, defProxyPort.getText().trim());
    }

    @Override
    public boolean readyToClose() {
        boolean validSetting;
        if (useProxy.isSelected()) {
            String host = defProxyHostname.getText();
            String port = defProxyPort.getText();
            if ((host == null) || (host.trim().isEmpty()) ||
                    (port == null) || (port.trim().isEmpty())) {
                validSetting = false;
            } else {
                Integer p;
                try {
                    p = Integer.parseInt(port);
                    validSetting = (p > 0);
                } catch (NumberFormatException e) {
                    validSetting = false;
                }
            }
        } else {
            validSetting = true;
        }
        if (!validSetting) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("Please specify both hostname and port"),
                            Globals.lang("Invalid setting"),
                            JOptionPane.ERROR_MESSAGE);
        }
        return validSetting;
    }

    @Override
    public String getTabName() {
        return Globals.lang("Network");
    }
}
