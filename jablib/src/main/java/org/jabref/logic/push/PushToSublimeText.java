package org.jabref.logic.push;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.model.entry.BibEntry;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToSublimeText extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.SUBLIME_TEXT;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToSublimeText.class);

    public PushToSublimeText(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    public void pushEntries(List<BibEntry> entries) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getCommandPaths().get(this.getDisplayName());

        // Check if a path to the command has been specified
        if (StringUtil.isNullOrEmpty(commandPath)) {
            notDefined = true;
            return;
        }
        try {
            String keyString = this.getKeyString(entries, getDelimiter());
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
