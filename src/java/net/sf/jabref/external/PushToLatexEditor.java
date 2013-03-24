/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external;

import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Class for pushing entries into LatexEditor.
 */
public class PushToLatexEditor implements PushToApplication {

    private boolean couldNotCall=false;
    private boolean notDefined=false;
    private JPanel settings = null;
    private JTextField ledPath = new JTextField(30),
        citeCommand = new JTextField(30);

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
            StringBuffer toSend = new StringBuffer("-i ")
                    .append(Globals.prefs.get("citeCommandLed")).append("{")
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
            Globals.lang("Pushed citations to %0", "LatexEditor");
    }

    public boolean requiresBibtexKeys() {
        return true;
    }

    public JPanel getSettingsPanel() {
        if (settings == null)
            initSettingsPanel();
        ledPath.setText(Globals.prefs.get("latexEditorPath"));
        citeCommand.setText(Globals.prefs.get("citeCommandLed"));
        return settings;
    }

    private void initSettingsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref, 4dlu, fill:pref", ""));
        builder.append(new JLabel(Globals.lang("Path to LatexEditor (LEd.exe)") + ":"));
        builder.append(ledPath);
        BrowseAction action = new BrowseAction(null, ledPath, false);
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(action);
        builder.append(browse);
        builder.nextLine();
        builder.append(Globals.lang("Cite command") + ":");
        builder.append(citeCommand);
        settings = builder.getPanel();
    }

    public void storeSettings() {
        Globals.prefs.put("latexEditorPath", ledPath.getText());
        Globals.prefs.put("citeCommandLed", citeCommand.getText());
    }
}
