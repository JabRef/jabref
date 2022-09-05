package org.jabref.gui.push;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for pushing entries into different editors.
 */
public abstract class AbstractPushToApplication implements PushToApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPushToApplication.class);

    protected boolean couldNotCall; // Set to true in case the command could not be executed, e.g., if the file is not found
    protected boolean couldNotConnect; // Set to true in case the tunnel to the program (if one is used) does not operate
    protected boolean notDefined; // Set to true if the corresponding path is not defined in the preferences

    protected String commandPath;

    protected final DialogService dialogService;
    protected final PreferencesService preferencesService;

    public AbstractPushToApplication(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_GENERIC;
    }

    @Override
    public String getTooltip() {
        return Localization.lang("Push entries to external application (%0)", getDisplayName());
    }

    @Override
    public Action getAction() {
        return new PushToApplicationAction();
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

        // Execute command
        try {
            if (OS.OS_X) {
                String[] commands = getCommandLine(keyString);
                if (commands.length < 3) {
                    LOGGER.error("Commandline does not contain enough parameters to \"push to application\"");
                    return;
                }
                ProcessBuilder processBuilder = new ProcessBuilder(
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
                ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(keyString));
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
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Path to %0 not defined", getDisplayName()) + ".");
        } else if (couldNotCall) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Could not call executable") + " '" + commandPath + "'.");
        } else if (couldNotConnect) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Error pushing entries"),
                    Localization.lang("Could not connect to %0", getDisplayName()) + ".");
        } else {
            dialogService.notify(Localization.lang("Pushed citations to %0", getDisplayName()) + ".");
        }
    }

    @Override
    public boolean requiresCitationKeys() {
        return true;
    }

    /**
     * Function to get the command to be executed for pushing keys to be cited
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
    protected String getCommandName() {
        return null;
    }

    protected String getCiteCommand() {
        return preferencesService.getExternalApplicationsPreferences().getCiteCommand();
    }

    public PushToApplicationSettings getSettings(PushToApplication application, PushToApplicationPreferences preferences) {
        return new PushToApplicationSettings(application, dialogService, preferencesService.getFilePreferences(), preferences);
    }

    protected class PushToApplicationAction implements Action {
        @Override
        public String getText() {
            return Localization.lang("Push entries to external application (%0)", getDisplayName());
        }

        @Override
        public Optional<JabRefIcon> getIcon() {
            return Optional.of(getApplicationIcon());
        }

        @Override
        public Optional<KeyBinding> getKeyBinding() {
            return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
        }
    }
}
