package org.jabref.gui.push;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.StreamGobbler;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToSublimeText extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.SUBLIME_TEXT;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToSublimeText.class);

    public PushToSublimeText(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
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
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getPushToApplicationPreferences().getCommandPaths().get(this.getDisplayName());

        // Check if a path to the command has been specified
        if (StringUtil.isNullOrEmpty(commandPath)) {
            notDefined = true;
            return;
        }
        try {
            LOGGER.debug("Sublime string: {}", String.join(" ", getCommandLine(keyString)));
            ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(keyString));
            processBuilder.inheritIO();
            Map<String, String> envs = processBuilder.environment();
            envs.put("PATH", Path.of(commandPath).getParent().toString());

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
        if (getCitePrefix().contains("\\")) {
            citeCommand = "\"\\" + getCitePrefix();
        }

        if (OS.WINDOWS) {
            // TODO we might need to escape the inner double quotes with """ """
            return new String[] {"cmd.exe", "/c", "\"" + commandPath + "\"" + "--command \"insert {\\\"characters\\\": \"\\" + getCitePrefix() + keyString + getCiteSuffix() + "\"}\""};
        } else {
            return new String[] {"sh", "-c", "\"" + commandPath + "\"" + " --command 'insert {\"characters\": \"" + citeCommand + keyString + getCiteSuffix() + "\"}'"};
        }
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
