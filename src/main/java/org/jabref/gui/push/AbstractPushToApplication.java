package org.jabref.gui.push;

import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
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
    protected JPanel settings;
    protected final JTextField path = new JTextField(30);
    protected String commandPath;
    protected String commandPathPreferenceKey;
    protected FormBuilder builder;

    @Override
    public String getName() {
        return Localization.menuTitle("Push entries to external application (%0)", getApplicationName());
    }

    @Override
    public String getTooltip() {
        return Localization.lang("Push to %0", getApplicationName());
    }

    @Override
    public void pushEntries(BibDatabase database, List<BibEntry> entries, String keyString, MetaData metaData) {

        couldNotConnect = false;
        couldNotCall = false;
        notDefined = false;

        initParameters();
        commandPath = Globals.prefs.get(commandPathPreferenceKey);

        // Check if a path to the command has been specified
        if ((commandPath == null) || commandPath.trim().isEmpty()) {
            notDefined = true;
            return;
        }

        // Execute command
        try {
            if (OS.OS_X) {
                String[] commands = getCommandLine(keyString);
                ProcessBuilder processBuilder = new ProcessBuilder("open -a " + commands[0] + " -n --args " + commands[1] + " " + commands[2]);
                processBuilder.start();
            } else {
                ProcessBuilder processBuilder = new ProcessBuilder(getCommandLine(keyString));
                processBuilder.start();
            }
        }

        // In case it did not work
        catch (IOException excep) {
            couldNotCall = true;

            LOGGER.warn("Error: Could not call executable '" + commandPath + "'.", excep);
        }
    }

    @Override
    public void operationCompleted(BasePanel panel) {
        if (notDefined) {
            panel.output(Localization.lang("Error") + ": "
                    + Localization.lang("Path to %0 not defined", getApplicationName()) + ".");
        } else if (couldNotCall) {
            panel.output(Localization.lang("Error") + ": "
                    + Localization.lang("Could not call executable") + " '" + commandPath + "'.");
        } else if (couldNotConnect) {
            panel.output(Localization.lang("Error") + ": "
                    + Localization.lang("Could not connect to %0", getApplicationName()) + ".");
        } else {
            panel.output(Localization.lang("Pushed citations to %0", getApplicationName()) + ".");
        }
    }

    @Override
    public boolean requiresBibtexKeys() {
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

    @Override
    public JPanel getSettingsPanel() {
        initParameters();
        commandPath = Globals.prefs.get(commandPathPreferenceKey);
        if (settings == null) {
            initSettingsPanel();
        }
        path.setText(commandPath);
        return settings;
    }

    /**
     * Function to initialize parameters. Currently it is expected that commandPathPreferenceKey is set to the path of
     * the application.
     */
    protected abstract void initParameters();

    /**
     * Create a FormBuilder, fill it with a textbox for the path and store the JPanel in settings
     */
    protected void initSettingsPanel() {
        builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p"));
        StringBuilder label = new StringBuilder(Localization.lang("Path to %0", getApplicationName()));
        // In case the application name and the actual command is not the same, add the command in brackets
        if (getCommandName() == null) {
            label.append(':');
        } else {
            label.append(" (").append(getCommandName()).append("):");
        }
        builder.add(label.toString()).xy(1, 1);
        builder.add(path).xy(3, 1);
        JButton browse = new JButton(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();

        browse.addActionListener(
                e -> DefaultTaskExecutor.runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration))
                        .ifPresent(f -> path.setText(f.toAbsolutePath().toString())));
        builder.add(browse).xy(5, 1);
        settings = builder.build();
    }

    @Override
    public void storeSettings() {
        Globals.prefs.put(commandPathPreferenceKey, path.getText());
    }

    protected String getCiteCommand() {
        return Globals.prefs.get(JabRefPreferences.CITE_COMMAND);
    }
}
