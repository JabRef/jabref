package org.jabref.gui.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javafx.beans.property.ObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.push.PushToApplicationConstants;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToEmacs extends AbstractPushToApplication implements PushToApplication {

    public static final String NAME = PushToApplicationConstants.EMACS;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToEmacs.class);

    public PushToEmacs(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_EMACS;
    }

    @Override
    public void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keys) {
        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferencesService.getPushToApplicationPreferences().getPushToApplicationCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        commandPath = preferencesService.getPushToApplicationPreferences().getPushToApplicationCommandPaths().get(this.getDisplayName());

        String[] addParams = preferencesService.getPushToApplicationPreferences().getEmacsArguments().split(" ");
        try {
            String[] com = new String[addParams.length + 2];
            com[0] = commandPath;
            System.arraycopy(addParams, 0, com, 1, addParams.length);
            String prefix;
            String suffix;
            prefix = "(with-current-buffer (window-buffer) (insert ";
            suffix = "))";

            if (OS.WINDOWS) {
                // Windows gnuclient/emacsclient escaping:
                // java string: "(insert \\\"\\\\cite{Blah2001}\\\")";
                // so cmd receives: (insert \"\\cite{Blah2001}\")
                // so emacs receives: (insert "\cite{Blah2001}")
                com[com.length - 1] = prefix.concat("\\\"\\" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\\\"").concat(suffix);
            } else {
                // Linux gnuclient/emacslient escaping:
                // java string: "(insert \"\\\\cite{Blah2001}\")"
                // so sh receives: (insert "\\cite{Blah2001}")
                // so emacs receives: (insert "\cite{Blah2001}")
                com[com.length - 1] = prefix.concat("\"" + getCiteCommand().replaceAll("\\\\", "\\\\\\\\") + "{" + keys + "}\"").concat(suffix);
            }

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
    public void operationCompleted() {
        if (couldNotConnect) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not connect to a running gnuserv process. Make sure that "
                            + "Emacs or XEmacs is running, and that the server has been started "
                            + "(by running the command 'server-start'/'gnuserv-start')."));
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the gnuclient/emacsclient program. Make sure you have "
                            + "the emacsclient/gnuclient program installed and available in the PATH."));
        } else {
            super.operationCompleted();
        }
    }

    @Override
    protected String getCommandName() {
        return "gnuclient " + Localization.lang("or") + " emacsclient";
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, ObjectProperty<PushToApplicationPreferences> preferences) {
        return new PushToEmacsSettings(application, dialogService, preferencesService.getFilePreferences(), preferences);
    }
}
