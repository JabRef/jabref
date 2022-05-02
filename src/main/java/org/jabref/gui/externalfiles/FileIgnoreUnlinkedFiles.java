package org.jabref.gui.externalfiles;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;

public class FileIgnoreUnlinkedFiles {
    private final String description;
    private final List<String> filesToIgnore;

    FileIgnoreUnlinkedFiles(FileType fileType) {
        this.description = Localization.lang("%0", fileType.getName());
        this.filesToIgnore = fileType.getExtensionsWithDot();
    }

    public String getDescription() {
        return this.description;
    }
}
