package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;

public class PushToApplicationsManager {

    private final List<PushToApplication> applications;

    private final DialogService dialogService;

    private final PushToApplicationAction action;
    private MenuItem menuItem;
    private Button toolBarButton;

    public PushToApplicationsManager(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        // Set up the current available choices:
        applications = new ArrayList<>();
        applications.add(new PushToEmacs(dialogService));
        applications.add(new PushToLyx(dialogService));
        applications.add(new PushToTexmaker(dialogService));
        applications.add(new PushToTeXstudio(dialogService));
        applications.add(new PushToVim(dialogService));
        applications.add(new PushToWinEdt(dialogService));

        this.action = new PushToApplicationAction(stateManager, this, dialogService);
    }

    public List<PushToApplication> getApplications() {
        return applications;
    }

    public PushToApplicationAction getPushToApplicationAction() {
        return action;
    }

    public void setMenuItem(MenuItem menuItem) {
        this.menuItem = menuItem;
    }

    public void setToolBarButton(Button toolBarButton) {
        this.toolBarButton = toolBarButton;
    }

    public PushToApplicationSettings getSettings(PushToApplication application) {
        if (application instanceof PushToEmacs) {
            return new PushToEmacsSettings(application, dialogService);
        } else if (application instanceof PushToLyx) {
            return new PushToLyxSettings(application, dialogService);
        } else if (application instanceof PushToVim) {
            return new PushToVimSettings(application, dialogService);
        } else {
            return new PushToApplicationSettings(application, dialogService);
        }
    }

    public PushToApplication getApplicationByName(String applicationName) {
        return applications.stream()
                           .filter(application -> application.getApplicationName().equals(applicationName))
                           .findAny()
                           .orElse(applications.get(0));
    }

    public void updateApplicationAction() {
        final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        action.updateApplication(Globals.prefs.getActivePushToApplication(this));

        if (menuItem != null) {
            factory.configureMenuItem(action.getActionInformation(), action, menuItem);
        }

        if (toolBarButton != null) {
            factory.configureIconButton(action.getActionInformation(), action, toolBarButton);
        }
    }
}
