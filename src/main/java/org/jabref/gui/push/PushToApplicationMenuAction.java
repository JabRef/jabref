package org.jabref.gui.push;

import java.util.Optional;

import javafx.scene.control.MenuItem;

import org.jabref.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.preferences.JabRefPreferences;

/**
 * An Action class representing the process of invoking a PushToApplicationMenu operation.
 */
public class PushToApplicationMenuAction extends SimpleCommand {

    private final PushToApplication application;
    private final PushToApplicationAction pushToApplicationAction;
    private MenuItem pushToApplicationMenuItem;

    public PushToApplicationMenuAction(PushToApplication pushToApplication, PushToApplicationAction pushToApplicationAction, MenuItem pushToApplicationMenuItem) {
        this.application = pushToApplication;
        this.pushToApplicationAction = pushToApplicationAction;
        this.pushToApplicationMenuItem = pushToApplicationMenuItem;
    }

    public Action getActionInformation() {
        return new Action() {

            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(application.getIcon());
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.empty();
            }

            @Override
            public String getText() {
                return application.getApplicationName();
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }

    @Override
    public void execute() {
        Globals.prefs.put(JabRefPreferences.PUSH_TO_APPLICATION, application.getApplicationName());
        pushToApplicationAction.updateApplication(application);
        //pushToApplicationMenuItem.setText(pushToApplicationAction.getActionInformation().getText());
    }
}
