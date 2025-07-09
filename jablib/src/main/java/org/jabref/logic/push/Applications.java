package org.jabref.logic.push;

import java.util.Optional;

public enum Applications {

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

    Applications(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public static Optional<Applications> getApplicationByDisplayName(String key) {
        for (Applications application : Applications.values()) {
            if (application.getDisplayName().equalsIgnoreCase(key)) {
                return Optional.of(application);
            }
        }
        return Optional.empty();
    }

    public static Optional<Applications> getApplicationById(String key) {
        for (Applications application : Applications.values()) {
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
