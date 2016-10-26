package net.sf.jabref.gui.worker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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


/**
 * This worker checks if there is a new version of JabRef available.
 * If there is it will display a Dialog to the User offering him multiple Options to proceed
 * (see changelog, go to the download page, ignore this version, and remind later).
 *
 * If the versions check is executed manually and this is the latest version it will also display a dialog to inform the user.
 */
public class VersionWorker extends SwingWorker<List<Version>, Void> {

    private static final Log LOGGER = LogFactory.getLog(VersionWorker.class);

    private final JabRefFrame mainFrame;

    /** If this versions check is executed automatically (eg. on startup) or manually by the user */
    private final boolean manualExecution;

    /** The current version of the installed JabRef */
    private final Version installedVersion;

    /** The version which was previously ignored by the user */
    private final Version toBeIgnored;


    public VersionWorker(JabRefFrame mainFrame, boolean manualExecution, Version installedVersion, Version toBeIgnored) {
        this.mainFrame = Objects.requireNonNull(mainFrame);
        this.manualExecution = manualExecution;
        this.installedVersion = Objects.requireNonNull(installedVersion);
        this.toBeIgnored = Objects.requireNonNull(toBeIgnored);
    }

    @Override
    protected List<Version> doInBackground() throws Exception {
        try {
            return Version.getAllAvailableVersions();
        } catch (IOException ioException) {
            LOGGER.warn("Could not connect to the updateserver.", ioException);
            return Collections.emptyList();
        }
    }

    @Override
    public void done(){
        if (this.isCancelled()){
            return;
        }

        try {
            List<Version> availableVersions = this.get();

            // couldn't find any version, connection problems?
            if (availableVersions.isEmpty()){
                String couldNotConnect = Localization.lang("Could not connect to the update server.");
                String tryLater = Localization.lang("Please try again later and/or check your network connection.");
                if (manualExecution) {
                    JOptionPane.showMessageDialog(this.mainFrame, couldNotConnect + "\n" + tryLater,
                            couldNotConnect, JOptionPane.ERROR_MESSAGE);
                }
                this.mainFrame.output(couldNotConnect + " " + tryLater);
                return;
            }

            // the newer version, excluding any alpha or beta builds, except if the installed one is one too
            Version newerVersion = null;
            for (Version version : availableVersions) {
                // ignoring any version which is not stable, except if the installed version is not stable itself
                if (installedVersion.getDevelopmentStage() == Version.DevelopmentStage.STABLE
                        && version.getDevelopmentStage() != Version.DevelopmentStage.STABLE) {
                    continue;
                }

                // check if this version is newer than the installed one and the last found "newer" version
                if (version.isNewerThan(installedVersion) && (newerVersion == null || version.isNewerThan(newerVersion))) {
                    newerVersion = version;
                }
            }

            // no new version could be found, only respect the ignored version on automated version checks
            if (newerVersion == null || (newerVersion.equals(toBeIgnored) && !manualExecution)) {
                String upToDate = Localization.lang("JabRef is up-to-date.");
                if (manualExecution) {
                    JOptionPane.showMessageDialog(this.mainFrame, upToDate, upToDate, JOptionPane.INFORMATION_MESSAGE);
                }
                this.mainFrame.output(upToDate);
                return;
            }

            // notify the user about a newer version
            new NewVersionDialog(this.mainFrame, installedVersion, newerVersion);

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while checking for updates", e);
        }
    }

}
