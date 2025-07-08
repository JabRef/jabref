package org.jabref.logic.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;

public class PushToApplications {

    public static final String EMACS = "Emacs";
    public static final String LYX = "LyX/Kile";
    public static final String TEXMAKER = "Texmaker";
    public static final String TEXSTUDIO = "TeXstudio";
    public static final String TEXWORKS = "TeXworks";
    public static final String VIM = "Vim";
    public static final String WIN_EDT = "WinEdt";
    public static final String SUBLIME_TEXT = "Sublime Text";
    public static final String TEXSHOP = "TeXShop";
    public static final String VSCODE = "VScode";

    private static final List<PushToApplication> APPLICATIONS = new ArrayList<>();

    private PushToApplications() {
    }

    public static List<PushToApplication> getAllApplications(NotificationService notificationService, CliPreferences preferences) {
        if (!APPLICATIONS.isEmpty()) {
            return Collections.unmodifiableList(APPLICATIONS);
        }

        APPLICATIONS.addAll(List.of(
                new PushToEmacs(notificationService, preferences),
                new PushToLyx(notificationService, preferences),
                new PushToSublimeText(notificationService, preferences),
                new PushToTexmaker(notificationService, preferences),
                new PushToTeXstudio(notificationService, preferences),
                new PushToTeXworks(notificationService, preferences),
                new PushToVim(notificationService, preferences),
                new PushToWinEdt(notificationService, preferences),
                new PushToTexShop(notificationService, preferences),
                new PushToVScode(notificationService, preferences)));

        return APPLICATIONS;
    }

    public static Optional<PushToApplication> getApplicationByName(String applicationName, NotificationService dialogService, CliPreferences preferences) {
        return getAllApplications(dialogService, preferences).stream()
                                                             .filter(application -> application.getDisplayName().equals(applicationName))
                                                             .findAny();
    }
}
