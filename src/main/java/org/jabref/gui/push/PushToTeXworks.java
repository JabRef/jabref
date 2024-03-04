package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.preferences.PreferencesService;

public class PushToTeXworks extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXWORKS;

    /**
     * Constructs a new {@code PushToTeXworks} instance.
     *
     * @param dialogService The dialog service for displaying messages to the user.
     * @param preferencesService The service for accessing user preferences.
     */
    public PushToTeXworks(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    /**
     * Gets the display name for the TeXworks push operation. This name is used 
     * in the GUI to represent the push action to the user.
     *
     * @return The display name for the push operation.
     */
    @Override
    public String getDisplayName() {
        return NAME;
    }

    /**
     * Gets the icon associated with the TeXworks application.
     * TODO: replace the placerholer icon with the real one.
     *
     * @return The icon for the TeXworks application.
     */
    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.DEFAULT_GROUP_ICON;
    }

    /**
    * Constructs the command line arguments for pushing citations to TeXworks.
    * The method formats the citation key and prefixes/suffixes as per user preferences
    * before invoking TeXworks with the command to insert text.
    *
    * @param keyString The citation key to be pushed.
    * @return An array of {@code String} containing the command line to execute.
    */
    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-text", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }

    /**
    * Gets the tooltip for the TeXworks push operation. The tooltip is used to display a short description of the push operation in the GUI.
    *
    * @param newCommandPath The new command path to be set.
    */
    public void setCommandPath(String newCommandPath) {
        commandPath = newCommandPath;
    }
}
