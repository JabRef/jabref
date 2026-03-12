package org.jabref.logic.push;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToVScode extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.VSCODE;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToVScode.class);

    public PushToVScode(NotificationService notificationService, PushToApplicationPreferences preferences) {
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

        if (StringUtil.isNullOrEmpty(commandPath)) {
            notDefined = true;
            return;
        }

        try {
            String keyString = this.getKeyString(entries, getDelimiter());
            ProcessBuilder processBuilder = new ProcessBuilder();

            if (OS.OS_X && commandPath.endsWith(".app")) {
                processBuilder.command(
                        "open",
                        "-a",
                        commandPath,
                        "--args",
                        "--reuse-window",
                        workingDirectory.toString()
                );
            } else {
                // The base class wraps commands with "open -a ... -n" on macOS, which
                // conflicts with --reuse-window: -n forces a new instance
                processBuilder.command(getCommandLine(keyString));
            }
            processBuilder.start();
        } catch (IOException exception) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, exception);
            couldNotCall = true;
        }
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        // TODO - Implementing this will fix https://github.com/JabRef/jabref/issues/6775
        return new String[] {commandPath, "--reuse-window", workingDirectory.toString()};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--reuse-window", "-g", "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
