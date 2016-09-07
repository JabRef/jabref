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
            LOGGER.warn("Could not connect to the updateserver.", ioException);
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
                String couldNotConnect = Localization.lang("Could not connect to the update server.");
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
