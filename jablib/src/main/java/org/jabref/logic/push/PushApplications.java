package org.jabref.logic.push;

import java.util.Optional;

public enum PushApplications {

    EMACS("emacs", "Emacs", "emacsPath"),
    LYX("lyx", "LyX/Kile", "lyxpipe"),
    TEXMAKER("texmaker", "Texmaker", "texmakerPath"),
    TEXSTUDIO("texstudio", "TeXstudio", "TeXstudioPath"),
    TEXWORKS("texworks", "TeXworks", "TeXworksPath"),
    VIM("vim", "Vim", "vim"),
    WIN_EDT("winedt", "WinEdt", "winEdtPath"),
    SUBLIME_TEXT("sublime", "Sublime Text", "sublimeTextPath"),
    TEXSHOP("texshop", "TeXShop"),
    VSCODE("vscode", "VScode", "VScodePath");

    private final String id;
    private final String displayName;
    private String key;


    PushApplications(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    PushApplications(String id, String displayName, String key) {
        this.id = id;
        this.displayName = displayName;
        this.key = key;
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

    public String getKey(){
        return key;
    }

}
