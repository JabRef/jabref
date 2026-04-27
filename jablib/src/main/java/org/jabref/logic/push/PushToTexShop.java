package org.jabref.logic.push;

import java.io.IOException;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToTexShop extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.TEXSHOP;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToTexShop.class);

    public PushToTexShop(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    public void pushEntries(List<BibEntry> entries) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        String path = preferences.getCommandPaths().get(this.getDisplayName());
        commandPath = path != null ? path : "";

        try {
            String keyString = this.getKeyString(entries, getDelimiter());
            String citeString = getCitePrefix() + keyString + getCiteSuffix();
            LOGGER.debug("TexShop string: {}", citeString);

            if (!OS.OS_X) {
                sendErrorNotification(Localization.lang("Push to application"), Localization.lang("Pushing citations to TeXShop is only possible on macOS!"));
                return;
            }

            // We use osascript to send the citation to TeXShop.
            // Using separate arguments for ProcessBuilder avoids shell injection.
            String script = "tell application \"TeXShop\"\n" +
                    "activate\n" +
                    "set TheString to item 1 of arguments\n" +
                    "set content of selection of front document to TheString\n" +
                    "end tell";

            ProcessBuilder processBuilder = new ProcessBuilder("osascript", "-e", script, "--", citeString);
            Process process = processBuilder.start();

            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::info);
            StringBuilder errorStringBuilder = new StringBuilder();
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), s -> {
                LOGGER.warn("Push to TeXShop error: {}", s);
                errorStringBuilder.append(s).append("\n");
            });

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);

            process.waitFor();
            if (process.exitValue() != 0) {
                couldNotPush = true;
                String error = errorStringBuilder.toString().trim();
                if (!error.isEmpty()) {
                    sendErrorNotification(Localization.lang("Error pushing entries"),
                            Localization.lang("Could not push to a running TeXShop server.") + " " + error);
                }
            }
        } catch (IOException | InterruptedException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;

            if (excep instanceof IOException) {
                sendErrorNotification(Localization.lang("Error pushing entries"),
                        Localization.lang("Could not call executable '%0'.", "osascript") + "\n" +
                                Localization.lang("Please check the path in the preferences.") + "\n" +
                                (OS.OS_X ? Localization.lang("On macOS, you can use the command-line binary.") : ""));
            }
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            // Detailed error notification might have been sent already in pushEntries
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {};
    }
}
