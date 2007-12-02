package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListEntry;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * Action for moving or renaming a file that is linked to from an entry in JabRef.
 */
public class MoveFileAction extends AbstractAction {
    private JabRefFrame frame;
    private EntryEditor eEditor;
    private FileListEditor editor;

    public MoveFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor) {
        this.frame = frame;
        this.eEditor = eEditor;
        this.editor = editor;
    }

    public void actionPerformed(ActionEvent event) {
        int selected = editor.getSelectedRow();
        if (selected == -1)
            return;
        FileListEntry flEntry = editor.getTableModel().getEntry(selected);
        // Check if the current file exists:
        String ln = flEntry.getLink();
        boolean httpLink = ln.toLowerCase().startsWith("http");
        if (httpLink) {
            // TODO: notify that this operation cannot be done on remote links

        }

        // Get an absolute path representation:
        String dir = frame.basePanel().metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
        File file = Util.expandFilename(ln, new String[]{dir});
        if ((file != null) && file.exists()) {
            // Ok, we found the file. Now get a new name:
            String extension = null;
            if (flEntry.getType() != null)
                extension = "." + flEntry.getType().getExtension();

            File newFile = null;
            boolean repeat = true;
            while (repeat) {
                repeat = false;
                String chosenFile = Globals.getNewFile(frame, file, extension, JFileChooser.SAVE_DIALOG, false);
                if (chosenFile == null) {
                    return; // cancelled
                }
                newFile = new File(chosenFile);
                // Check if the file already exists:
                if (newFile.exists() && (JOptionPane.showConfirmDialog
                        (frame, "'" + newFile.getName() + "' " + Globals.lang("exists. Overwrite file?"),
                                Globals.lang("Move/Rename file"), JOptionPane.OK_CANCEL_OPTION)
                        != JOptionPane.OK_OPTION)) {
                    repeat = true;
                }
            }

            if (!newFile.equals(file)) {
                try {
                    boolean success = file.renameTo(newFile);
                    if (!success) {
                        success = Util.copyFile(file, newFile, true);
                    }
                    if (success) {
                        // TODO: Relativise path?
                        flEntry.setLink(newFile.getPath());
                        eEditor.updateField(editor);
                        JOptionPane.showMessageDialog(frame, Globals.lang("File moved"),
                                Globals.lang("Move/Rename file"), JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(frame, Globals.lang("Move file failed"),
                                Globals.lang("Move/Rename file"), JOptionPane.ERROR_MESSAGE);
                    }

                } catch (SecurityException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, Globals.lang("Could not move file") + ": " + ex.getMessage(),
                            Globals.lang("Move/Rename file"), JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, Globals.lang("Could not move file") + ": " + ex.getMessage(),
                            Globals.lang("Move/Rename file"), JOptionPane.ERROR_MESSAGE);
                }

            }
        }
        else {

            // File doesn't exist, so we can't move it.
            JOptionPane.showMessageDialog(frame, Globals.lang("Could not find file '%0'.", flEntry.getLink()),
                    Globals.lang("File not found"), JOptionPane.ERROR_MESSAGE);
            
        }

    }
}