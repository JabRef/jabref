package net.sf.jabref.external;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;
import java.io.InputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 7, 2007
 * Time: 6:55:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToVim implements PushToApplication {

    private boolean couldNotConnect=false, couldNotRunVim =false;

    public String getName() {
        return Globals.menuTitle("Insert selected citations into Vim") ;
    }

    public String getApplicationName() {
        return "Vim";
    }

    public String getTooltip() {
        return Globals.lang("Push selection to Vim");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("vim");
    }

    public String getKeyStrokeName() {
        return null;
    }

    public void pushEntries(BibtexEntry[] entries, String keys) {
        couldNotConnect = false;
        couldNotRunVim = false;
        try {
            String[] com = Globals.ON_WIN ?
                    // Windows escaping:
                    // java string: "\\\\cite{Blah2001}"
                    // so cmd receives: "\\cite{Blah2001}"
                    // so vim receives: "\cite{Blah2001}"
                    new String[]{"vim", "--remote-send",
                            "\\\\" + Globals.prefs.get("citeCommand") +
                                    "{" + keys + "}"}
                    :
                    // Linux escaping:
                    // java string: "\\cite{Blah2001}"
                    // so sh receives: "\cite{Blah2001}"
                    // so vim receives: "\cite{Blah2001}"
                    new String[]{"vim", "--remote-send",
                            "\\" + Globals.prefs.get("citeCommand") +
                                    "{" + keys + "}"};

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
                        System.out.println(sb.toString());
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
            couldNotRunVim = true;
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect)
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "<HTML>" +
                            Globals.lang("Could not send to Vim. Make sure that Vim is running with"
                                +"the server option enabled.")+"<BR>"
                            +Globals.lang("JabRef connects to the server "
                                +"'vim', which is enabled by starting Vim with the option "
                                +"'--servername vim'.")+"<BR>"
                            +Globals.lang("Also make sure that Vim is in Insert mode.")
                            + "</HTML>",
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else if (couldNotRunVim)
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    Globals.lang("Could not run Vim. Make sure you have Vim installed and "
                            + "the 'vim' command available on your path."),
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        else {
            panel.output(Globals.lang("Pushed citations to Vim"));
        }
    }

    public boolean requiresBibtexKeys() {
        return true;
    }
}
