package net.sf.jabref.external;

import net.sf.jabref.*;

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
public class PushToEmacs implements PushToApplication {

    private boolean couldNotConnect=false, couldNotRunClient=false;

    public String getName() {
        return Globals.menuTitle("Insert selected citations into Emacs") ;
    }

    public String getApplicationName() {
        return "Emacs";
    }

    public String getTooltip() {
        return Globals.lang("Push selection to Emacs");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("emacs");
    }

    public String getKeyStrokeName() {
        return "Push to Emacs";
    }

    public void pushEntries(BibtexEntry[] entries, String keys) {

        couldNotConnect=false;
        couldNotRunClient=false;
        StringBuffer command = new StringBuffer("(insert\"\\\\")
                .append(Globals.prefs.get("citeCommand")).append("{");

        try {
            command.append(keys);
            command.append("}\")");
            String[] com = Globals.ON_WIN ?
		// Windows gnuclient:
		new String[] {"gnuclient", "-qe", "'"+command.toString()+"'"}
		:
		// Linux client:
		new String[]{"gnuclient", "-batch", "-eval",
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
                        couldNotConnect = true;
                        return;
                    }
                }
            };
            Thread t = new Thread(errorListener);
            t.start();
            t.join();
        }
        catch (IOException excep) {
            couldNotRunClient = true;
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect)
            JOptionPane.showMessageDialog(
                panel.frame(),
                "<HTML>"+
                Globals.lang("Could not connect to a running gnuserv process. Make sure that "
                +"Emacs or XEmacs is running,<BR>and that the server has been started "
                +"(by running the command 'gnuserv-start').")
                +"</HTML>",
                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else if (couldNotRunClient)
            JOptionPane.showMessageDialog(
                panel.frame(),
                Globals.lang("Could not run the 'gnuclient' program. Make sure you have "
                +"the gnuserv/gnuclient programs installed."),
                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else {
            panel.output(Globals.lang("Pushed citations to Emacs"));
        }
    }

    public boolean requiresBibtexKeys() {
        return true;
    }
}

