package org.jabref.gui.filelist;

import org.jabref.model.entry.LinkedFile;

/**
 * An implementation of this interface is called to confirm whether a FileListEntryEditor
 * is ready to close when Ok is pressed, or whether there is a problem that needs to be
 * resolved first.
 */
@FunctionalInterface
public interface ConfirmCloseFileListEntryEditor {

    boolean confirmClose(LinkedFile entry);
}
