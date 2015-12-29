/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.collab;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import net.sf.jabref.gui.*;
import net.sf.jabref.logic.l10n.Localization;

public class FileUpdatePanel extends SidePaneComponent implements ActionListener,
        ChangeScanner.DisplayResultCallback {

    public static final String NAME = "fileUpdate";

    private final SidePaneManager manager;

    private final ChangeScanner scanner;


    public FileUpdatePanel(BasePanel panel, SidePaneManager manager, File file, ChangeScanner scanner) {
        super(manager, IconTheme.JabRefIcon.SAVE.getIcon(), Localization.lang("File changed"));
        close.setEnabled(false);
        this.panel = panel;
        this.manager = manager;
        this.scanner = scanner;

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());

        JLabel message = new JLabel("<html><center>"
                + Localization.lang("The file<BR>'%0'<BR>has been modified<BR>externally!", file.getName())
                + "</center></html>", SwingConstants.CENTER);

        main.add(message, BorderLayout.CENTER);
        JButton test = new JButton(Localization.lang("Review changes"));
        main.add(test, BorderLayout.SOUTH);
        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        add(main, BorderLayout.CENTER);
        test.addActionListener(this);
    }

    /**
     * We include a getter for the BasePanel this component refers to, because this
     * component needs to be closed if the BasePanel is closed.
     * @return the base panel this component refers to.
     */
    public BasePanel getPanel() {
        return panel;
    }

    /**
     * Unregister when this component closes. We need that to avoid showing
     * two such external change warnings at the same time, only the latest one.
     */
    @Override
    public void componentClosing() {
        manager.unregisterComponent(FileUpdatePanel.NAME);
    }

    /**
     * actionPerformed
     *
     * @param e
     *            ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // ChangeScanner scanner = new ChangeScanner(frame, panel); //,
        // panel.database(), panel.metaData());
        // try {
        scanner.displayResult(this);
        // scanner.changeScan(panel.file());

        // } catch (IOException ex) {
        // ex.printStackTrace();
        // }
    }

    /**
     * Callback method for signalling that the change scanner has displayed the
     * scan results to the user.
     * @param resolved true if there were no changes, or if the user has resolved them.
     */
    @Override
    public void scanResultsResolved(boolean resolved) {
        if (resolved) {
            manager.hideComponent(this);
            panel.setUpdatedExternally(false);
        }
    }
}
