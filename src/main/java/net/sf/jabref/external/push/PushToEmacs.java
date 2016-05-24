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
package net.sf.jabref.external.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA. User: alver Date: Jan 14, 2006 Time: 4:55:23 PM
 */
public class PushToEmacs extends AbstractPushToApplication implements PushToApplication {

    private static final Log LOGGER = LogFactory.getLog(PushToEmacs.class);

    private final JTextField additionalParams = new JTextField(30);
    private final JCheckBox useEmacs23 = new JCheckBox();


    @Override
    public String getApplicationName() {
        return "Emacs";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("emacs");
    }

    @Override
    public JPanel getSettingsPanel() {
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        useEmacs23.setSelected(Globals.prefs.getBoolean(JabRefPreferences.EMACS_23));
        return super.getSettingsPanel();
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
        Globals.prefs.putBoolean(JabRefPreferences.EMACS_23, useEmacs23.isSelected());
    }

    @Override
    protected void initSettingsPanel() {
        super.initSettingsPanel();
        builder.appendRows("2dlu, p, 2dlu, p");
        builder.add(Localization.lang("Additional parameters") + ":").xy(1, 3);
        builder.add(additionalParams).xy(3, 3);
        builder.add(Localization.lang("Use EMACS 23 insertion string") + ":").xy(1, 5);
        builder.add(useEmacs23).xy(3, 5);
        settings = builder.build();
    }

    @Override
    public void pushEntries(BibDatabase database, List<BibEntry> entries, String keys, MetaData metaData) {

        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        initParameters();
        commandPath = Globals.prefs.get(commandPathPreferenceKey);

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        commandPath = Globals.prefs.get(commandPathPreferenceKey);
        String[] addParams = Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS).split(" ");
        try {
            String[] com = new String[addParams.length + 2];
            com[0] = commandPath;
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
            prefix.concat("\\\"\\" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\\\"").concat(suffix) :
            // Linux gnuclient escaping:
            // java string: "(insert \"\\\\cite{Blah2001}\")"
            // so sh receives: (insert "\\cite{Blah2001}")
            // so emacs receives: (insert "\cite{Blah2001}")
            prefix.concat("\"" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\"").concat(suffix);

            final Process p = Runtime.getRuntime().exec(com);

            JabRefExecutorService.INSTANCE.executeAndWait(() -> {
                try (InputStream out = p.getErrorStream()) {
                    int c;
                    StringBuilder sb = new StringBuilder();
                    try {
                        while ((c = out.read()) != -1) {
                            sb.append((char) c);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Could not read from stderr.", e);
                    }
                    // Error stream has been closed. See if there were any errors:
                    if (!sb.toString().trim().isEmpty()) {
                        LOGGER.warn("Push to Emacs error: " + sb);
                        couldNotConnect = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("File problem.", e);
                }
            });
        } catch (IOException excep) {
            couldNotCall = true;
            LOGGER.warn("Problem pushing to Emacs.", excep);
        }
    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (couldNotConnect) {
            JOptionPane.showMessageDialog(panel.frame(), "<HTML>" +
                    Localization.lang("Could not connect to a running gnuserv process. Make sure that "
                            + "Emacs or XEmacs is running,<BR>and that the server has been started "
                            + "(by running the command 'server-start'/'gnuserv-start').") + "</HTML>",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotCall) {
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("Could not run the gnuclient/emacsclient program. Make sure you have "
                            + "the emacsclient/gnuclient program installed and available in the PATH."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            super.operationCompleted(panel);
        }
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.EMACS_PATH;
    }

    @Override
    protected String getCommandName() {
        return "gnuclient " + Localization.lang("or") + " emacsclient";
    }

}
