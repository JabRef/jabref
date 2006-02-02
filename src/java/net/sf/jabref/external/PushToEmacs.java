package net.sf.jabref.external;

import net.sf.jabref.BaseAction;
import net.sf.jabref.Globals;
import net.sf.jabref.BasePanel;

import javax.swing.*;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Jan 14, 2006
 * Time: 4:55:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToEmacs extends BaseAction {

    private BasePanel panel;

    public PushToEmacs(BasePanel panel) {
        this.panel = panel;
    }

    public void action() {

        int numSelected = panel.mainTable.getSelectedRowCount();

        if (numSelected > 0) {
            String keys = panel.getKeysForSelection();
            StringBuffer command = new StringBuffer("(insert\"\\\\")
                    .append(Globals.prefs.get("citeCommand")).append("{");
            if (keys.length() == 0)
                panel.output(Globals.lang("Please define BibTeX key first"));
            else {
                try {
                    command.append(keys);
                    command.append("}\")");
                    String[] com = new String[]{"gnuclient", "-batch", "-eval",
                        command.toString()};
                    final Process p = Runtime.getRuntime().exec(com);

                    Runnable errorListener = new Runnable() {
                        public void run() {
                            InputStream out = p.getErrorStream();
                            int c;
                            StringBuffer sb = new StringBuffer();
                            try {
                                while ((c = out.read()) != -1)
                                    sb.append((char) c);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // Error stream has been closed. See if there were any errors:
                            if (sb.toString().trim().length() > 0) {
                                JOptionPane.showMessageDialog(
                                        panel.frame(),
                                        "<HTML>"+
                                        Globals.lang("Could not connect to a running gnuserv process. Make sure that "
                                        +"Emacs or XEmacs is running,<BR>and that the server has been started "
                                        +"(by running the command 'gnuserv-start').")
                                        +"</HTML>",
                                        Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
                            }
                            else {
                                panel.output(Globals.lang("Pushed citations to Emacs"));
                            }
                        }
                    };
                    (new Thread(errorListener)).start();

                }
                catch (IOException excep) {
                    JOptionPane.showMessageDialog(
                        panel.frame(),
                        Globals.lang("Could not run the 'gnuclient' program. Make sure you have "
                        +"the gnuserv/gnuclient programs installed."),
                        Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}

