/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.io.InputStream;

import javax.swing.*;

import net.sf.jabref.*;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Jan 14, 2006
 * Time: 4:55:23 PM
 */
public class PushToEmacs implements PushToApplication {

    private JPanel settings;
    private final JTextField citeCommand = new JTextField(30);
    private final JTextField emacsPath = new JTextField(30);
    private final JTextField additionalParams = new JTextField(30);
    private final JCheckBox useEmacs23 = new JCheckBox();

    private boolean couldNotConnect;
    private boolean couldNotRunClient;


    @Override
    public String getName() {
        return Localization.lang("Insert selected citations into %0" ,getApplicationName());
    }

    @Override
    public String getApplicationName() {
        return "Emacs";
    }

    @Override
    public String getTooltip() {
        return Localization.lang("Push to %0", getApplicationName());
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("emacs");
    }

    @Override
    public String getKeyStrokeName() {
        return "Push to Emacs";
    }

    @Override
    public JPanel getSettingsPanel() {
        if (settings == null) {
            initSettingsPanel();
        }
        citeCommand.setText(Globals.prefs.get(JabRefPreferences.CITE_COMMAND_EMACS));
        emacsPath.setText(Globals.prefs.get(JabRefPreferences.EMACS_PATH));
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        useEmacs23.setSelected(Globals.prefs.getBoolean(JabRefPreferences.EMACS_23));
        return settings;
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put(JabRefPreferences.CITE_COMMAND_EMACS, citeCommand.getText());
        Globals.prefs.put(JabRefPreferences.EMACS_PATH, emacsPath.getText());
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
        Globals.prefs.putBoolean(JabRefPreferences.EMACS_23, useEmacs23.isSelected());
    }

    private void initSettingsPanel() {
        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p, 2dlu, p, 2dlu, p, 2dlu, p"));
        builder.add(Localization.lang("Path to gnuclient or emacsclient") + ":").xy(1, 1);
        builder.add(emacsPath).xy(3,1);
        BrowseAction action = BrowseAction.buildForFile(emacsPath);
        JButton browse = new JButton(Localization.lang("Browse"));
        browse.addActionListener(action);
        builder.add(browse).xy(5,1);
        builder.add(Localization.lang("Additional parameters") + ":").xy(1, 3);
        builder.add(additionalParams).xy(3,3);
        builder.add(Localization.lang("Use EMACS 23 insertion string") + ":").xy(1, 5);
        builder.add(useEmacs23).xy(3,5);
        builder.add(Localization.lang("Cite command") + ":").xy(1, 7);
        builder.add(citeCommand).xy(3,7);
        settings = builder.build();
    }

    @Override
    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keys, MetaData metaData) {

        couldNotConnect = false;
        couldNotRunClient = false;
        String command = Globals.prefs.get(JabRefPreferences.EMACS_PATH);
        String[] addParams = Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS).split(" ");
        try {
            String[] com = new String[addParams.length + 2];
            com[0] = command;
            System.arraycopy(addParams, 0, com, 1, addParams.length);
            String prefix;
            String suffix;
            if (Globals.prefs.getBoolean(JabRefPreferences.EMACS_23)) {
                prefix = "(with-current-buffer (window-buffer) (insert ";
                suffix = "))";
            } else {
                prefix = "(insert ";
                suffix = ")";
            }

            com[com.length - 1] = OS.WINDOWS ?
                    // Windows gnuclient escaping:
                    // java string: "(insert \\\"\\\\cite{Blah2001}\\\")";
                    // so cmd receives: (insert \"\\cite{Blah2001}\")
                    // so emacs receives: (insert "\cite{Blah2001}")
                    prefix.concat("\\\"\\" + Globals.prefs.get(JabRefPreferences.CITE_COMMAND_EMACS).replaceAll("\\\\", "\\\\\\\\") +
                            "{" + keys + "}\\\"").concat(suffix)
                    :
                    // Linux gnuclient escaping:
                    // java string: "(insert \"\\\\cite{Blah2001}\")"
                    // so sh receives: (insert "\\cite{Blah2001}")
                    // so emacs receives: (insert "\cite{Blah2001}")
                    prefix.concat("\"" + Globals.prefs.get(JabRefPreferences.CITE_COMMAND_EMACS).replaceAll("\\\\", "\\\\\\\\") +
                            "{" + keys + "}\"").concat(suffix);

            final Process p = Runtime.getRuntime().exec(com);

            Runnable errorListener = new Runnable() {

                @Override
                public void run() {
                    InputStream out = p.getErrorStream();
                    //                    try {
                    //                    	if (out.available() <= 0)
                    //                    		out = p.getInputStream();
                    //                    } catch (Exception e) {
                    //                    }
                    int c;
                    StringBuilder sb = new StringBuilder();
                    try {
                        while ((c = out.read()) != -1) {
                            sb.append((char) c);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (!sb.toString().trim().isEmpty()) {
                        System.out.println(sb);
                        couldNotConnect = true;
                    }
                }
            };
            JabRefExecutorService.INSTANCE.executeAndWait(errorListener);
        } catch (IOException excep) {
            couldNotRunClient = true;
        }

    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    "<HTML>" +
                            Localization.lang("Could not connect to a running gnuserv process. Make sure that "
                                    + "Emacs or XEmacs is running,<BR>and that the server has been started "
                                    + "(by running the command 'server-start'/'gnuserv-start').")
                            + "</HTML>",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotRunClient) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    Localization.lang("Could not run the gnuclient/emacsclient program. Make sure you have "
                            + "the emacsclient/gnuclient program installed and available in the PATH."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            panel.output(Localization.lang("Pushed citations to %0", getApplicationName()));
        }
    }

    @Override
    public boolean requiresBibtexKeys() {
        return true;
    }
}
