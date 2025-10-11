package org.jabref.logic.push;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.NotificationService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for pushing entries into different editors.
 */
public abstract class AbstractPushToApplication implements PushToApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPushToApplication.class);
    protected boolean couldNotCall; // Set to true in case the command could not be executed, e.g., if the file is not found
    protected boolean couldNotPush; // Set to true in case the tunnel to the program (if one is used) does not operate
    protected boolean notDefined; // Set to true if the corresponding path is not defined in the preferences

    protected String commandPath;

    protected final NotificationService notificationService;
    protected final PushToApplicationPreferences preferences;

    public AbstractPushToApplication(NotificationService notificationService, PushToApplicationPreferences preferences) {
        this.notificationService = notificationService;
        this.preferences = preferences;
    }

    protected String getKeyString(List<BibEntry> entries, @NonNull String delimiter) {
        return entries.stream()
                      .map(BibEntry::getCitationKey)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .filter(key -> !key.isEmpty())
                      .collect(Collectors.joining(delimiter));
    }

    public void pushEntries(List<BibEntry> entries) {
        pushEntries(entries, new ProcessBuilder());
    }

    @VisibleForTesting
    public void pushEntries(List<BibEntry> entries, ProcessBuilder processBuilder) {
        couldNotPush = false;
        couldNotCall = false;
        notDefined = false;

        commandPath = preferences.getCommandPaths().get(this.getDisplayName());

        // Check if a path to the command has been specified
        if (StringUtil.isNullOrEmpty(commandPath)) {
            notDefined = true;
            return;
        }

        String keyString = this.getKeyString(entries, getDelimiter());

        // Execute command
        try {
            if (OS.OS_X) {
                String[] commands = getCommandLine(keyString);
                if (commands.length < 3) {
                    LOGGER.error("Commandline does not contain enough parameters to \"push to application\"");
                    return;
                }
                processBuilder.command(
                        "open",
                        "-a",
                        commands[0],
                        "-n",
                        "--args",
                        commands[1],
                        commands[2]
                );
                processBuilder.start();
            } else {
                processBuilder.command(getCommandLine(keyString));
                processBuilder.start();
            }
        } catch (IOException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;
        }
    }

    @Override
    public void onOperationCompleted() {
        if (notDefined) {
            sendErrorNotification(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Path to %0 not defined", getDisplayName()) + ".");
        } else if (couldNotCall) {
            sendErrorNotification(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Could not call executable") + " '" + commandPath + "'.");
        } else if (couldNotPush) {
            sendErrorNotification(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Could not connect to %0", getDisplayName()) + ".");
        } else {
            notificationService.notify(Localization.lang("Pushed citations to %0", getDisplayName()) + ".");
        }
    }

    @Override
    public void sendErrorNotification(String message) {
        this.sendErrorNotification(message, "");
    }

    @Override
    public void sendErrorNotification(String title, String message) {
        if (StringUtil.isNullOrEmpty(message)) {
            notificationService.notify(title);
        } else {
            notificationService.notify(title.concat(" ").concat(message));
        }
    }

    @Override
    public boolean requiresCitationKeys() {
        return true;
    }

    /**
     * Constructs the command line arguments for pushing citations to the application.
     * The method formats the citation key and prefixes/suffixes as per user preferences
     * before invoking the application with the command to insert text.
     *
     * @param keyString String containing the Bibtex keys to be pushed to the application
     * @return String array with the command to call and its arguments
     */
    @SuppressWarnings("unused")
    protected String[] getCommandLine(String keyString) {
        return new String[0];
    }

    /**
     * Function to get the command name in case it is different from the application name
     *
     * @return String with the command name
     */
    public String getCommandName() {
        return null;
    }

    protected String getCitePrefix() {
        return preferences.getCiteCommand().prefix();
    }

    public String getDelimiter() {
        return preferences.getCiteCommand().delimiter();
    }

    protected String getCiteSuffix() {
        return preferences.getCiteCommand().suffix();
    }

    public void jumpToLine(Path fileName, int line, int column) {
        commandPath = preferences.getCommandPaths().get(this.getDisplayName());

        if (StringUtil.isNullOrEmpty(commandPath)) {
            notDefined = true;
            return;
        }

        String[] command = jumpToLineCommandlineArguments(fileName, line, column);
        ProcessBuilder processBuilder = new ProcessBuilder();
        try {
            processBuilder.command(command);
            processBuilder.start();
        } catch (IOException excep) {
            LOGGER.warn("Error: Could not call executable '{}'", commandPath, excep);
            couldNotCall = true;
        }
    }

    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        LOGGER.error("Not yet implemented");
        return new String[0];
    }
}
