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
    private boolean toFileDir;

    public MoveFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor,
                          boolean toFileDir) {
        this.frame = frame;
        this.eEditor = eEditor;
        this.editor = editor;
        this.toFileDir = toFileDir;
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
                String chosenFile;
                if (toFileDir) {
                    String suggName = eEditor.getEntry().getCiteKey()+extension;
                    CheckBoxMessage cbm = new CheckBoxMessage(Globals.lang("Move file to file directory?"),
                            Globals.lang("Rename to '%0'",suggName),
                            Globals.prefs.getBoolean("renameOnMoveFileToFileDir"));
                    int answer;
                    // Only ask about renaming file if the file doesn't have the proper name already:
                    if (!suggName.equals(file.getName()))
                        answer = JOptionPane.showConfirmDialog(frame, cbm, Globals.lang("Move/Rename file"),
                                JOptionPane.YES_NO_OPTION);
                    else
                        answer = JOptionPane.showConfirmDialog(frame, Globals.lang("Move file to file directory?"),
                                Globals.lang("Move/Rename file"), JOptionPane.YES_NO_OPTION);
                    if (answer != JOptionPane.YES_OPTION)
                        return;
                    Globals.prefs.putBoolean("renameOnMoveFileToFileDir", cbm.isSelected());
                    StringBuilder sb = new StringBuilder(dir);
                    if (!dir.endsWith(File.separator))
                        sb.append(File.separator);
                    if (cbm.isSelected()) {
                        // Rename:
                        sb.append(suggName);
                    }
                    else {
                        // Do not rename:
                        sb.append(file.getName());
                    }
                    chosenFile = sb.toString();
                    System.out.println(chosenFile);
                } else {
                    chosenFile = Globals.getNewFile(frame, file, extension, JFileChooser.SAVE_DIALOG, false);
                }
                if (chosenFile == null) {
                    return; // cancelled
                }
                newFile = new File(chosenFile);
                // Check if the file already exists:
                if (newFile.exists() && (JOptionPane.showConfirmDialog
                        (frame, "'" + newFile.getName() + "' " + Globals.lang("exists. Overwrite file?"),
                                Globals.lang("Move/Rename file"), JOptionPane.OK_CANCEL_OPTION)
                        != JOptionPane.OK_OPTION)) {
                    if (!toFileDir)
                        repeat = true;
                    else
                        return;
                }
            }

            if (!newFile.equals(file)) {
                try {
                    boolean success = file.renameTo(newFile);
                    if (!success) {
                        success = Util.copyFile(file, newFile, true);
                    }
                    if (success) {
                        // Relativise path, if possible.
                        if (newFile.getPath().startsWith(dir)) {
                            if ((newFile.getPath().length() > dir.length()) &&
                                    (newFile.getPath().charAt(dir.length()) == File.separatorChar))
                                flEntry.setLink(newFile.getPath().substring(1+dir.length()));
                            else
                                flEntry.setLink(newFile.getPath().substring(dir.length()));


                        }
                        else
                            flEntry.setLink(newFile.getCanonicalPath());
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