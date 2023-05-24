package org.jabref.gui.push;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

public class PushToLyx extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.LYX;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToLyx.class);

    public PushToLyx(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_LYX;
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotConnect) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("Verify that LyX is running and that the lyxpipe is valid.")
                            + "[" + commandPath + "]");
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("Unable to write to %0.", commandPath + ".in"));
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToLyxSettings(application, dialogService, preferencesService.getFilePreferences(), preferences);
    }

    @Override
    public void pushEntries(BibDatabaseContext database, final List<BibEntry> entries, final String keyString) {
        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferencesService.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        if (!commandPath.endsWith(".in")) {
            commandPath = commandPath + ".in";
        }
        File lp = new File(commandPath); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
        if (!lp.exists() || !lp.canWrite()) {
            // See if it helps to append ".in":
            lp = new File(commandPath + ".in");
            if (!lp.exists() || !lp.canWrite()) {
                couldNotConnect = true;
                return;
            }
        }

        final File lyxpipe = lp;

        JabRefExecutorService.INSTANCE.executeAndWait(() -> {
            try (FileWriter fw = new FileWriter(lyxpipe, StandardCharsets.UTF_8); BufferedWriter lyxOut = new BufferedWriter(fw)) {
                String citeStr = "LYXCMD:sampleclient:citation-insert:" + keyString;
                lyxOut.write(citeStr + "\n");
            } catch (IOException excep) {
                couldNotCall = true;
                LOGGER.warn("Problem pushing to LyX/Kile.", excep);
            }
        });
    }
}
