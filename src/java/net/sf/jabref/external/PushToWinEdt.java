package net.sf.jabref.external;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.util.List;
import java.util.Iterator;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 4, 2006
 * Time: 10:01:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToWinEdt implements PushToApplication {

    private boolean couldNotCall=false;

    public String getName() {
        return Globals.lang("Insert selected citations into WinEdt");
    }

    public String getTooltip() {
        return Globals.lang("Push selection to WinEdt");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("winedt");
    }

    public String getKeyStrokeName() {
        return "Push to WinEdt";
    }

    public void pushEntries(BibtexEntry[] entries, String keyString) {

        couldNotCall = false;

        String winEdt = Globals.prefs.get("winEdtPath");
        //winEdt = "osascript";
        try {
            StringBuffer toSend = new StringBuffer("\"[InsText('\\")
                    .append(Globals.prefs.get("citeCommand")).append("{")
                    .append(keyString)
                    .append("}');]\"");
            Runtime.getRuntime().exec(winEdt + " " + toSend.toString());

        }

        catch (IOException excep) {
            couldNotCall = true;
            excep.printStackTrace();
        }


    }

    public void operationCompleted(BasePanel panel) {
        if (couldNotCall) {
            panel.output(Globals.lang("Error") + ": " + Globals.lang("Could not call executable") + " '"
                    +Globals.prefs.get("winEdtPath") + "'.");
        }
        else
            Globals.lang("Pushed citations to WinEdt");
    }

    public boolean requiresBibtexKeys() {
        return true;
    }
}
