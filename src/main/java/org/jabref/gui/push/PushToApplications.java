package org.jabref.gui.push;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;

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

    public static List<PushToApplication> getAllApplications(DialogService dialogService, GuiPreferences preferences) {
        if (!APPLICATIONS.isEmpty()) {
            return APPLICATIONS;
        }

        APPLICATIONS.addAll(List.of(
                new PushToEmacs(dialogService, preferences),
                new PushToLyx(dialogService, preferences),
                new PushToSublimeText(dialogService, preferences),
                new PushToTexmaker(dialogService, preferences),
                new PushToTeXstudio(dialogService, preferences),
                new PushToTeXworks(dialogService, preferences),
                new PushToVim(dialogService, preferences),
                new PushToWinEdt(dialogService, preferences),
                new PushToTexShop(dialogService, preferences),
                new PushToVScode(dialogService, preferences)));

        return APPLICATIONS;
    }

    public static Optional<PushToApplication> getApplicationByName(String applicationName, DialogService dialogService, GuiPreferences preferences) {
        return getAllApplications(dialogService, preferences).stream()
                                                             .filter(application -> application.getDisplayName().equals(applicationName))
                                                             .findAny();
    }
}
