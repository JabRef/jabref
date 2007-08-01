package net.sf.jabref.external;

import java.io.IOException;

import javax.swing.Icon;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;

public class PushToWinEdt implements PushToApplication {

    private boolean couldNotCall=false;
    private boolean notDefined=false;

    public String getName() {
        return Globals.lang("Insert selected citations into WinEdt");
    }

    public String getApplicationName() {
        return "WinEdt";
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
        notDefined = false;

        String winEdt = Globals.prefs.get("winEdtPath");
        if ((winEdt == null) || (winEdt.trim().length() == 0)) {
            notDefined = true;
            return;
        }

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
        if (notDefined) {
            panel.output(Globals.lang("Error") + ": "+
                    Globals.lang("Path to %0 not defined", getApplicationName())+".");
        }
        else if (couldNotCall) {
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
