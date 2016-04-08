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
package net.sf.jabref.sql.importer;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import net.sf.jabref.BibDatabaseContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.*;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import net.sf.jabref.sql.DBConnectDialog;
import net.sf.jabref.sql.DBExporterAndImporterFactory;
import net.sf.jabref.sql.DBImportExportDialog;
import net.sf.jabref.sql.DBStrings;
import net.sf.jabref.sql.SQLUtil;

/**
 * Created by IntelliJ IDEA. User: alver Date: Mar 27, 2008 Time: 6:09:08 PM To change this template use File | Settings
 * | File Templates.
 * <p>
 * Jan. 20th Changed to accommodate the new way to connect to DB and also to show the exceptions and to display more than
 * one DB imported (by ifsteinm)
 */
public class DbImportAction extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(DbImportAction.class);

    private BibDatabaseContext databaseContext;
    private boolean connectedToDB;
    private final JabRefFrame frame;
    private DBStrings dbs;
    private List<DBImporterResult> databases;


    public DbImportAction(JabRefFrame frame) {
        this.frame = frame;
    }

    public AbstractAction getAction() {
        return new DbImpAction();
    }


    class DbImpAction extends MnemonicAwareAction {

        public DbImpAction() {
            super();
            putValue(Action.NAME, Localization.menuTitle("Import from external SQL database"));

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Util.runAbstractWorker(DbImportAction.this);
            } catch (Throwable throwable) {
                LOGGER.warn("Problem importing from database", throwable);
            }
        }
    }


    // run first, in EDT:
    @Override
    public void init() {

        dbs = new DBStrings();
        dbs.initialize();
        DBConnectDialog dbd = new DBConnectDialog(frame, dbs);
        dbs = dbd.getDBStrings();
        // panel.metaData().getDBStrings();

        // get DBStrings from user if necessary
        if (dbs.isConfigValid()) {
            connectedToDB = true;
        } else {
            // init DB strings if necessary
            if (!dbs.isInitialized()) {
                dbs.initialize();
            }

            // show connection dialog
            dbd = new DBConnectDialog(frame, dbs);
            dbd.setLocationRelativeTo(frame);
            dbd.setVisible(true);

            connectedToDB = dbd.isConnectedToDB();

            // store database strings
            if (connectedToDB) {
                dbs = dbd.getDBStrings();
                dbd.dispose();
            }
        }

    }

    // run second, on a different thread:
    @Override
    public void run() {
        performImport();
    }

    private void performImport() {
        if (connectedToDB) {
            try {
                frame.output(Localization.lang("Attempting SQL import..."));
                DBExporterAndImporterFactory factory = new DBExporterAndImporterFactory();
                DBImporter importer = factory.getImporter(dbs.getServerType());
                try (Connection conn = importer.connectToDB(dbs);
                        Statement statement = SQLUtil.queryAllFromTable(conn, "jabref_database");
                        ResultSet rs = statement.getResultSet()) {

                    Vector<String> v;
                    Vector<Vector<String>> matrix = new Vector<>();

                    while (rs.next()) {
                        v = new Vector<>();
                        v.add(rs.getString("database_name"));
                        matrix.add(v);
                    }

                    if (matrix.isEmpty()) {
                        JOptionPane.showMessageDialog(frame,
                                Localization.lang("There are no available databases to be imported"),
                                Localization.lang("Import from SQL database"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        DBImportExportDialog dialogo = new DBImportExportDialog(frame, matrix,
                                DBImportExportDialog.DialogType.IMPORTER);
                        if (dialogo.removeAction) {
                            String dbName = dialogo.selectedDB;
                            importer.removeDB(dialogo, dbName, conn, databaseContext);
                            performImport();
                        } else if (dialogo.moreThanOne) {
                            databases = importer.performImport(dbs, dialogo.listOfDBs, frame.getCurrentBasePanel().getBibDatabaseContext().getMode());
                            for (DBImporterResult res : databases) {
                                databaseContext = res.getDatabaseContext();
                                dbs.isConfigValid(true);
                            }
                            frame.output(Localization.lang("%0 databases will be imported",
                                    Integer.toString(databases.size())));
                        } else {
                            frame.output(Localization.lang("Importing cancelled"));
                        }
                    }
                }
            } catch (Exception ex) {
                String preamble = Localization.lang("Could not import from SQL database for the following reason:");
                String errorMessage = SQLUtil.getExceptionMessage(ex);
                dbs.isConfigValid(false);
                JOptionPane.showMessageDialog(frame, preamble + '\n' + errorMessage,
                        Localization.lang("Import from SQL database"), JOptionPane.ERROR_MESSAGE);
                frame.output(Localization.lang("Error importing from database"));
                LOGGER.error("Error importing from databae", ex);
            }
        }
    }

    // run third, on EDT:
    @Override
    public void update() {
        if (databases == null) {
            return;
        }
        for (DBImporterResult res : databases) {
            databaseContext = res.getDatabaseContext();
            if (databaseContext != null) {
                BasePanel pan = frame.addTab(databaseContext, Globals.prefs.getDefaultEncoding(), true);
                pan.getBibDatabaseContext().getMetaData().setDBStrings(dbs);
                frame.setTabTitle(pan, res.getName() + "(Imported)", "Imported DB");
                pan.markBaseChanged();
            }
        }
        frame.output(Localization.lang("Imported %0 databases successfully", Integer.toString(databases.size())));
    }

}
