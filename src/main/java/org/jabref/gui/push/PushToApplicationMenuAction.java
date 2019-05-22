package org.jabref.gui.push;

import java.util.Optional;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import org.jabref.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
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

    private PushToApplicationsManager manager;

    public PushToApplicationMenuAction(PushToApplication pushToApplication, PushToApplicationAction pushToApplicationAction, PushToApplicationsManager manager) {
        this.application = pushToApplication;
        this.pushToApplicationAction = pushToApplicationAction;
        this.manager = manager;
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
        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        MenuItem menuItem = manager.getMenuItem();
        Button toolBarButton = manager.getToolBarButton();

        if(menuItem != null) {
            factory.configureMenuItem(pushToApplicationAction.getActionInformation(), pushToApplicationAction, menuItem);
        }

        if(toolBarButton != null) {
            factory.configureIconButton(pushToApplicationAction.getActionInformation(),pushToApplicationAction, toolBarButton);
        }
    }
}
