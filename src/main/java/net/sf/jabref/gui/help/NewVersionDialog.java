/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.gui.help;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.Version;
import net.sf.jabref.logic.util.VersionPreferences;

public class NewVersionDialog extends JDialog {

    public NewVersionDialog(JFrame frame, Version currentVersion, Version latestVersion, Version toBeIgnored) {
        super(frame);
        setTitle(Localization.lang("New version available"));

        JLabel lblTitle = new JLabel(Localization.lang("A new version of JabRef has been released."));
        JLabel lblCurrentVersion = new JLabel(Localization.lang("Installed version") + ": " + currentVersion.getFullVersion());
        JLabel lblLatestVersion = new JLabel(Localization.lang("Latest version") + ": " + latestVersion.getFullVersion());

        String localization = Localization.lang("To see what's new view the changelog.");
        JLabel lblMoreInformation = new JLabel("<HTML><a href=" + latestVersion.getChangelogUrl() + "'>" + localization + "</a></HTML>");
        lblMoreInformation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblMoreInformation.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JabRefDesktop.openBrowserShowPopup(latestVersion.getChangelogUrl());
            }
        });

        JButton btnIgnoreUpdate = new JButton(Localization.lang("Ignore this update"));
        btnIgnoreUpdate.addActionListener(e -> {
            new VersionPreferences(Globals.prefs).setAsIgnoredVersion(toBeIgnored);
            dispose();
        });

        JButton btnDownloadUpdate = new JButton(Localization.lang("Download update"));
        btnDownloadUpdate.addActionListener(e -> {
            JabRefDesktop.openBrowserShowPopup(Version.JABREF_DOWNLOAD_URL);
            dispose();
        });

        JButton btnRemindMeLater = new JButton(Localization.lang("Remind me later"));
        btnRemindMeLater.addActionListener(e -> dispose());

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridheight = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 5, 5, 2);

        c.gridx = c.gridy = 0;
        c.gridwidth = 3;
        panel.add(lblTitle, c);

        c.gridy = 1;
        panel.add(lblCurrentVersion, c);

        c.gridy = 2;
        panel.add(lblLatestVersion, c);

        c.gridy = 3;
        panel.add(lblMoreInformation, c);

        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 1;
        panel.add(btnDownloadUpdate, c);

        c.gridx = 1;
        panel.add(btnIgnoreUpdate, c);

        c.gridx = 2;
        panel.add(btnRemindMeLater, c);

        add(panel);
        pack();
        setLocationRelativeTo(frame);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }

}
