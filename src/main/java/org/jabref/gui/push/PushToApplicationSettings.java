package org.jabref.gui.push;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PushToApplicationSettings {
    protected final JTextField path = new JTextField(30);
    protected JPanel settings;
    protected FormBuilder builder;
    protected AbstractPushToApplication application;
    private DialogService dialogService;

    /**
     * This method asks the implementing class to return a JPanel populated with the imlementation's options panel, if
     * necessary. If the JPanel is shown to the user, and the user indicates that settings should be stored, the
     * implementation's storeSettings() method will be called. This method must make sure all widgets in the panel are
     * in the correct selection states.
     *
     * @return a JPanel containing options, or null if options are not needed.
     */
    public JPanel getSettingsPanel() {
        application.initParameters();
        String commandPath = Globals.prefs.get(application.commandPathPreferenceKey);
        if (settings == null) {
            initSettingsPanel();
        }
        path.setText(commandPath);
        return settings;
    }

    /**
     * Create a FormBuilder, fill it with a textbox for the path and store the JPanel in settings
     */
    protected void initSettingsPanel() {
        builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p"));
        StringBuilder label = new StringBuilder(Localization.lang("Path to %0", application.getApplicationName()));
        // In case the application name and the actual command is not the same, add the command in brackets
        if (application.getCommandName() == null) {
            label.append(':');
        } else {
            label.append(" (").append(application.getCommandName()).append("):");
        }
        builder.add(label.toString()).xy(1, 1);
        builder.add(path).xy(3, 1);
        JButton browse = new JButton(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();

        browse.addActionListener(
                e -> DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showFileOpenDialog(fileDialogConfiguration))
                                        .ifPresent(f -> path.setText(f.toAbsolutePath().toString())));
        builder.add(browse).xy(5, 1);
        settings = builder.build();
    }

    /**
     * This method is called to indicate that the settings panel returned from the getSettingsPanel() method has been
     * shown to the user and that the user has indicated that the settings should be stored. This method must store the
     * state of the widgets in the settings panel to Globals.prefs.
     */
    public void storeSettings() {
        Globals.prefs.put(application.commandPathPreferenceKey, path.getText());
    }
}
