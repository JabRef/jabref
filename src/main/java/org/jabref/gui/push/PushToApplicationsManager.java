package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.preferences.PreferencesService;

public class PushToApplicationsManager {

    private final List<PushToApplication> applications;
    private final List<Object> reconfigurableControls = new ArrayList<>();

    private final DialogService dialogService;

    private final PushToApplicationAction action;

    public PushToApplicationsManager(DialogService dialogService,
                                     StateManager stateManager,
                                     PreferencesService preferencesService) {

        this.dialogService = dialogService;

        // Set up the current available choices:
        applications = List.of(
                new PushToEmacs(dialogService),
                new PushToLyx(dialogService),
                new PushToTexmaker(dialogService),
                new PushToTeXstudio(dialogService),
                new PushToVim(dialogService),
                new PushToWinEdt(dialogService));

        this.action = new PushToApplicationAction(
                getApplicationByName(preferencesService.getExternalApplicationsPreferences().getPushToApplicationName()),
                stateManager,
                dialogService);
    }

    public List<PushToApplication> getApplications() {
        return applications;
    }

    public PushToApplicationAction getPushToApplicationAction() {
        return action;
    }

    public void registerReconfigurable(Object object) {
        this.reconfigurableControls.add(object);
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

    public void updateApplicationAction(PushToApplication application) {
        final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        this.action.updateApplication(application);

        reconfigurableControls.forEach(object -> {
            if (object instanceof MenuItem) {
                factory.configureMenuItem(action.getActionInformation(), action, (MenuItem) object);
            } else if (object instanceof ButtonBase) {
                factory.configureIconButton(action.getActionInformation(), action, (ButtonBase) object);
            }
        });
    }
}
