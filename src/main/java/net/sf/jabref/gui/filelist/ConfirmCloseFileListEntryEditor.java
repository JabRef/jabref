package net.sf.jabref.gui.filelist;

/**
 * An implementation of this interface is called to confirm whether a FileListEntryEditor
 * is ready to close when Ok is pressed, or whether there is a problem that needs to be
 * resolved first.
 */
@FunctionalInterface
public interface ConfirmCloseFileListEntryEditor {

    boolean confirmClose(FileListEntry entry);
}
