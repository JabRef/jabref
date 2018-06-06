package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.DialogService;

public class PushToApplications {

    private final List<PushToApplication> applications;

    public PushToApplications(DialogService dialogService) {
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
        return null;
    }
}
