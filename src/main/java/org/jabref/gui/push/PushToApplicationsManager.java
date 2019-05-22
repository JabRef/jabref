package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.preferences.JabRefPreferences;

public class PushToApplicationsManager {

    private final List<PushToApplication> applications;

    private final DialogService dialogService;

    private final PushToApplicationAction pushToApplicationAction;

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

        this.pushToApplicationAction = new PushToApplicationAction(stateManager, this, dialogService);
    }

    public List<PushToApplication> getApplications() {
        return applications;
    }

    public PushToApplicationAction getPushToApplicationAction() {
        return pushToApplicationAction;
    }

    public PushToApplicationSettings getSettings(PushToApplication application) {
        if (application instanceof PushToEmacs) {
            return new PushToEmacsSettings(dialogService);
        } else if (application instanceof PushToLyx) {
            return new PushToLyxSettings(dialogService);
        } else if (application instanceof PushToVim) {
            return new PushToVimSettings(dialogService);
        } else {
            return new PushToApplicationSettings(dialogService);
        }
    }

    public PushToApplication getActiveApplication(JabRefPreferences preferences) {
        String appSelected = preferences.get(JabRefPreferences.PUSH_TO_APPLICATION);
        return applications.stream()
                           .filter(application -> application.getApplicationName().equals(appSelected))
                           .findAny()
                           .orElse(applications.get(0));
    }
}
