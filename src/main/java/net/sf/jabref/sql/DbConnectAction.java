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
package net.sf.jabref.sql;

import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.sql.exporter.DBExporter;

import javax.swing.*;
import java.sql.Connection;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: alver Date: Mar 27, 2008 Time: 6:05:13 PM To
 * change this template use File | Settings | File Templates.
 *
 * Jan 20th Adjusted to accomodate changes on SQL Exporter module by ifsteinm
 *
 */
public class DbConnectAction implements BaseAction {

    private final BasePanel panel;


    public DbConnectAction(BasePanel panel) {
        this.panel = panel;
    }

    public AbstractAction getAction() {
        return new DbImpAction();
    }


    private class DbImpAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            action();

        }
    }


    @Override
    public void action() {

        DBStrings dbs = panel.getBibDatabaseContext().getMetaData().getDBStrings();

        // init DB strings if necessary
        if (!dbs.isInitialized()) {
            dbs.initialize();
        }

        // show connection dialog
        DBConnectDialog dbd = new DBConnectDialog(panel.frame(), dbs);
        dbd.setLocationRelativeTo(panel);
        dbd.setVisible(true);

        // connect to database to test DBStrings
        if (dbd.isConnectedToDB()) {

            dbs = dbd.getDBStrings();

            try {

                panel.frame().output(
                        Localization.lang("Establishing SQL connection..."));
                DBExporter exporter = (new DBExporterAndImporterFactory())
                        .getExporter(dbs.getServerType());
                try (Connection conn = exporter.connectToDB(dbs)) {
                    // Nothing
                }
                dbs.isConfigValid(true);
                panel.frame().output(
                        Localization.lang("SQL connection established."));
            } catch (Exception ex) {
                String errorMessage = SQLUtil.getExceptionMessage(ex);
                dbs.isConfigValid(false);

                String preamble = Localization.lang("Could not connect to SQL database for the following reason:");
                panel.frame().output(preamble + "  " + errorMessage);

                JOptionPane.showMessageDialog(panel.frame(), preamble + '\n' + errorMessage,
                        Localization.lang("Connect to SQL database"),
                        JOptionPane.ERROR_MESSAGE);
            } finally {
                panel.getBibDatabaseContext().getMetaData().setDBStrings(dbs);
                dbd.dispose();
            }
        }
    }
}
