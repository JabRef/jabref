package org.jabref.gui.push;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

public class PushToLyx extends AbstractPushToApplication implements PushToApplication {

    public static final String NAME = PushToApplicationConstants.LYX;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToLyx.class);

    public PushToLyx(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_LYX;
    }

    @Override
    public void operationCompleted() {
        if (couldNotConnect) {
            dialogService.showErrorDialogAndWait(Localization.lang("Error pushing entries"),
                    Localization.lang("verify that LyX is running and that the lyxpipe is valid")
                            + ". [" + commandPath + "]");
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(Localization.lang("unable to write to") + " " + commandPath + ".in");
        } else {
            super.operationCompleted();
        }
    }

    @Override
    public PushToApplicationSettings getSettings(PushToApplication application, ObjectProperty<PushToApplicationPreferences> preferences) {
        return new PushToLyxSettings(application, dialogService, preferencesService, preferences);
    }

    @Override
    public void pushEntries(BibDatabaseContext database, final List<BibEntry> entries, final String keyString) {
        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferencesService.getPushToApplicationPreferences().getPushToApplicationCommandPaths().get(this.getDisplayName());

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
            try (FileWriter fw = new FileWriter(lyxpipe); BufferedWriter lyxOut = new BufferedWriter(fw)) {
                String citeStr = "LYXCMD:sampleclient:citation-insert:" + keyString;
                lyxOut.write(citeStr + "\n");
            } catch (IOException excep) {
                couldNotCall = true;
                LOGGER.warn("Problem pushing to LyX/Kile.", excep);
            }
        });
    }
}
