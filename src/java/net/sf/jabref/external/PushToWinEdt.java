package net.sf.jabref.external;

import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PushToWinEdt implements PushToApplication {

    private boolean couldNotCall=false;
    private boolean notDefined=false;
    private JPanel settings = null;
    private JTextField winEdtPath = new JTextField(30),
        citeCommand = new JTextField(30);

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

    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keyString, MetaData metaData) {

        couldNotCall = false;
        notDefined = false;

        String winEdt = Globals.prefs.get("winEdtPath");
        if ((winEdt == null) || (winEdt.trim().length() == 0)) {
            notDefined = true;
            return;
        }

        try {
            StringBuffer toSend = new StringBuffer("\"[InsText('")
                    .append(Globals.prefs.get("citeCommandWinEdt")).append("{")
                    .append(keyString.replaceAll("'", "''"))
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

    public JPanel getSettingsPanel() {
        if (settings == null)
            initSettingsPanel();
        winEdtPath.setText(Globals.prefs.get("winEdtPath"));
        citeCommand.setText(Globals.prefs.get("citeCommandWinEdt"));
        return settings;
    }

    private void initSettingsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref, 4dlu, fill:pref", ""));
        builder.append(new JLabel(Globals.lang("Path to WinEdt.exe") + ":"));
        builder.append(winEdtPath);
        BrowseAction action = new BrowseAction(null, winEdtPath, false);
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(action);
        builder.append(browse);
        builder.nextLine();
        builder.append(Globals.lang("Cite command") + ":");
        builder.append(citeCommand);
        settings = builder.getPanel();
    }

    public void storeSettings() {
        Globals.prefs.put("winEdtPath", winEdtPath.getText());
        Globals.prefs.put("citeCommandWinEdt", citeCommand.getText());
    }
}
