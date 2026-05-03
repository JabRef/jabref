package org.jabref.logic.push;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToVim extends AbstractPushToApplication {

    public static final PushApplications APPLICATION = PushApplications.VIM;

    private static final Logger LOGGER = LoggerFactory.getLogger(PushToVim.class);

    public PushToVim(NotificationService notificationService, PushToApplicationPreferences preferences) {
        super(notificationService, preferences);
    }

    @Override
    public String getDisplayName() {
        return APPLICATION.getDisplayName();
    }

    @Override
    public void pushEntries(List<BibEntry> entries) {
        if (!determineCommandPath()) {
            return;
        }

        String keyString = this.getKeyString(entries, getDelimiter());

        try {
            String[] com = new String[] {commandPath, "--servername",
                    preferences.getVimServer(), "--remote-send",
                    "<C-\\><C-N>a" + getCitePrefix() + keyString + getCiteSuffix()};

            LOGGER.atDebug()
                  .setMessage("Executing command {}")
                  .addArgument(() -> Arrays.toString(com))
                  .log();

            final Process p = Runtime.getRuntime().exec(com);

            HeadlessExecutorService.INSTANCE.executeAndWait(() -> {
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
                        LOGGER.warn("Push to Vim error: {}", sb);
                        couldNotPush = true;
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error handling std streams", e);
                }
            });
        } catch (IOException excep) {
            LOGGER.warn("Problem pushing to Vim.", excep);
            couldNotCall = true;
        }
    }

    @Override
    public void onOperationCompleted() {
        if (couldNotPush) {
            sendErrorNotification(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not push to a running Vim server."));
        } else if (couldNotCall) {
            sendErrorNotification(Localization.lang("Error pushing entries"),
                    Localization.lang("Could not run the 'vim' program."));
        } else {
            super.onOperationCompleted();
        }
    }

    @Override
    public void jumpToLine(Path fileName, int line, int column) {
        if (!determineCommandPath()) {
            return;
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            String[] command = jumpToLineCommandlineArguments(fileName, line, column);
            if (OS.WINDOWS) {
                processBuilder.command("cmd",
                        "/c",
                        "start",
                        "",
                        "\"%s\"".formatted(command[0]),
                        "\"%s\"".formatted(command[1]),
                        "\"%s\"".formatted(command[2]),
                        "\"+normal %s|\"".formatted(Integer.toString(column)));
            } else if (OS.LINUX) {
                processBuilder.command("gnome-terminal",
                        "--",
                        command[0],
                        command[1],
                        command[2],
                        command[3]);
            } else if (OS.OS_X) {
                processBuilder.command("open",
                        "-a",
                        "Terminal",
                        "--args",
                        command[0],
                        command[1],
                        command[2],
                        command[3]);
            }
            processBuilder.start();
        } catch (IOException e) {
            LOGGER.warn("Problem pushing to Vim.", e);
            couldNotCall = true;
        }
    }

    private boolean determineCommandPath() {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getCommandPaths().get(this.getDisplayName());

        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return false;
        }
        return true;
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "+%s".formatted(line), fileName.toString(), "+\"normal %s|\"".formatted(column)};
    }
}
