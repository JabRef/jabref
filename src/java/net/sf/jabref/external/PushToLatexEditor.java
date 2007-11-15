package net.sf.jabref.external;

import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 4, 2006
 * Time: 10:14:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToLatexEditor implements PushToApplication {

    private boolean couldNotCall=false;
    private boolean notDefined=false;

    public String getName() {
        return Globals.menuTitle("Insert selected citations into LatexEditor");
    }

    public String getApplicationName() {
        return "LatexEditor";
    }

    public String getTooltip() {
        return Globals.lang("Push to LatexEditor");
    }

    public Icon getIcon() {
        return GUIGlobals.getImage("edit");
    }

    public String getKeyStrokeName() {
        return null;
    }

    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keyString, MetaData metaData) {

        couldNotCall = false;
        notDefined = false;

        String led = Globals.prefs.get("latexEditorPath");

        if ((led == null) || (led.trim().length() == 0)) {
            notDefined = true;
            return;
        }

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
        if (notDefined) {
            panel.output(Globals.lang("Error") + ": "+
                    Globals.lang("Path to %0 not defined", getApplicationName())+".");
        }
        else if (couldNotCall) {
            panel.output(Globals.lang("Error") + ": " + Globals.lang("Could not call executable") + " '"
                    +Globals.prefs.get("latexEditorPath") + "'.");
        }
        else
            Globals.lang("Pushed citations to WinEdt");
    }

    public boolean requiresBibtexKeys() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public JPanel getSettingsPanel() {
        return null;
    }

    public void storeSettings() {
        
    }
}
