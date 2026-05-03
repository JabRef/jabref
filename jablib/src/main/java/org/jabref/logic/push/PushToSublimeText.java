package org.jabref.logic.push;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;

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
        commandPath = preferences.getCommandPaths().getOrDefault(this.getDisplayName(), "");
        // Check if a path to the command has been specified
        if (StringUtil.isBlank(commandPath)) {
            notDefined = true;
            return;
        }
        try {
            String keyString = this.getKeyString(entries, getDelimiter());
            LOGGER.debug("Sublime string: {}", String.join(" ", getCommandLine(keyString)));

            String[] command = getCommandLine(keyString);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Map<String, String> envs = processBuilder.environment();
            Path parent = Path.of(commandPath).getParent();
            if (parent != null) {
                envs.put("PATH", parent.toString());
            }

            Process process = processBuilder.start();
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::info);
            StringBuilder errorStringBuilder = new StringBuilder();
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), s -> {
                LOGGER.warn("Push to Sublime Text error: {}", s);
                errorStringBuilder.append(s).append("\n");
            });

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);

            process.waitFor();
            String error = errorStringBuilder.toString().trim();
            if (process.exitValue() != 0) {
                couldNotPush = true;
                if (!error.isEmpty()) {
                    sendErrorNotification(Localization.lang("Error pushing entries"),
                            Localization.lang("Could not push to %0.", getDisplayName()) + " " + error);
                } else {
                    sendErrorNotification(Localization.lang("Error pushing entries"),
                            Localization.lang("Could not push to %0.", getDisplayName()));
                }
            }
        } catch (IOException | InterruptedException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;

            if (excep instanceof IOException) {
                sendErrorNotification(Localization.lang("Error pushing entries"),
                        Localization.lang("Could not call executable '%0'.", commandPath) + "\n" +
                                Localization.lang("Please check the path in the preferences.") + "\n" +
                                (OS.OS_X ? Localization.lang("On macOS, you can use the command-line binary (e.g., /usr/local/bin/subl).") + "\n" +
                                        Localization.lang("To create it, run: %0", "sudo ln -s /Applications/Sublime\\ Text.app/Contents/SharedSupport/bin/subl /usr/local/bin/subl") : ""));
            }
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            // Detailed error notification might have been sent already in pushEntries
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        String citeCommand = getCitePrefix();
        // we need to escape the extra slashses
        if (getCitePrefix().contains("\\")) {
            citeCommand = "\\" + getCitePrefix();
        }

        return new String[] {commandPath, "--command", "insert {\"characters\": \"" + citeCommand + keyString + getCiteSuffix() + "\"}"};
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
