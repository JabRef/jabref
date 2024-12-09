package org.jabref.gui.push;

import java.io.IOException;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.StreamGobbler;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToTexShop extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXSHOP;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToTexShop.class);

    public PushToTexShop(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXSHOP;
    }

    @Override
    public void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keyString) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        try {
            LOGGER.debug("TexShop string: {}", String.join(" ", getCommandLine(keyString)));
            ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(keyString));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::info);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::info);

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
        } catch (IOException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;
        }
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        String citeCommand = getCitePrefix();
        // we need to escape the extra slashses
        int intSlashPosition = getCitePrefix().indexOf("\\");

        if (intSlashPosition != -1) {
            StringBuilder sb = new StringBuilder(getCitePrefix());
            sb.insert(intSlashPosition, "\\");
            citeCommand = sb.toString();
        }

        String osascriptTexShop = "osascript -e 'tell application \"TeXShop\"\n" +
                "activate\n" +
                "set TheString to \"" + citeCommand + keyString + getCiteSuffix() + "\"\n" +
                "set content of selection of front document to TheString\n" +
                "end tell'";

        if (OS.OS_X) {
            return new String[] {"sh", "-c", osascriptTexShop};
        } else {
            dialogService.showInformationDialogAndWait(Localization.lang("Push to application"), Localization.lang("Pushing citations to TeXShop is only possible on macOS!"));
            return new String[] {};
        }
    }
}
