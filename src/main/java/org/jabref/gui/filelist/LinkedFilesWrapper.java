package org.jabref.gui.filelist;

import org.jabref.model.entry.LinkedFile;

/**
 * Workaround to inject {@link LinkedFile} into javafx controller
 */
public class LinkedFilesWrapper {

    private LinkedFile linkedFile;

    public LinkedFile getLinkedFile() {
        return linkedFile;
    }

    public void setLinkedFile(LinkedFile linkedFile) {
        this.linkedFile = linkedFile;
    }
}
