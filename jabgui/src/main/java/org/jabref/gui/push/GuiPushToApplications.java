package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.push.PushToApplicationPreferences;

public class GuiPushToApplications {

    private static final List<GuiPushToApplication> APPLICATIONS = new ArrayList<>();

    private GuiPushToApplications() {
    }

    public static List<GuiPushToApplication> getAllGUIApplications(DialogService dialogService, PushToApplicationPreferences preferences) {
        if (!APPLICATIONS.isEmpty()) {
            return Collections.unmodifiableList(APPLICATIONS);
        }

        APPLICATIONS.addAll(List.of(
                new GuiPushToEmacs(dialogService, preferences),
                new GuiPushToLyx(dialogService, preferences),
                new GuiPushToSublimeText(dialogService, preferences),
                new GuiPushToTexmaker(dialogService, preferences),
                new GuiPushToTeXstudio(dialogService, preferences),
                new GuiPushToTeXworks(dialogService, preferences),
                new GuiPushToVim(dialogService, preferences),
                new GuiPushToWinEdt(dialogService, preferences),
                new GuiPushToTexShop(dialogService, preferences),
                new GuiPushToVScode(dialogService, preferences)));

        return Collections.unmodifiableList(APPLICATIONS);
    }

    /// In case object "preferences" changes for itself, that update won't be propagated. Does not harm as the current callers do not change that reference itself.
    public static Optional<GuiPushToApplication> getGUIApplicationByName(String applicationName, DialogService dialogService, PushToApplicationPreferences preferences) {
        return getAllGUIApplications(dialogService, preferences).stream()
                                                                .filter(application -> application.getDisplayName().equalsIgnoreCase(applicationName))
                                                                .findAny();
    }
}
