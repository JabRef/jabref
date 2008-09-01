package net.sf.jabref.sql;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.Util;

/**
 * Created by IntelliJ IDEA.
* User: alver
* Date: Mar 27, 2008
* Time: 6:09:08 PM
* To change this template use File | Settings | File Templates.
*/
public class DbImportAction extends AbstractWorker {

    BibtexDatabase database = null;
    MetaData metaData = null;
    String errorMessage = null;
    boolean connectToDB = false;
    private JabRefFrame frame;
    private DBStrings dbs = null;

    public DbImportAction(JabRefFrame frame) {
        this.frame = frame;
    }

    public AbstractAction getAction() {
        return new DbImpAction();
    }

    class DbImpAction extends MnemonicAwareAction {
        public DbImpAction() {
            super(GUIGlobals.getImage("dbImport"));
            putValue(NAME, "Import from external SQL database");
            
        }
        public void actionPerformed(ActionEvent e) {
            try {
                Util.runAbstractWorker(DbImportAction.this);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    // run first, in EDT:
    public void init() {

        dbs = new DBStrings();
        dbs.initialize();
        DBConnectDialog dbd = new DBConnectDialog(frame, dbs);
        dbs = dbd.getDBStrings();
                //panel.metaData().getDBStrings();

        // get DBStrings from user if necessary
        if (!dbs.isConfigValid()) {

            // init DB strings if necessary
            if (! dbs.isInitialized()) {
                dbs.initialize();
            }

            // show connection dialog
            dbd = new DBConnectDialog(frame, dbs);
            Util.placeDialog(dbd, frame);
            dbd.setVisible(true);

            connectToDB = dbd.getConnectToDB();

            // store database strings
            if (connectToDB) {
                dbs = dbd.getDBStrings();
                dbd.dispose();
            }

        } else {

            connectToDB  = true;

        }

    }

    // run second, on a different thread:
    public void run() {

        if (connectToDB) {

            try {

                frame.output(Globals.lang("Attempting SQL import..."));
                Object[] res = SQLutil.importDatabase(null, dbs);
                database = (BibtexDatabase)res[0];
                metaData = (MetaData)res[1];
                dbs.isConfigValid(true);

            } catch (Exception ex) {

                errorMessage = SQLutil.getExceptionMessage(ex,SQLutil.DBTYPE.MYSQL);
                dbs.isConfigValid(false);

            }

        }

    }

    // run third, on EDT:
    public void update() {

        if (database != null) {

            BasePanel pan = frame.addTab(database, null, metaData,
                    Globals.prefs.get("defaultEncoding"), true);
            pan.metaData().setDBStrings(dbs);
            return;
        }
        /*String url = SQLutil.createJDBCurl(metaData.getDBStrings());

        // if no error, report success
        if (errorMessage == null) {
            if (connectToDB) {
                frame.output(Globals.lang("%0 export successful", url));
            }
        }

        // show an error dialog if an error occurred
        else {

            String preamble = "Could not export to SQL database for the following reason:";
            frame.output(Globals.lang(preamble)
                    + "  " + errorMessage);

            JOptionPane.showMessageDialog(frame, Globals.lang(preamble)
                + "\n" + errorMessage, Globals.lang("Export to SQL database"),
                JOptionPane.ERROR_MESSAGE);

            errorMessage = null;

        } */
    }

}
