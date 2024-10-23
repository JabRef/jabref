package org.jabref.gui.push;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToEmacs extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.EMACS;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToEmacs.class);

    /**
     * @param preferences getPushToApplicationPreferences(), getExternalApplicationsPreferences(), and getFilePreferences() are used
     */
    public PushToEmacs(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_EMACS;
    }

    @Override
    public void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keys) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        PushToApplicationPreferences pushToApplicationPreferences = preferences.getPushToApplicationPreferences();

        commandPath = pushToApplicationPreferences.getCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        commandPath = pushToApplicationPreferences.getCommandPaths().get(this.getDisplayName());

        String[] addParams = pushToApplicationPreferences.getEmacsArguments().split(" ");
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
                                                    + keys
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
                                                    + keys
                                                    + getCiteSuffix().replace("\\", "\\\\")
                                                    + "\"").concat(suffix);
            }

            LOGGER.atDebug()
                  .setMessage("Executing command {}")
                  .addArgument(() -> Arrays.toString(com))
                  .log();

            final Process p = Runtime.getRuntime().exec(com);

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
                    if (!sb.toString().trim().isEmpty()) {
                        LOGGER.warn("Push to Emacs error: {}", sb);
                        couldNotPush = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error handling std streams", e);
                }
            });
        } catch (IOException excep) {
            LOGGER.warn("Problem pushing to Emacs.", excep);
            couldNotCall = true;
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not push to a running emacs daemon."));
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the emacs client."));
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    protected String getCommandName() {
        return "gnuclient " + Localization.lang("or") + " emacsclient";
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToEmacsSettings(application, dialogService, this.preferences.getFilePreferences(), preferences);
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "+%s".formatted(line), fileName.toString()};
    }
}
