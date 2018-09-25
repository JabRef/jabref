package org.jabref.gui.push;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

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
    protected final TextField path1 = new TextField();
    protected JPanel settings;
    protected GridPane jfxSettings;
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
    public JPanel getSettingsPanel(int n) {
        switch (n) {
            case 0: application = new PushToEmacs(dialogService);
                    break;
            case 1: application = new PushToLyx(dialogService);
                    break;
            case 2: application = new PushToTexmaker(dialogService);
                    break;
            case 3: application = new PushToTeXstudio(dialogService);
                     break;
            case 4: application = new PushToVim(dialogService);
                    break;
            case 5: application = new PushToWinEdt(dialogService);
                    break;
            default: application = null;
                    break;
        }
        application.initParameters();
        String commandPath = Globals.prefs.get(application.commandPathPreferenceKey);
        if (settings == null) {
            initSettingsPanel();
        }
        path.setText(commandPath);
        return settings;
    }

    public GridPane getJFXSettingPane() {
        application.initParameters();
        String commandPath = Globals.prefs.get(application.commandPathPreferenceKey);
        if (jfxSettings == null) {
            initJFXSettingsPanel();
        }
        path1.setText(commandPath);
        return jfxSettings;
    }

    /**
     * Create a FormBuilder, fill it with a textbox for the path and store the JPanel in settings
     */
    protected void initSettingsPanel() {
        builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "p"));
        StringBuilder label = new StringBuilder(Localization.lang("Path to %0", application.commandPathPreferenceKey));
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

    protected void initJFXSettingsPanel() {
        jfxSettings = new GridPane();
        StringBuilder label = new StringBuilder(Localization.lang("Path to %0", application.getApplicationName()));
        // In case the application name and the actual command is not the same, add the command in brackets
        if (application.getCommandName() == null) {
            label.append(':');
        } else {
            label.append(" (").append(application.getCommandName()).append("):");
        }
        jfxSettings.add(new Label(label.toString()),1,1);
        jfxSettings.add(path1,2, 1);
        Button browse = new Button(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();

        browse.setOnAction(
                e -> DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showFileOpenDialog(fileDialogConfiguration))
                        .ifPresent(f -> path.setText(f.toAbsolutePath().toString())));
        jfxSettings.add(browse,3, 1);
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
