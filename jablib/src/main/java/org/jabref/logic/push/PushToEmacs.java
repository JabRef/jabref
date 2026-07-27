package org.jabref.logic.push;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToEmacs extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.EMACS;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToEmacs.class);

    /// @param preferences getPushToApplicationPreferences(), getExternalApplicationsPreferences(), and getFilePreferences() are used
    public PushToEmacs(NotificationService notificationService, PushToApplicationPreferences preferences) {
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
        commandPath = preferences.getCommandPaths().getOrDefault(this.getDisplayName(), "");

        if (StringUtil.isBlank(commandPath)) {
            notDefined = true;
            return;
        }

        String keyString = getKeyString(entries, getDelimiter());

        String[] addParams = preferences.getEmacsArguments().split(" ");
        try {
            String[] com = new String[addParams.length + 2];
            com[0] = commandPath;
            System.arraycopy(addParams, 0, com, 1, addParams.length);

            // Surrounding with is handled below
            String prefix = "(with-current-buffer (window-buffer (selected-window)) (insert ";
            String suffix = "))";

            if (OS.WINDOWS) {
                // Windows gnuclient/emacsclient escaping:
                // java string: "(insert \\\"\\\\cite{Blah2001}\\\")";
                // so cmd receives: (insert \"\\cite{Blah2001}\")
                // so emacs receives: (insert "\cite{Blah2001}")

                com[com.length - 1] = prefix.concat("\""
                                                    + getCitePrefix().replace("\\", "\\\\")
                                                    + keyString
                                                    + getCiteSuffix().replace("\\", "\\\\")
                                                    + "\"").concat(suffix)
                                            .replace("\"", "\\\"");
            } else {
                // Linux gnuclient/emacslient escaping:
                // java string: "(insert \"\\\\cite{Blah2001}\")"
                // so sh receives: (insert "\\cite{Blah2001}")
                // so emacs receives: (insert "\cite{Blah2001}")
                com[com.length - 1] = prefix.concat("\""
                        + getCitePrefix().replace("\\", "\\\\")
                        + keyString
                        + getCiteSuffix().replace("\\", "\\\\")
                        + "\"").concat(suffix);
            }

            LOGGER.atDebug()
                  .setMessage("Executing command {}")
                  .addArgument(() -> Arrays.toString(com))
                  .log();

            ProcessBuilder processBuilder = new ProcessBuilder(com);
            final Process p = processBuilder.start();

            HeadlessExecutorService.INSTANCE.executeAndWait(() -> {
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
                    String error = sb.toString().trim();
                    if (!error.isEmpty()) {
                        LOGGER.warn("Push to Emacs error: {}", error);
                        couldNotPush = true;
                        if (error.contains("can't find socket") || error.contains("No socket or alternate editor")) {
                            sendErrorNotification(Localization.lang("Error pushing entries"),
                                    Localization.lang("Could not connect to Emacs. Make sure the server is running. In Emacs, type %0 to start it.", "M-x server-start"));
                        } else {
                            sendErrorNotification(Localization.lang("Error pushing entries"),
                                    Localization.lang("Could not push to a running emacs daemon."));
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error handling std streams", e);
                }
            });
        } catch (IOException excep) {
            LOGGER.warn("Problem pushing to Emacs.", excep);
            couldNotCall = true;
            sendErrorNotification(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not call executable '%0'.", commandPath) + "\n" +
                            Localization.lang("Please check the path in the preferences.") + "\n" +
                            (OS.OS_X ? Localization.lang("On macOS, you can use the command-line binary.") : ""));
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            // Detailed error notification might have been sent already in pushEntries
            // But we don't want to swallow the success message if there's no error.
        } else if (couldNotCall) {
            this.sendErrorNotification(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the emacs client."));
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    public String getCommandName() {
        return "gnuclient " + Localization.lang("or") + " emacsclient";
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "+%s".formatted(line), fileName.toString()};
    }
}
