package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final PushToApplicationAction action;

    public PushToApplicationsManager(DialogService dialogService,
                                     StateManager stateManager,
                                     PreferencesService preferencesService) {

        // Set up the current available choices:
        applications = List.of(
                new PushToEmacs(dialogService, preferencesService),
                new PushToLyx(dialogService, preferencesService),
                new PushToTexmaker(dialogService, preferencesService),
                new PushToTeXstudio(dialogService, preferencesService),
                new PushToVim(dialogService, preferencesService),
                new PushToWinEdt(dialogService, preferencesService));

        this.action = new PushToApplicationAction(
                getApplicationByName(preferencesService.getExternalApplicationsPreferences().getPushToApplicationName())
                        .orElse(new PushToEmacs(dialogService, preferencesService)),
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

    public Optional<PushToApplication> getApplicationByName(String applicationName) {
        return applications.stream()
                           .filter(application -> application.getDisplayName().equals(applicationName))
                           .findAny();
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
