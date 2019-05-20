package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

public class PushToApplicationsManager {

    private final List<PushToApplication> applications;

    public PushToApplicationsManager(DialogService dialogService) {
        // Set up the current available choices:
        applications = new ArrayList<>();
        applications.add(new PushToEmacs(dialogService));
        applications.add(new PushToLyx(dialogService));
        applications.add(new PushToTexmaker(dialogService));
        applications.add(new PushToTeXstudio(dialogService));
        applications.add(new PushToVim(dialogService));
        applications.add(new PushToWinEdt(dialogService));
    }

    public List<PushToApplication> getApplications() {
        return applications;
    }

    public static PushToApplicationSettings getSettings(PushToApplication application) {
        if (application instanceof PushToEmacs) {
            return new PushToEmacsSettings();
        } else if (application instanceof PushToLyx) {
            return new PushToLyxSettings();
        } else if (application instanceof PushToVim) {
            return new PushToVimSettings();
        } else {
            return new PushToApplicationSettings();
        }
    }

    public PushToApplication getLastUsedApplication(JabRefPreferences preferences) {
        String appSelected = preferences.get(JabRefPreferences.PUSH_TO_APPLICATION);
        return applications.stream()
                           .filter(application -> application.getApplicationName().equals(appSelected))
                           .findAny()
                           .orElse(applications.get(0));
    }
}
