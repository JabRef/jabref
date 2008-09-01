package net.sf.jabref.sql;

import net.sf.jabref.BaseAction;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Util;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.sql.Connection;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
* User: alver
* Date: Mar 27, 2008
* Time: 6:05:13 PM
* To change this template use File | Settings | File Templates.
*/
public class DbConnectAction extends BaseAction {
    private BasePanel panel;

    public DbConnectAction(BasePanel panel) {
        this.panel = panel;
    }

    public AbstractAction getAction() {
        return new DbImpAction();
    }

    class DbImpAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            action();

        }
    }

    public void action() {

        DBStrings dbs = panel.metaData().getDBStrings();

        // init DB strings if necessary
        if (! dbs.isInitialized()) {
            dbs.initialize();
        }

        // show connection dialog
        DBConnectDialog dbd = new DBConnectDialog(panel.frame(), dbs);
        Util.placeDialog(dbd, panel);
        dbd.setVisible(true);

        // connnect to database to test DBStrings
        if (dbd.getConnectToDB()) {

            dbs = dbd.getDBStrings();

            try {

                panel.frame().output(Globals.lang("Establishing SQL connection..."));
                Connection conn = SQLutil.connectToDB(dbs);
                conn.close();
                dbs.isConfigValid(true);
                panel.frame().output(Globals.lang("SQL connection established."));

            } catch (Exception ex) {

                String errorMessage = SQLutil.getExceptionMessage(ex,SQLutil.DBTYPE.MYSQL);
                dbs.isConfigValid(false);

                String preamble = "Could not connect to SQL database for the following reason:";
                panel.frame().output(Globals.lang(preamble)
                        + "  " +  errorMessage);

                JOptionPane.showMessageDialog(panel.frame(), Globals.lang(preamble)
                    + "\n" + errorMessage, Globals.lang("Connect to SQL database"),
                    JOptionPane.ERROR_MESSAGE);

            } finally {

                panel.metaData().setDBStrings(dbs);
                dbd.dispose();

            }

        }

    }

}
