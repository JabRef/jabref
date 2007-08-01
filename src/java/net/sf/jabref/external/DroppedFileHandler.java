package net.sf.jabref.external;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.*;
import net.sf.jabref.gui.MainTable;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;
import net.sf.jabref.util.XMPUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class holds the functionality of autolinking to a file that's dropped
 * onto an entry.
 * 
 * Options for handling the files are:
 * 
 * 1) Link to the file in its current position (disabled if the file is remote)
 * 
 * 2) Copy the file to ??? directory, rename after bibtex key, and extension
 * 
 * 3) Move the file to ??? directory, rename after bibtex key, and extension
 */
public class DroppedFileHandler {
    private JabRefFrame frame;

    private BasePanel panel;

    private JRadioButton linkInPlace = new JRadioButton(), copyRadioButton = new JRadioButton(),
        moveRadioButton = new JRadioButton();
    
    private JLabel destDirLabel = new JLabel();

    private JCheckBox renameCheckBox = new JCheckBox();

    private JPanel optionsPanel = new JPanel();

    public DroppedFileHandler(JabRefFrame frame, BasePanel panel) {

        this.frame = frame;
        this.panel = panel;

        ButtonGroup grp = new ButtonGroup();
        grp.add(linkInPlace);
        grp.add(copyRadioButton);
        grp.add(moveRadioButton);
        copyRadioButton.setSelected(true);

        DefaultFormBuilder builder = new DefaultFormBuilder(optionsPanel, new FormLayout(
            "left:pref", ""));
        builder.append(linkInPlace);
        builder.append(destDirLabel);
        builder.append(copyRadioButton);
        builder.append(moveRadioButton);
        builder.append(renameCheckBox);
    }

    /**
     * Offer copy/move/linking options for a dragged external file. Perform the
     * chosen operation, if any.
     * 
     * @param fileName
     *            The name of the dragged file.
     * @param fileType
     *            The FileType associated with the file.
     * @param localFile
     *            Indicate whether this is a local file, or a remote file copied
     *            to a local temporary file.
     * @param mainTable
     *            The MainTable the file was dragged to.
     * @param dropRow
     *            The row where the file was dropped.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, boolean localFile,
        MainTable mainTable, int dropRow) {

        NamedCompound edits = new NamedCompound(Globals.lang("Drop %0", fileType.extension));

        if (tryXmpImport(fileName, fileType, localFile, mainTable, edits)) {
            panel.undoManager.addEdit(edits);
            return;
        }

        BibtexEntry entry = mainTable.getEntryAt(dropRow);

        // Show dialog
        boolean newEntry = false;
        boolean rename = entry.getCiteKey() != null && entry.getCiteKey().length() > 0;
        String citeKeyOrReason = (rename ? entry.getCiteKey() : Globals.lang("Entry has no citekey"));
        int reply = showLinkMoveCopyRenameDialog(Globals.lang("Link to file %0", fileName),
            fileType, rename, citeKeyOrReason, newEntry, false);

        if (reply != JOptionPane.OK_OPTION)
            return;

        /*
         * Ok, we're ready to go. See first if we need to do a file copy before
         * linking:
         */
        boolean success = true;
        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = fileName;
        } else {
            destFilename = (renameCheckBox.isSelected() ? entry.getCiteKey() + "." + fileType.extension : fileName);
            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doRename(fileName, fileType, destFilename, edits);
            }
        }

        if (success) {
            doLink(entry, fileType, destFilename, edits);
            panel.markBaseChanged();
        }

        panel.undoManager.addEdit(edits);

    }

    private boolean tryXmpImport(String fileName, ExternalFileType fileType, boolean localFile,
        MainTable mainTable, NamedCompound edits) {

        if (!fileType.extension.equals("pdf")) {
            return false;
        }

        List xmpEntriesInFile = null;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (Exception e) {
            return false;
        }

        if ((xmpEntriesInFile == null) || (xmpEntriesInFile.size() == 0)) {
            return false;
        }

        JLabel confirmationMessage = new JLabel(
            Globals
                .lang("The PDF contains one or several bibtex-records.\nDo you want to import these as new entries into the current database?"));

        int reply = JOptionPane.showConfirmDialog(frame, confirmationMessage, Globals.lang(
            "XMP metadata found in PDF: %0", fileName), JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (reply == JOptionPane.CANCEL_OPTION) {
            return true; // The user canceled thus that we are done.
        }
        if (reply == JOptionPane.NO_OPTION) {
            return false;
        }

        // reply == JOptionPane.YES_OPTION)

        /*
         * TODO Extract Import functionality from ImportMenuItem then we could
         * do:
         * 
         * ImportMenuItem importer = new ImportMenuItem(frame, (mainTable ==
         * null), new PdfXmpImporter());
         * 
         * importer.automatedImport(new String[] { fileName });
         */

        boolean isSingle = xmpEntriesInFile.size() == 1;
        BibtexEntry single = (isSingle ? (BibtexEntry) xmpEntriesInFile.get(0) : null);

        reply = showLinkMoveCopyRenameDialog(Globals.lang("Link to PDF %0", fileName), fileType,
            isSingle, (isSingle ? single.getCiteKey() : Globals.lang("Cannot rename for several entries.")),
            false, !isSingle);

        boolean success = true;

        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = fileName;
        } else {
            if (renameCheckBox.isSelected()) {
                destFilename = fileName;
            } else {
                destFilename = single.getCiteKey() + "." + fileType.extension;
            }

            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doRename(fileName, fileType, destFilename, edits);
            }
        }
        if (success) {

            Iterator it = xmpEntriesInFile.iterator();

            while (it.hasNext()) {
                try {
                    BibtexEntry entry = (BibtexEntry) it.next();
                    entry.setId(Util.createNeutralId());
                    panel.getDatabase().insertEntry(entry);
                    doLink(entry, fileType, destFilename, edits);
                } catch (KeyCollisionException ex) {

                }
            }
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
        }
        return true;
    }

    public int showLinkMoveCopyRenameDialog(String dialogTitle, ExternalFileType fileType,
        final boolean allowRename, String citekeyOrReason, boolean newEntry,
        final boolean multipleEntries) {
        
        String dir = panel.metaData().getFileDirectory(fileType.getFieldName());
        if ((dir == null) || !(new File(dir)).exists()) {
            destDirLabel.setText(Globals.lang("%0 directory is not set or does not exist!", fileType.getName()));
            copyRadioButton.setEnabled(false);
            moveRadioButton.setEnabled(false);
            linkInPlace.setSelected(true);
        } else {
            destDirLabel.setText(Globals.lang("%0 directory is '%1':", fileType.getName(), dir));
            copyRadioButton.setEnabled(true);
            moveRadioButton.setEnabled(true);
        }
        
        ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				renameCheckBox.setEnabled(!linkInPlace.isSelected()
						&& allowRename && (!multipleEntries));
			}
		};

		if (multipleEntries) {
			linkInPlace.setText(Globals
					.lang("Leave files in their current directory."));
			copyRadioButton.setText(Globals.lang("Copy files to %0.", fileType
					.getName()));

			moveRadioButton.setText(Globals.lang("Move files to %0.", fileType
					.getName()));
		} else {
			linkInPlace.setText(Globals
					.lang("Leave file in its current directory."));
			copyRadioButton.setText(Globals.lang("Copy file to %0.", fileType
					.getName()));
			moveRadioButton.setText(Globals.lang("Move file to %0.", fileType
					.getName()));
		}
		
        renameCheckBox.setText(Globals.lang("Rename to match citekey") + ": " + citekeyOrReason);
        linkInPlace.addChangeListener(cl);
        cl.stateChanged(new ChangeEvent(linkInPlace));

        try {
            return JOptionPane.showConfirmDialog(frame, optionsPanel, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        } finally {
            linkInPlace.removeChangeListener(cl);
        }
    }
    
    /**
     * Make a extension to the file.
     * 
     * @param entry
     *            The entry to extension from.
     * @param fileType
     *            The FileType associated with the file.
     * @param filename
     *            The path to the file.
     * @param edits
     *            An NamedCompound action this action is to be added to. If none
     *            is given, the edit is added to the panel's undoManager.
     */
    private void doLink(BibtexEntry entry, ExternalFileType fileType, String filename,
        NamedCompound edits) {

        UndoableFieldChange edit = new UndoableFieldChange(entry, fileType.getFieldName(), entry
            .getField(fileType.getFieldName()), filename);
        entry.setField(fileType.getFieldName(), filename);

        if (edits == null) {
            panel.undoManager.addEdit(edit);
        } else {
            edits.addEdit(edit);
        }
    }

    /**
     * Move the given file to the base directory for its file type, and rename
     * it to the given filename.
     * 
     * @param fileName
     *            The name of the source file.
     * @param fileType
     *            The FileType associated with the file.
     * @param destFilename
     *            The destination filename.
     * @param edits
     *            TODO we should be able to undo this action
     * @return true if the operation succeeded.
     */
    private boolean doRename(String fileName, ExternalFileType fileType, String destFilename,
        NamedCompound edits) {
        String dir = panel.metaData().getFileDirectory(fileType.getFieldName());
        if ((dir == null) || !(new File(dir)).exists()) {
            // OOps, we don't know which directory to put it in, or the given
            // dir doesn't exist....
            // This should not happen!!
            return false;
        }
        destFilename = new File(destFilename).getName();
        File f = new File(fileName);
        File destFile = new File(new StringBuffer(dir).append(System.getProperty("file.separator"))
            .append(destFilename).toString());
        f.renameTo(destFile);
        return true;
    }

    /**
     * Copy the given file to the base directory for its file type, and give it
     * the given name.
     * 
     * @param fileName
     *            The name of the source file.
     * @param fileType
     *            The FileType associated with the file.
     * @param toFile
     *            The destination filename. An existing path-component will be removed.
     * @param edits
     *            TODO we should be able to undo this!
     * @return
     */
    private boolean doCopy(String fileName, ExternalFileType fileType, String toFile,
        NamedCompound edits) {

        String dir = panel.metaData().getFileDirectory(fileType.getFieldName());
        if ((dir == null) || !(new File(dir)).exists()) {
            // OOps, we don't know which directory to put it in, or the given
            // dir doesn't exist....
            System.out.println("dir: " + dir + "\t ext: " + fileType.getExtension());
            return false;
        }
        toFile = new File(toFile).getName();
        
        File destFile = new File(new StringBuffer(dir).append(System.getProperty("file.separator"))
            .append(toFile).toString());
        if (destFile.equals(new File(fileName))){
            // File is already in the correct position. Don't override!
            return true;
        }
        
        if (destFile.exists()) {
            int answer = JOptionPane.showConfirmDialog(frame, "'" + destFile.getPath() + "' "
                + Globals.lang("exists.Overwrite?"), Globals.lang("File exists"),
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
