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
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import net.sf.jabref.gui.*;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
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
 *
 * Jan. 20th Changed to accomodate the new way to connect to DB and also to show the exceptions and to display more than
 * one DB imported (by ifsteinm)
 *
 */
public class DbImportAction extends AbstractWorker {

    private BibtexDatabase database;
    private MetaData metaData;
    private boolean connectToDB;
    private final JabRefFrame frame;
    private DBStrings dbs;
    private ArrayList<Object[]> databases;


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
                throwable.printStackTrace();
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
        if (!dbs.isConfigValid()) {

            // init DB strings if necessary
            if (!dbs.isInitialized()) {
                dbs.initialize();
            }

            // show connection dialog
            dbd = new DBConnectDialog(frame, dbs);
            PositionWindow.placeDialog(dbd, frame);
            dbd.setVisible(true);

            connectToDB = dbd.getConnectToDB();

            // store database strings
            if (connectToDB) {
                dbs = dbd.getDBStrings();
                dbd.dispose();
            }

        } else {

            connectToDB = true;

        }

    }

    // run second, on a different thread:
    @Override
    public void run() {
        performImport();
    }

    private void performImport() {
        if (connectToDB) {
            try {
                frame.output(Localization.lang("Attempting SQL import..."));
                DBExporterAndImporterFactory factory = new DBExporterAndImporterFactory();
                DBImporter importer = factory.getImporter(dbs.getServerType());
                try (Connection conn = importer.connectToDB(dbs)) {
                    try (ResultSet rs = SQLUtil.queryAllFromTable(conn, "jabref_database")) {
                        Vector<String> v;
                        Vector<Vector<String>> matrix = new Vector<>();

                        while (rs.next()) {
                            v = new Vector<>();
                            v.add(rs.getString("database_name"));
                            matrix.add(v);
                        }

                        if (!matrix.isEmpty()) {
                            DBImportExportDialog dialogo = new DBImportExportDialog(frame, matrix,
                                    DBImportExportDialog.DialogType.IMPORTER);

                            if (dialogo.removeAction) {
                                String dbName = dialogo.selectedDB;
                                importer.removeDB(dialogo, dbName, conn, metaData);
                                performImport();
                            } else {
                                if (dialogo.moreThanOne) {
                                    databases = importer.performImport(dbs, dialogo.listOfDBs);
                                    for (Object[] res : databases) {
                                        database = (BibtexDatabase) res[0];
                                        metaData = (MetaData) res[1];
                                        dbs.isConfigValid(true);
                                    }
                                    frame.output(Localization.lang("%0 databases will be imported",
                                            Integer.toString(databases.size())));
                                } else {
                                    frame.output(Localization.lang("Importing cancelled"));
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(frame,
                                    Localization.lang("There are no available databases to be imported"),
                                    Localization.lang("Import from SQL database"), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            } catch (Exception ex) {
                String preamble = Localization.lang("Could not import from SQL database for the following reason:");
                String errorMessage = SQLUtil.getExceptionMessage(ex);
                dbs.isConfigValid(false);
                JOptionPane.showMessageDialog(frame, Localization.lang(preamble) + '\n' + errorMessage,
                        Localization.lang("Import from SQL database"), JOptionPane.ERROR_MESSAGE);
                frame.output(Localization.lang("Error importing from database"));
                ex.printStackTrace();
            }
        }
    }

    // run third, on EDT:
    @Override
    public void update() {
        if (databases == null) {
            return;
        }
        for (Object[] res : databases) {
            database = (BibtexDatabase) res[0];
            metaData = (MetaData) res[1];
            if (database != null) {
                BasePanel pan = frame.addTab(database, null, metaData,
 Globals.prefs.getDefaultEncoding(), true);
                pan.metaData().setDBStrings(dbs);
                frame.setTabTitle(pan, res[2] + "(Imported)", "Imported DB");
                pan.markBaseChanged();
            }
        }
        frame.output(Localization.lang("Imported %0 databases successfully", Integer.toString(databases.size())));
    }

}
