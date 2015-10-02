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
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

public class PushToWinEdt implements PushToApplication {

    private boolean couldNotCall;
    private boolean notDefined;
    private JPanel settings;
    private final JTextField winEdtPath = new JTextField(30);
    private final JTextField citeCommand = new JTextField(30);


    @Override
    public String getName() {
        return Localization.lang("Insert selected citations into WinEdt");
    }

    @Override
    public String getApplicationName() {
        return "WinEdt";
    }

    @Override
    public String getTooltip() {
        return Localization.lang("Push selection to WinEdt");
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("winedt");
    }

    @Override
    public String getKeyStrokeName() {
        return "Push to WinEdt";
    }

    @Override
    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keyString, MetaData metaData) {

        couldNotCall = false;
        notDefined = false;

        String winEdt = Globals.prefs.get(JabRefPreferences.WIN_EDT_PATH);
        if (winEdt == null || winEdt.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        try {
            Runtime.getRuntime().exec(new String[] {winEdt, "\"[InsText('" + Globals.prefs.get(JabRefPreferences.CITE_COMMAND_WIN_EDT) + "{" + keyString.replaceAll("'", "''") + "}');]\""});

        }

        catch (IOException excep) {
            couldNotCall = true;
            excep.printStackTrace();
        }

    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (notDefined) {
            panel.output(Localization.lang("Error") + ": " +
                    Localization.lang("Path to %0 not defined", getApplicationName()) + ".");
        }
        else if (couldNotCall) {
            panel.output(Localization.lang("Error") + ": " + Localization.lang("Could not call executable") + " '"
                    + Globals.prefs.get(JabRefPreferences.WIN_EDT_PATH) + "'.");
        } else {
            Localization.lang("Pushed citations to WinEdt");
        }
    }

    @Override
    public boolean requiresBibtexKeys() {
        return true;
    }

    @Override
    public JPanel getSettingsPanel() {
        if (settings == null) {
            initSettingsPanel();
        }
        winEdtPath.setText(Globals.prefs.get(JabRefPreferences.WIN_EDT_PATH));
        citeCommand.setText(Globals.prefs.get(JabRefPreferences.CITE_COMMAND_WIN_EDT));
        return settings;
    }

    private void initSettingsPanel() {
        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p, 2dlu, p"));
        builder.add(Localization.lang("Path to %0", getApplicationName()) + ":").xy(1, 1);
        builder.add(winEdtPath).xy(3,1);
        BrowseAction action = BrowseAction.buildForFile(winEdtPath);
        JButton browse = new JButton(Localization.lang("Browse"));
        browse.addActionListener(action);
        builder.add(browse).xy(5,1);
        builder.add(Localization.lang("Cite command") + ":").xy(1, 3);
        builder.add(citeCommand).xy(3,3);
        settings = builder.build();
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put(JabRefPreferences.WIN_EDT_PATH, winEdtPath.getText());
        Globals.prefs.put(JabRefPreferences.CITE_COMMAND_WIN_EDT, citeCommand.getText());
    }
}
