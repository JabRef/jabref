package org.jabref.gui.externalfiles;

import org.jabref.logic.l10n.Localization;

public enum FileIgnoreUnlinkedFiles {
    DEFAULT(Localization.lang("Default")),
    INCLUDE_ALL(Localization.lang("Include All Files"));

    private final String fileIgnoreOption;

    FileIgnoreUnlinkedFiles(String fileIgnoreOption) {
        this.fileIgnoreOption = fileIgnoreOption;
    }

    public String getFileIgnoreOption() {
        return fileIgnoreOption;
    }
}
