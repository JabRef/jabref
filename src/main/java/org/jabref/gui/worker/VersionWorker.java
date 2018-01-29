package org.jabref.gui.worker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.help.NewVersionDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This worker checks if there is a new version of JabRef available.
 * If there is it will display a Dialog to the User offering him multiple Options to proceed
 * (see changelog, go to the download page, ignore this version, and remind later).
 *
 * If the versions check is executed manually and this is the latest version it will also display a dialog to inform the user.
 */
public class VersionWorker extends SwingWorker<List<Version>, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionWorker.class);

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
    public void done() {
        if (this.isCancelled()) {
            return;
        }

        try {
            List<Version> availableVersions = this.get();

            // couldn't find any version, connection problems?
            if (availableVersions.isEmpty()) {
                showConnectionError();
            } else {
                showUpdateInfo(availableVersions);
            }

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while checking for updates", e);
        }
    }

    /**
     * prints the connection problem to the status bar and shows a dialog if it was executed manually
     */
    private void showConnectionError() {
        String couldNotConnect = Localization.lang("Could not connect to the update server.");
        String tryLater = Localization.lang("Please try again later and/or check your network connection.");
        if (manualExecution) {
            JOptionPane.showMessageDialog(this.mainFrame, couldNotConnect + "\n" + tryLater,
                    couldNotConnect, JOptionPane.ERROR_MESSAGE);
        }
        this.mainFrame.output(couldNotConnect + " " + tryLater);
    }

    /**
     * Prints up-to-date to the status bar (and shows a dialog it was executed manually) if there is now new version.
     * Shows a "New Version" Dialog to the user if there is.
     */
    private void showUpdateInfo(List<Version> availableVersions) {
        // the newer version, excluding any non-stable versions, except if the installed one is unstable too
        Optional<Version> newerVersion = installedVersion.shouldBeUpdatedTo(availableVersions);

        // no new version could be found, only respect the ignored version on automated version checks
        if (!newerVersion.isPresent() || (newerVersion.get().equals(toBeIgnored) && !manualExecution)) {
            String upToDate = Localization.lang("JabRef is up-to-date.");
            if (manualExecution) {
                JOptionPane.showMessageDialog(this.mainFrame, upToDate, upToDate, JOptionPane.INFORMATION_MESSAGE);
            }
            this.mainFrame.output(upToDate);

        } else {
            // notify the user about a newer version
            new NewVersionDialog(this.mainFrame, installedVersion, newerVersion.get());
        }
    }

}
