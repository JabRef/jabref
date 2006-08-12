package net.sf.jabref.external;

import net.sf.jabref.gui.MainTable;
import net.sf.jabref.*;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.io.File;
import java.io.IOException;


/**
 * This class holds the functionality of autolinking to a file that's dropped
 * onto an entry.
 *
 * Options for handling the files are:
 * 1) Link to the file in its current position (disabled if the file is remote)
 * 2) Copy the file to ??? directory, rename after bibtex key, and link
 * 3) Move the file to ??? directory, rename after bibtex key, and link
 */
public class DroppedFileHandler {
    private JabRefFrame frame;
    private BasePanel panel;

    private JRadioButton
        linkInPlace = new JRadioButton(Globals.lang("Link from entry")),
        copyRename = new JRadioButton(),
        moveRename = new JRadioButton();
    private JPanel optionsPanel = new JPanel();

    public DroppedFileHandler(JabRefFrame frame, BasePanel panel) {

        this.frame = frame;
        this.panel = panel;

        ButtonGroup grp = new ButtonGroup();
        grp.add(linkInPlace);
        grp.add(copyRename);
        grp.add(moveRename);
        copyRename.setSelected(true);

        DefaultFormBuilder builder = new DefaultFormBuilder(optionsPanel,
                new FormLayout("left:pref", ""));
        builder.append(linkInPlace);
        builder.append(copyRename);
        builder.append(moveRename);
    }

    /**
     * Offer copy/move/linking options for a dragged external file. Perform the
     * chosen operation, if any.
     * @param fileName The name of the dragged file.
     * @param fileType The FileType associated with the file.
     * @param localFile Indicate whether this is a local file, or a remote file copied
     *                  to a local temporary file.
     * @param mainTable The MainTable the file was dragged to.
     * @param dropRow The row where the file was dropped.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, boolean localFile,
                                  MainTable mainTable, int dropRow) {

        linkInPlace.setEnabled(localFile);
        if (!localFile && linkInPlace.isSelected())
            copyRename.setSelected(true);
        
        BibtexEntry entry = mainTable.getEntryAt(dropRow);
        String destFilename = entry.getCiteKey()+"."+fileType.getExtension();
        copyRename.setText(Globals.lang("Copy to %0 directory, rename to '%1', and link from entry.",
                fileType.getName(), destFilename));
        moveRename.setText(Globals.lang("Move to %0 directory, rename to '%1', and link from entry.",
                fileType.getName(), destFilename));
        int reply = JOptionPane.showConfirmDialog(frame, optionsPanel,
                Globals.lang("Link to file"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (reply != JOptionPane.OK_OPTION)
            return;

        // Ok, we're ready to go. See first if we need to do a file copy before
        // linking:
        boolean success = true;

        if (linkInPlace.isSelected()) {
            destFilename = fileName;
        }
        else if (copyRename.isSelected()) {
            success = doCopy(fileName, fileType, destFilename);
        }
        else if (moveRename.isSelected()) {
            success = doRename(fileName, fileType, destFilename);
        }
        if (success) {
            doLink(entry, fileType, destFilename);
            panel.markBaseChanged();
        }
    }

    /**
     * Make a link to the file.
     * @param entry The entry to link from.
     * @param fileType The FileType associated with the file.
     * @param filename The path to the file.
     */
    private void doLink(BibtexEntry entry, ExternalFileType fileType, String filename) {
        UndoableFieldChange edit = new UndoableFieldChange(entry, fileType.getFieldName(),
                entry.getField(fileType.getFieldName()), filename);
        entry.setField(fileType.getFieldName(), filename);
        panel.undoManager.addEdit(edit);
    }

    /**
     * Move the given file to the base directory for its file type, and
     * rename it to the given filename.
     * @param fileName The name of the source file.
     * @param fileType The FileType associated with the file.
     * @param destFilename The destination filename.
     * @return true if the operation succeeded.
     */
    private boolean doRename(String fileName, ExternalFileType fileType,
                             String destFilename) {
        String dir = panel.metaData().getFileDirectory(fileType.getFieldName());
        if ((dir == null) || !(new File(dir)).exists()) {
            // OOps, we don't know which directory to put it in, or the given dir
            // doesn't exist....
            return false;
        }
        File f = new File(fileName);
        File destFile = new File(new StringBuffer(dir).
                append(System.getProperty("file.separator")).
                append(destFilename).toString());
        f.renameTo(destFile);
        return true;
    }



    /**
     * Copy the given file to the base directory for its file type,
     * and give it the given name.
     * @param fileName The name of the source file.
     * @param fileType The FileType associated with the file.
     * @param toFile The destination filename.
     * @return
     */
    private boolean doCopy(String fileName, ExternalFileType fileType, String toFile) {

        String dir = panel.metaData().getFileDirectory(fileType.getFieldName());
        if ((dir == null) || !(new File(dir)).exists()) {
            // OOps, we don't know which directory to put it in, or the given dir
            // doesn't exist....
            System.out.println("dir: "+dir+"\t ext: "+fileType.getExtension());
            return false;
        }
        File destFile = new File(new StringBuffer(dir).append(System.getProperty("file.separator")).
                append(toFile).toString());
        if (destFile.exists()) {
            int answer = JOptionPane.showConfirmDialog(frame, "'"+destFile.getPath()+"' "+
                    Globals.lang("exists.Overwrite?"), Globals.lang("File exists"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION)
                return false;
        }
        try {
            Util.copyFile(new File(fileName), destFile, true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


}
