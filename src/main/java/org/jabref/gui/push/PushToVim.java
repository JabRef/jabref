package org.jabref.gui.push;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToVim extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.VIM;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToVim.class);

    public PushToVim(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_VIM;
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToVimSettings(application, dialogService, preferencesService.getFilePreferences(), preferences);
    }

    @Override
    public void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keys) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferencesService.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        try {
            String[] com = new String[]{commandPath, "--servername",
                    preferencesService.getPushToApplicationPreferences().getVimServer(), "--remote-send",
                    "<C-\\><C-N>a" + getCitePrefix() + keys + getCiteSuffix()};

            LOGGER.atDebug()
                  .setMessage("Executing command {}")
                  .addArgument(() -> Arrays.toString(com))
                  .log();

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
                        LOGGER.warn("Push to Vim error: {}", sb);
                        couldNotPush = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error handling std streams", e);
                }
            });
        } catch (IOException excep) {
            LOGGER.warn("Problem pushing to Vim.", excep);
            couldNotCall = true;
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not push to a running Vim server."));
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the 'vim' program."));
        } else {
            super.onOperationCompleted();
        }
    }
}
