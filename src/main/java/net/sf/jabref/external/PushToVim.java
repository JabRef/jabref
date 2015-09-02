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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 7, 2007
 * Time: 6:55:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class PushToVim implements PushToApplication {

    private JPanel settings = null;
    private final JTextField vimPath = new JTextField(30);
    private final JTextField vimServer = new JTextField(30);
    private final JTextField citeCommand = new JTextField(30);

    private boolean couldNotConnect = false, couldNotRunClient = false;


    @Override
    public String getName() {
        return Globals.lang("Insert selected citations into Vim");
    }

    @Override
    public String getApplicationName() {
        return "Vim";
    }

    @Override
    public String getTooltip() {
        return Globals.lang("Push selection to Vim");
    }

    @Override
    public Icon getIcon() {
        return GUIGlobals.getImage("vim");
    }

    @Override
    public String getKeyStrokeName() {
        return null;
    }

    @Override
    public JPanel getSettingsPanel() {
        if (settings == null) {
            initSettingsPanel();
        }
        vimPath.setText(Globals.prefs.get(JabRefPreferences.VIM));
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
        citeCommand.setText(Globals.prefs.get(JabRefPreferences.CITE_COMMAND_VIM));
        return settings;
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put(JabRefPreferences.VIM, vimPath.getText());
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
        Globals.prefs.put(JabRefPreferences.CITE_COMMAND_VIM, citeCommand.getText());
    }

    private void initSettingsPanel() {
        DefaultFormBuilder builder = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref, 4dlu, fill:pref", ""));

        builder.append(new JLabel(Globals.lang("Path to Vim") + ":"));
        builder.append(vimPath);
        BrowseAction action = BrowseAction.buildForFile(vimPath);
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(action);
        builder.append(browse);
        builder.nextLine();
        builder.append(Globals.lang("Vim Server Name") + ":");
        builder.append(vimServer);
        builder.nextLine();
        builder.append(Globals.lang("Cite command") + ":");
        builder.append(citeCommand);
        settings = builder.getPanel();
    }

    @Override
    public void pushEntries(BibtexDatabase database, BibtexEntry[] entries, String keys,
            MetaData metaData) {

        couldNotConnect = false;
        couldNotRunClient = false;
        try {
            String[] com = new String[] {Globals.prefs.get(JabRefPreferences.VIM), "--servername", Globals.prefs.get(JabRefPreferences.VIM_SERVER), "--remote-send",
                    "<C-\\><C-N>a" + Globals.prefs.get(JabRefPreferences.CITE_COMMAND_VIM) +
                            "{" + keys + "}"};

            final Process p = Runtime.getRuntime().exec(com);

            Runnable errorListener = new Runnable() {

                @Override
                public void run() {
                    InputStream out = p.getErrorStream();
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
                    if (sb.toString().trim().length() > 0) {
                        System.out.println(sb.toString());
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
                            Globals.lang("Could not connect to Vim server. Make sure that "
                                    + "Vim is running<BR>with correct server name.")
                            + "</HTML>",
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else if (couldNotRunClient) {
            JOptionPane.showMessageDialog(
                    panel.frame(),
                    Globals.lang("Could not run the 'vim' program."),
                    Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
        } else {
            panel.output(Globals.lang("Pushed citations to Vim"));
        }
    }

    @Override
    public boolean requiresBibtexKeys() {
        return true;
    }
}
