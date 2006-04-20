package net.sf.jabref.external;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 4, 2006
 * Time: 10:14:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToLatexEditor implements PushToApplication {

    private boolean couldNotCall=false;

    public String getName() {
        return Globals.menuTitle("Insert selected citations into LatexEditor");
    }

    public String getTooltip() {
        return Globals.lang("Push to LatexEditor");
    }

    public Icon getIcon() {
        return null;
    }

    public String getKeyStrokeName() {
        return null;
    }

    public void pushEntries(BibtexEntry[] entries, String keyString) {

        couldNotCall = false;

        String led = Globals.prefs.get("latexEditorPath");

        try {
            StringBuffer toSend = new StringBuffer("-i \\")
                    .append(Globals.prefs.get("citeCommand")).append("{")
                    .append(keyString)
                    .append("}");
            Runtime.getRuntime().exec(led + " " + toSend.toString());

        }

        catch (IOException excep) {
            couldNotCall = true;
            excep.printStackTrace();
        }
    }

    public void operationCompleted(BasePanel panel) {
        if (couldNotCall) {
            panel.output(Globals.lang("Error") + ": " + Globals.lang("Could not call executable") + " '"
                    +Globals.prefs.get("latexEditorPath") + "'.");
        }
        else
            Globals.lang("Pushed citations to WinEdt");
    }

    public boolean requiresBibtexKeys() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
