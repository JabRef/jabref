package net.sf.jabref.external;

import net.sf.jabref.gui.FileListEntry;

/**
 * An implementation of this interface is called to confirm whether a FileListEntryEditor
 * is ready to close when Ok is pressed, or whether there is a problem that needs to be
 * resolved first.
 */
public interface ConfirmCloseFileListEntryEditor {

    public boolean confirmClose(FileListEntry entry);
}
