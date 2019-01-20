package org.jabref.gui.push;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Customized UI component for pushing to external applications. Has a selection popup menu to change the selected
 * external application. This class implements the ActionListener interface. When actionPerformed() is invoked, the
 * currently selected PushToApplication is activated. The actionPerformed() method can be called with a null argument.
 */
public class PushToApplicationButton extends SimpleCommand implements ActionListener {

    private final JabRefFrame frame;
    private final List<PushToApplication> pushActions;
    private final PushToApplication toApp;
    private final Map<PushToApplication, PushToApplicationAction> actions = new HashMap<>();

    public PushToApplicationButton(JabRefFrame frame, List<PushToApplication> pushActions) {
        this.frame = frame;
        this.pushActions = pushActions;
        // Set the last used external application
        toApp = getLastUsedApplication();
    }

    private PushToApplication getLastUsedApplication() {
        String appSelected = Globals.prefs.get(JabRefPreferences.PUSH_TO_APPLICATION);
        for (PushToApplication application : pushActions) {
            if (application.getApplicationName().equals(appSelected)) {
                return application;
            }
        }

        // Nothing found, pick first
        return pushActions.get(0);
    }

    public org.jabref.gui.actions.Action getMenuAction() {
        PushToApplication application = getLastUsedApplication();

        return new org.jabref.gui.actions.Action() {
            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(application.getIcon());
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.of(KeyBinding.PUSH_TO_APPLICATION);
            }

            @Override
            public String getText() {
                return Localization.lang("Push entries to external application (%0)", application.getApplicationName());
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        execute();
    }

    @Override
    public void execute() {
        // Lazy initialization of the push action:
        PushToApplicationAction action = actions.get(toApp);
        if (action == null) {
            action = new PushToApplicationAction(frame, toApp);
            actions.put(toApp, action);
        }
        action.actionPerformed(new ActionEvent(toApp, 0, "push"));
    }
}
