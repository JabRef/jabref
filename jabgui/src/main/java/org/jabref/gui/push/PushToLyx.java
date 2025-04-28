package org.jabref.gui.push;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToLyx extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.LYX;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToLyx.class);

    public PushToLyx(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
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
        if (couldNotPush) {
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
        return new PushToLyxSettings(application, dialogService, this.preferences.getFilePreferences(), preferences);
    }

    @Override
    public void pushEntries(BibDatabaseContext database, final List<BibEntry> entries, final String keyString) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        Path lp = Path.of(commandPath); // this needs to fixed because it gives "asdf" when going prefs.get("lyxpipe")
        if (!Files.exists(lp) || !Files.isWritable(lp)) {
            // See if it helps to append ".in":
            lp = Path.of(commandPath + ".in");
            if (!Files.exists(lp) || !Files.isWritable(lp)) {
                couldNotPush = true;
                return;
            }
        }

        final Path lyxPipe = lp;

        HeadlessExecutorService.INSTANCE.executeAndWait(() -> {
            try (BufferedWriter lyxOut = Files.newBufferedWriter(lyxPipe, StandardCharsets.UTF_8)) {
                String citeStr = "LYXCMD:sampleclient:citation-insert:" + keyString;
                lyxOut.write(citeStr + "\n");
            } catch (IOException excep) {
                couldNotCall = true;
                LOGGER.warn("Problem pushing to LyX/Kile.", excep);
            }
        });
    }
}
