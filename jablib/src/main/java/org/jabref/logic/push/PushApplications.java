package org.jabref.logic.push;

import java.util.Optional;

public enum PushApplications {

    EMACS("emacs", "Emacs"),
    LYX("lyx", "LyX/Kile"),
    TEXMAKER("texmaker", "Texmaker"),
    TEXSTUDIO("texstudio", "TeXstudio"),
    TEXWORKS("texworks", "TeXworks"),
    VIM("vim", "Vim"),
    WIN_EDT("winedt", "WinEdt"),
    SUBLIME_TEXT("sublime", "Sublime Text"),
    TEXSHOP("texshop", "TeXShop"),
    VSCODE("vscode", "VScode");

    private final String id;
    private final String displayName;

    PushApplications(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static Optional<PushApplications> getApplicationByDisplayName(String key) {
        for (PushApplications application : PushApplications.values()) {
            if (application.getDisplayName().equalsIgnoreCase(key)) {
                return Optional.of(application);
            }
        }
        return Optional.empty();
    }

    public static Optional<PushApplications> getApplicationById(String key) {
        for (PushApplications application : PushApplications.values()) {
            if (application.getId().equalsIgnoreCase(key)) {
                return Optional.of(application);
            }
        }
        return Optional.empty();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
