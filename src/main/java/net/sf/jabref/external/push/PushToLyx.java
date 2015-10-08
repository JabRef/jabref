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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

public class PushToLyx extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "LyX/Kile";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("lyx");
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.LYXPIPE;
    }

    @Override
    protected String getCouldNotCall() {
        // @formatter:off
        return Localization.lang("Error") + ": " +
                Localization.lang("unable to write to") + " " + commandPath +
                ".in";
        // @formatter:on
    }

    @Override
    protected String getCouldNotConnect() {
        // @formatter:off
        return Localization.lang("Error") + ": " +
                Localization.lang("verify that LyX is running and that the lyxpipe is valid")
                + ". [" + commandPath + "]";
        // @formatter:on

    }

    @Override
    protected void initSettingsPanel() {
        settings = new JPanel();
        settings.add(new JLabel(Localization.lang("Path to LyX pipe") + ":"));
        settings.add(Path);
    }

    @Override
    public void pushEntries(BibtexDatabase database, final BibtexEntry[] entries, final String keyString, MetaData metaData) {

        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        initParameters();
        commandPath = Globals.prefs.get(commandPathPreferenceKey);

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        if (!commandPath.endsWith(".in")) {
            commandPath = commandPath + ".in";
        }
        File lp = new File(commandPath); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
        if (!lp.exists() || !lp.canWrite()) {
            // See if it helps to append ".in":
            lp = new File(commandPath + ".in");
            if (!lp.exists() || !lp.canWrite()) {
                couldNotConnect = true;
                return;
            }
        }

        final File lyxpipe = lp;

        JabRefExecutorService.INSTANCE.executeAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    FileWriter fw = new FileWriter(lyxpipe);
                    BufferedWriter lyx_out = new BufferedWriter(fw);
                    String citeStr;

                    citeStr = "LYXCMD:sampleclient:citation-insert:" + keyString;
                    lyx_out.write(citeStr + "\n");

                    lyx_out.close();

                } catch (IOException excep) {
                    couldNotCall = true;
                }
            }
        });
    }

    /*class Timeout extends javax.swing.Timer
    {
      public Timeout(int timeout, final Thread toStop, final String message) {
        super(timeout, new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            toStop.stop();         // !!! <- deprecated
            // toStop.interrupt(); // better ?, interrupts wait and IO
            //stop();
            //output(message);
          }
        });
      }
    } */

}
