/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.worker;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.help.NewVersionDialog;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VersionWorker extends SwingWorker<Version, Void> {

    private static final Log LOGGER = LogFactory.getLog(VersionWorker.class);

    private final JabRefFrame mainFrame;
    private final boolean manualExecution;
    private final Version installedVersion;
    private final Version toBeIgnored;

    public VersionWorker(JabRefFrame mainFrame, boolean manualExecution, Version installedVersion, Version toBeIgnored) {
        this.mainFrame = Objects.requireNonNull(mainFrame);
        this.manualExecution = manualExecution;
        this.installedVersion = Objects.requireNonNull(installedVersion);
        this.toBeIgnored = Objects.requireNonNull(toBeIgnored);
    }

    @Override
    protected Version doInBackground() throws Exception {
        try {
            return Version.getLatestVersion();
        } catch (IOException ioException) {
            LOGGER.warn("Couldn't connect to the updateserver.", ioException);
            return null;
        }
    }

    @Override
    public void done(){
        if (this.isCancelled()){
            return;
        }

        try {
            Version latestVersion = this.get();

            if (latestVersion == null){
                String couldNotConnect = Localization.lang("Couldn't connect to the update server.");
                String tryLater = Localization.lang("Please try again later and/or check your network connection.");
                if (manualExecution) {
                    JOptionPane.showMessageDialog(this.mainFrame, couldNotConnect + "\n" + tryLater,
                            couldNotConnect, JOptionPane.ERROR_MESSAGE);
                }
                this.mainFrame.output(couldNotConnect + " " + tryLater);
                return;
            }

            // only respect the ignored version on automated version checks
            if (latestVersion.equals(toBeIgnored) && !manualExecution) {
                return;
            }

            boolean newer = latestVersion.isNewerThan(installedVersion);
            if (newer){
                new NewVersionDialog(this.mainFrame, installedVersion, latestVersion, toBeIgnored);
                return;
            }

            String upToDate = Localization.lang("JabRef is up-to-date.");
            if (manualExecution) {
                JOptionPane.showMessageDialog(this.mainFrame, upToDate, upToDate, JOptionPane.INFORMATION_MESSAGE);
            }
            this.mainFrame.output(upToDate);

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while checking for updates", e);
        }
    }

}
