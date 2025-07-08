package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.preferences.CliPreferences;

public class GUIPushToApplications {

    private static final List<GUIPushToApplication> APPLICATIONS = new ArrayList<>();

    public static List<GUIPushToApplication> getAllGUIApplications(DialogService dialogService, CliPreferences preferences) {
        if (!APPLICATIONS.isEmpty()) {
            return Collections.unmodifiableList(APPLICATIONS);
        }

        APPLICATIONS.addAll(List.of(
                new GUIPushToEmacs(dialogService, preferences),
                new GUIPushToLyx(dialogService, preferences),
                new GUIPushToSublimeText(dialogService, preferences),
                new GUIPushToTexmaker(dialogService, preferences),
                new GUIPushToTeXstudio(dialogService, preferences),
                new GUIPushToTeXworks(dialogService, preferences),
                new GUIPushToVim(dialogService, preferences),
                new GUIPushToWinEdt(dialogService, preferences),
                new GUIPushToTexShop(dialogService, preferences),
                new GUIPushToVScode(dialogService, preferences)));

        return APPLICATIONS;
    }

    public static Optional<GUIPushToApplication> getGUIApplicationByName(String applicationName, DialogService dialogService, CliPreferences preferences) {
        return getAllGUIApplications(dialogService, preferences).stream()
                                                                .filter(application -> application.getDisplayName().equals(applicationName))
                                                                .findAny();
    }
}
