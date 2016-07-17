/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.remote.JabRefMessageHandler;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.remote.RemoteUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class AdvancedTab extends JPanel implements PrefsTab {

    private final JabRefPreferences preferences;
    private final JCheckBox useRemoteServer;
    private final JCheckBox useIEEEAbrv;
    private final JTextField remoteServerPort;

    private final JCheckBox useConvertToEquation;
    private final JCheckBox useCaseKeeperOnSearch;
    private final JCheckBox useUnitFormatterOnSearch;
    private final RemotePreferences remotePreferences;

    private final JRadioButton defaultConsole;
    private final JRadioButton specifiedConsole;
    private final JTextField consoleEmulatorPath;
    private final JFileChooser consoleChooser;
    private final JButton browseButton;


    public AdvancedTab(JabRefPreferences prefs) {
        preferences = prefs;
        remotePreferences = new RemotePreferences(preferences);

        useRemoteServer = new JCheckBox(Localization.lang("Listen for remote operation on port") + ':');
        useIEEEAbrv = new JCheckBox(Localization.lang("Use IEEE LaTeX abbreviations"));
        remoteServerPort = new JTextField();
        useConvertToEquation = new JCheckBox(Localization.lang("Prefer converting subscripts and superscripts to equations rather than text"));
        useCaseKeeperOnSearch = new JCheckBox(Localization.lang("Add {} to specified title words on search to keep the correct case"));
        useUnitFormatterOnSearch = new JCheckBox(Localization.lang("Format units by adding non-breaking separators and keeping the correct case on search"));

        defaultConsole = new JRadioButton(Localization.lang("Use default terminal emulator"));
        specifiedConsole = new JRadioButton(Localization.lang("Specify terminal emulator") + ":");
        consoleEmulatorPath = new JTextField(25);
        consoleChooser = new JFileChooser();
        browseButton = new JButton(Localization.lang("Browse"));

        ButtonGroup consoleOptions = new ButtonGroup();
        consoleOptions.add(defaultConsole);
        consoleOptions.add(specifiedConsole);

        JPanel consoleOptionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints layoutConstraints = new GridBagConstraints();

        defaultConsole.addActionListener(new ConsoleRadioButtonActionListener());
        specifiedConsole.addActionListener(new ConsoleRadioButtonActionListener());
        browseButton.addActionListener(new BrowseButtonActionListener());

        layoutConstraints.fill = GridBagConstraints.HORIZONTAL;

        layoutConstraints.gridx = 0;
        layoutConstraints.gridy = 0;
        layoutConstraints.insets = new Insets(0, 0, 6, 0);
        consoleOptionPanel.add(defaultConsole, layoutConstraints);

        layoutConstraints.gridy = 1;
        consoleOptionPanel.add(specifiedConsole, layoutConstraints);

        layoutConstraints.gridx = 1;
        consoleOptionPanel.add(consoleEmulatorPath, layoutConstraints);
        layoutConstraints.gridx = 2;
        layoutConstraints.insets = new Insets(0, 4, 6, 0);
        consoleOptionPanel.add(browseButton, layoutConstraints);

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:3dlu",//, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
                        "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        JPanel pan = new JPanel();

        builder.appendSeparator(Localization.lang("Remote operation"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(new JLabel("<html>"
                + Localization.lang("This feature lets new files be opened or imported into an "
                        + "already running instance of JabRef<BR>instead of opening a new instance. For instance, this "
                        + "is useful when you open a file in JabRef<br>from your web browser."
                        + "<BR>Note that this will prevent you from running more than one instance of JabRef at a time.")
                + "</html>"));
        builder.nextLine();
        builder.append(new JPanel());

        JPanel p = new JPanel();
        p.add(useRemoteServer);
        p.add(remoteServerPort);
        p.add(new HelpAction(HelpFiles.REMOTE).getHelpButton());
        builder.append(p);

        // IEEE
        builder.nextLine();
        builder.appendSeparator(Localization.lang("Search %0", "IEEEXplore"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(useIEEEAbrv);

        builder.nextLine();
        builder.appendSeparator(Localization.lang("Import conversions"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(useConvertToEquation);
        builder.nextLine();
        builder.append(pan);
        builder.append(useCaseKeeperOnSearch);
        builder.nextLine();
        builder.append(pan);
        builder.append(useUnitFormatterOnSearch);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Open console"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(consoleOptionPanel);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);

    }

    @Override
    public void setValues() {
        useRemoteServer.setSelected(remotePreferences.useRemoteServer());
        remoteServerPort.setText(String.valueOf(remotePreferences.getPort()));
        useIEEEAbrv.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV));
        useConvertToEquation.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION));
        useCaseKeeperOnSearch.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH));
        useUnitFormatterOnSearch.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH));
        defaultConsole.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));
        specifiedConsole.setSelected(!Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));
        consoleEmulatorPath.setText(Globals.prefs.get(JabRefPreferences.CONSOLE_APPLICATION));

        updateEnableStatus();
    }

    @Override
    public void storeSettings() {
        if (preferences.getBoolean(JabRefPreferences.USE_IEEE_ABRV) != useIEEEAbrv.isSelected()) {
            preferences.putBoolean(JabRefPreferences.USE_IEEE_ABRV, useIEEEAbrv.isSelected());
            Globals.journalAbbreviationLoader.update(preferences);
        }
        storeRemoteSettings();

        preferences.putBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION, useConvertToEquation.isSelected());
        preferences.putBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH, useCaseKeeperOnSearch.isSelected());
        preferences.putBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH, useUnitFormatterOnSearch.isSelected());
        preferences.putBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION, defaultConsole.isSelected());
        preferences.put(JabRefPreferences.CONSOLE_APPLICATION, consoleEmulatorPath.getText());
    }

    private void storeRemoteSettings() {
        getPortAsInt().ifPresent(newPort -> {
            if (remotePreferences.isDifferentPort(newPort)) {
                remotePreferences.setPort(newPort);

                if (remotePreferences.useRemoteServer()) {
                    JOptionPane.showMessageDialog(null,
                            Localization.lang("Remote server port").concat(" ")
                                    .concat(Localization.lang("You must restart JabRef for this to come into effect.")),
                            Localization.lang("Remote server port"), JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        remotePreferences.setUseRemoteServer(useRemoteServer.isSelected());
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), remotePreferences.getPort());
        } else {
            Globals.REMOTE_LISTENER.stop();
        }
    }

    private Optional<Integer> getPortAsInt() {
        try {
            return Optional.of(Integer.parseInt(remoteServerPort.getText()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private class BrowseButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int answer = consoleChooser.showOpenDialog(AdvancedTab.this);
            if (answer == JFileChooser.APPROVE_OPTION) {
                consoleEmulatorPath.setText(consoleChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private class ConsoleRadioButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateEnableStatus();
        }
    }

    private void updateEnableStatus() {
        consoleEmulatorPath.setEnabled(specifiedConsole.isSelected());
        browseButton.setEnabled(specifiedConsole.isSelected());
    }

    @Override
    public boolean validateSettings() {
        boolean isValid = true;

        try {
            int portNumber = Integer.parseInt(remoteServerPort.getText());
            if (!RemoteUtil.isUserPort(portNumber)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    Localization.lang("You must enter an integer value in the interval 1025-65535 in the text field for")
                            + " '" + Localization.lang("Remote server port") + '\'',
                    Localization.lang("Remote server port"), JOptionPane.ERROR_MESSAGE);
            isValid = false;
        }

        if (!consoleEmulatorPath.getText().trim().isEmpty()) {
            Path path = Paths.get(consoleEmulatorPath.getText());

            if (!Files.exists(path) || Files.isDirectory(path) || !path.isAbsolute()) {
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Please type in the absolute path to an existing terminal emulator."),
                        Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                isValid = false;
            }
        }

        return isValid;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Advanced");
    }

}
