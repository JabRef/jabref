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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.push.PushToApplicationConstants;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToVim extends AbstractPushToApplication implements PushToApplication {

    public static final String NAME = PushToApplicationConstants.VIM;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToVim.class);

    public PushToVim(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_VIM;
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, ObjectProperty<PushToApplicationPreferences> preferences) {
        return new PushToVimSettings(application, dialogService, preferencesService.getFilePreferences(), preferences);
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

        try {
            String[] com = new String[]{commandPath, "--servername",
                    preferencesService.getPushToApplicationPreferences().getVimServer(), "--remote-send",
                    "<C-\\><C-N>a" + getCiteCommand() +
                            "{" + keys + "}"};

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
                        LOGGER.warn("Push to Vim error: " + sb);
                        couldNotConnect = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("File problem.", e);
                }
            });
        } catch (IOException excep) {
            couldNotCall = true;
            LOGGER.warn("Problem pushing to Vim.", excep);
        }
    }

    @Override
    public void operationCompleted() {
        if (couldNotConnect) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not connect to Vim server. Make sure that Vim is running with correct server name."));
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the 'vim' program."));
        } else {
            super.operationCompleted();
        }
    }
}
