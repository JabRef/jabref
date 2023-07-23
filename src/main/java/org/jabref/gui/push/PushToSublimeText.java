package org.jabref.gui.push;

import java.io.IOException;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.StreamGobbler;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for pushing entries into SublimeText.
 */
public class PushToSublimeText extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.SUBLIME_TEXT;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToSublimeText.class);

    public PushToSublimeText(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_SUBLIMETEXT;
    }

    @Override
    public void pushEntries(BibDatabaseContext database, List<BibEntry> entries, String keyString) {
        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferencesService.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        // Check if a path to the command has been specified
        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }
        try {

            LOGGER.info("Sublime string: {}", String.join(" ", getCommandLine(keyString)));
            ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(keyString));
            processBuilder.redirectOutput();
            processBuilder.redirectError();
            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::info);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LOGGER::info);

            JabRefExecutorService.INSTANCE.execute(streamGobblerInput);
            JabRefExecutorService.INSTANCE.execute(streamGobblerError);
        } catch (IOException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;
        }
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        if (OS.WINDOWS) {
            return new String[] {commandPath, "--command", "\"insert {\\\"characters\\\": \"\\" + getCiteCommand() + "{" + keyString + "}\\\"}\""};
        } else {
            return new String[] {commandPath, "--command", "'insert {\"characters\": \"\\" + getCiteCommand() + "{" + keyString + "}\"}'"};
        }
    }
}
