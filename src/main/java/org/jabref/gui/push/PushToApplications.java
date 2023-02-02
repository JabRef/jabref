package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.preferences.PreferencesService;

public class PushToApplications {

    public static final String EMACS = "Emacs";
    public static final String LYX = "LyX/Kile";
    public static final String TEXMAKER = "Texmaker";
    public static final String TEXSTUDIO = "TeXstudio";
    public static final String VIM = "Vim";
    public static final String WIN_EDT = "WinEdt";

    private static final List<PushToApplication> APPLICATIONS = new ArrayList<>();

    private PushToApplications() {
    }

    public static List<PushToApplication> getAllApplications(DialogService dialogService, PreferencesService preferencesService) {
        if (!APPLICATIONS.isEmpty()) {
            return APPLICATIONS;
        }

        APPLICATIONS.addAll(List.of(
                new PushToEmacs(dialogService, preferencesService),
                new PushToLyx(dialogService, preferencesService),
                new PushToTexmaker(dialogService, preferencesService),
                new PushToTeXstudio(dialogService, preferencesService),
                new PushToVim(dialogService, preferencesService),
                new PushToWinEdt(dialogService, preferencesService)));

        return APPLICATIONS;
    }

    public static Optional<PushToApplication> getApplicationByName(String applicationName, DialogService dialogService, PreferencesService preferencesService) {
        return getAllApplications(dialogService, preferencesService).stream()
                                                                    .filter(application -> application.getDisplayName().equals(applicationName))
                                                                    .findAny();
    }
}
