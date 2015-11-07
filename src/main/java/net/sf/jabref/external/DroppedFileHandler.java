/*  Copyright (C) 2003-2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.external;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.logic.id.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.util.Util;
import net.sf.jabref.logic.xmp.XMPUtil;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class holds the functionality of autolinking to a file that's dropped
 * onto an entry.
 * <p>
 * Options for handling the files are:
 * <p>
 * 1) Link to the file in its current position (disabled if the file is remote)
 * <p>
 * 2) Copy the file to ??? directory, rename after bibtex key, and extension
 * <p>
 * 3) Move the file to ??? directory, rename after bibtex key, and extension
 */
public class DroppedFileHandler {

    public static final String DFH_LEAVE = "DroppedFileHandler_LeaveFileInDir";
    public static final String DFH_COPY = "DroppedFileHandler_CopyFile";
    public static final String DFH_MOVE = "DroppedFileHandler_MoveFile";
    public static final String DFH_RENAME = "DroppedFileHandler_RenameFile";

    private final JabRefFrame frame;

    private final BasePanel panel;

    private final JRadioButton linkInPlace = new JRadioButton();
    private final JRadioButton copyRadioButton = new JRadioButton();
    private final JRadioButton moveRadioButton = new JRadioButton();

    private final JLabel destDirLabel = new JLabel();

    private final JCheckBox renameCheckBox = new JCheckBox();

    private final JTextField renameToTextBox = new JTextField(50);

    private final JPanel optionsPanel = new JPanel();


    public DroppedFileHandler(JabRefFrame frame, BasePanel panel) {

        this.frame = frame;
        this.panel = panel;

        ButtonGroup grp = new ButtonGroup();
        grp.add(linkInPlace);
        grp.add(copyRadioButton);
        grp.add(moveRadioButton);

        FormLayout layout = new FormLayout("left:15dlu,pref,pref,pref", "bottom:14pt,pref,pref,pref,pref");
        layout.setRowGroups(new int[][]{{1, 2, 3, 4, 5}});
        FormBuilder builder = FormBuilder.create().layout(layout);

        builder.add(linkInPlace).xyw(1, 1, 4);
        builder.add(destDirLabel).xyw(1, 2, 4);
        builder.add(copyRadioButton).xyw(2, 3, 3);
        builder.add(moveRadioButton).xyw(2, 4, 3);
        builder.add(renameCheckBox).xyw(2, 5, 1);
        builder.add(renameToTextBox).xyw(4, 5, 1);
        optionsPanel.add(builder.getPanel());
    }

    /**
     * Offer copy/move/linking options for a dragged external file. Perform the
     * chosen operation, if any.
     *
     * @param fileName  The name of the dragged file.
     * @param fileType  The FileType associated with the file.
     * @param localFile Indicate whether this is a local file, or a remote file copied
     *                  to a local temporary file.
     * @param mainTable The MainTable the file was dragged to.
     * @param dropRow   The row where the file was dropped.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, boolean localFile,
                                  MainTable mainTable, int dropRow) {

        BibtexEntry entry = mainTable.getEntryAt(dropRow);
        handleDroppedfile(fileName, fileType, localFile, entry);
    }

    /**
     * @param fileName  The name of the dragged file.
     * @param fileType  The FileType associated with the file.
     * @param localFile Indicate whether this is a local file, or a remote file copied
     *                  to a local temporary file.
     * @param entry     The target entry for the drop.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, boolean localFile,
                                  BibtexEntry entry) {
        NamedCompound edits = new NamedCompound(Localization.lang("Drop %0", fileType.extension));

        if (tryXmpImport(fileName, fileType, localFile, edits)) {
            edits.end();
            panel.undoManager.addEdit(edits);
            return;
        }

        // Show dialog
        boolean newEntry = false;
        if (!showLinkMoveCopyRenameDialog(fileName, fileType, entry, newEntry, false, panel.database())) {
            return;
        }

        /*
         * Ok, we're ready to go. See first if we need to do a file copy before
         * linking:
         */

        boolean success = true;
        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(new File(fileName), panel.metaData().getFileDirectory(Globals.FILE_FIELD)).toString();
        } else {
            destFilename = renameCheckBox.isSelected() ? renameToTextBox.getText() : new File(fileName).getName();
            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, fileType, destFilename, edits);
            }
        }

        if (success) {
            doLink(entry, fileType, destFilename, false, edits);
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
        }
        edits.end();
        panel.undoManager.addEdit(edits);

    }

    // Done by MrDlib
    public void linkPdfToEntry(String fileName, MainTable entryTable, int dropRow) {
        BibtexEntry entry = entryTable.getEntryAt(dropRow);
        linkPdfToEntry(fileName, entryTable, entry);
    }

    public void linkPdfToEntry(String fileName, MainTable entryTable, BibtexEntry entry) {
        ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("pdf");
        NamedCompound edits = new NamedCompound(Localization.lang("Drop %0", fileType.extension));

        // Show dialog
        boolean newEntry = false;
        if (!showLinkMoveCopyRenameDialog(fileName, fileType, entry, newEntry, false, panel.database())) {
            return;
        }

        /*
         * Ok, we're ready to go. See first if we need to do a file copy before
         * linking:
         */

        boolean success = true;
        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(new File(fileName), panel.metaData().getFileDirectory(Globals.FILE_FIELD)).toString();
        } else {
            destFilename = renameCheckBox.isSelected() ? renameToTextBox.getText() : new File(fileName).getName();
            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, fileType, destFilename, edits);
            }
        }

        if (success) {
            doLink(entry, fileType, destFilename, false, edits);
            panel.markBaseChanged();
        }
        edits.end();
        panel.undoManager.addEdit(edits);
    }

    public void importXmp(List<BibtexEntry> xmpEntriesInFile, String fileName) {
        ExternalFileType fileType = Globals.prefs.getExternalFileTypeByExt("pdf");
        NamedCompound edits = new NamedCompound(Localization.lang("Drop %0", fileType.extension));

        boolean isSingle = xmpEntriesInFile.size() == 1;
        BibtexEntry single = isSingle ? xmpEntriesInFile.get(0) : null;

        boolean success = true;

        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(new File(fileName), panel.metaData().getFileDirectory(Globals.FILE_FIELD)).toString();
        } else {
            if (renameCheckBox.isSelected()) {
                destFilename = fileName;
            } else {
                destFilename = single.getCiteKey() + "." + fileType.extension;
            }

            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, fileType, destFilename, edits);
            }
        }
        if (success) {

            for (BibtexEntry aXmpEntriesInFile : xmpEntriesInFile) {

                aXmpEntriesInFile.setId(IdGenerator.next());
                edits.addEdit(new UndoableInsertEntry(panel.getDatabase(), aXmpEntriesInFile, panel));
                panel.getDatabase().insertEntry(aXmpEntriesInFile);
                doLink(aXmpEntriesInFile, fileType, destFilename, true, edits);

            }
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
        }
        edits.end();
        panel.undoManager.addEdit(edits);
    }

    // Done by MrDlib

    private boolean tryXmpImport(String fileName, ExternalFileType fileType, boolean localFile,
                                 NamedCompound edits) {

        if (!fileType.extension.equals("pdf")) {
            return false;
        }

        List<BibtexEntry> xmpEntriesInFile;
        try {
            xmpEntriesInFile = XMPUtil.readXMP(fileName);
        } catch (Exception e) {
            return false;
        }

        if ((xmpEntriesInFile == null) || xmpEntriesInFile.isEmpty()) {
            return false;
        }

        JLabel confirmationMessage = new JLabel(
                Localization.lang("The PDF contains one or several bibtex-records.\nDo you want to import these as new entries into the current database?"));

        int reply = JOptionPane.showConfirmDialog(frame, confirmationMessage,
                Localization.lang("XMP metadata found in PDF: %0", fileName), JOptionPane.YES_NO_CANCEL_OPTION,
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
        BibtexEntry single = isSingle ? xmpEntriesInFile.get(0) : null;

        boolean success = true;

        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(new File(fileName), panel.metaData().getFileDirectory(Globals.FILE_FIELD)).toString();
        } else {
            if (renameCheckBox.isSelected()) {
                destFilename = fileName;
            } else {
                destFilename = single.getCiteKey() + "." + fileType.extension;
            }

            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, fileType, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, fileType, destFilename, edits);
            }
        }
        if (success) {

            for (BibtexEntry aXmpEntriesInFile : xmpEntriesInFile) {

                aXmpEntriesInFile.setId(IdGenerator.next());
                edits.addEdit(new UndoableInsertEntry(panel.getDatabase(), aXmpEntriesInFile, panel));
                panel.getDatabase().insertEntry(aXmpEntriesInFile);
                doLink(aXmpEntriesInFile, fileType, destFilename, true, edits);

            }
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
        }
        return true;
    }

    //
    // @return true if user pushed "Ok", false otherwise
    //
    private boolean showLinkMoveCopyRenameDialog(String linkFileName, ExternalFileType fileType,
                                                 BibtexEntry entry, boolean newEntry, final boolean multipleEntries, BibtexDatabase database) {

        String dialogTitle = Localization.lang("Link to file %0", linkFileName);
        String[] dirs = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
        int found = -1;
        for (int i = 0; i < dirs.length; i++) {
            if (new File(dirs[i]).exists()) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            destDirLabel.setText(Localization.lang("File directory is not set or does not exist!"));
            copyRadioButton.setEnabled(false);
            moveRadioButton.setEnabled(false);
            renameToTextBox.setEnabled(false);
            renameCheckBox.setEnabled(false);
            linkInPlace.setSelected(true);
        } else {
            destDirLabel.setText(Localization.lang("File directory is '%0':", dirs[found]));
            copyRadioButton.setEnabled(true);
            moveRadioButton.setEnabled(true);
            renameToTextBox.setEnabled(true);
            renameCheckBox.setEnabled(true);
        }

        ChangeListener cl = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent arg0) {
                renameCheckBox.setEnabled(!linkInPlace.isSelected()
                        && !multipleEntries);
                renameToTextBox.setEnabled(!linkInPlace.isSelected()
                        && !multipleEntries);
                if (multipleEntries) {
                    renameToTextBox.setText("Multiple entries");
                }
            }
        };

        if (multipleEntries) {
            linkInPlace.setText(Localization.lang("Leave files in their current directory."));
            copyRadioButton.setText(Localization.lang("Copy files to file directory."));

            moveRadioButton.setText(Localization.lang("Move files to file directory."));
        } else {
            linkInPlace.setText(Localization.lang("Leave file in its current directory."));
            copyRadioButton.setText(Localization.lang("Copy file to file directory."));
            moveRadioButton.setText(Localization.lang("Move file to file directory."));
        }

        renameCheckBox.setText(Localization.lang("Rename file to").concat(": "));

        // Determine which name to suggest:
        String targetName = Util.getLinkedFileName(database, entry);

        renameToTextBox.setText(targetName.concat(".").concat(fileType.extension));

        linkInPlace.setSelected(frame.prefs().getBoolean(DroppedFileHandler.DFH_LEAVE));
        copyRadioButton.setSelected(frame.prefs().getBoolean(DroppedFileHandler.DFH_COPY));
        moveRadioButton.setSelected(frame.prefs().getBoolean(DroppedFileHandler.DFH_MOVE));
        renameCheckBox.setSelected(frame.prefs().getBoolean(DroppedFileHandler.DFH_RENAME));

        linkInPlace.addChangeListener(cl);
        cl.stateChanged(new ChangeEvent(linkInPlace));

        try {
            Object[] messages = {Localization.lang("How would you like to link to '%0'?", linkFileName),
                    optionsPanel};
            int reply = JOptionPane.showConfirmDialog(frame, messages, dialogTitle,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (reply == JOptionPane.OK_OPTION) {
                // store user's choice
                frame.prefs().putBoolean(DroppedFileHandler.DFH_LEAVE, linkInPlace.isSelected());
                frame.prefs().putBoolean(DroppedFileHandler.DFH_COPY, copyRadioButton.isSelected());
                frame.prefs().putBoolean(DroppedFileHandler.DFH_MOVE, moveRadioButton.isSelected());
                frame.prefs().putBoolean(DroppedFileHandler.DFH_RENAME, renameCheckBox.isSelected());
                return true;
            } else {
                return false;
            }
        } finally {
            linkInPlace.removeChangeListener(cl);
        }
    }

    /**
     * Make a extension to the file.
     *
     * @param entry    The entry to extension from.
     * @param fileType The FileType associated with the file.
     * @param filename The path to the file.
     * @param edits    An NamedCompound action this action is to be added to. If none
     *                 is given, the edit is added to the panel's undoManager.
     */
    private void doLink(BibtexEntry entry, ExternalFileType fileType, String filename,
                        boolean avoidDuplicate, NamedCompound edits) {

        String oldValue = entry.getField(Globals.FILE_FIELD);
        FileListTableModel tm = new FileListTableModel();
        if (oldValue != null) {
            tm.setContent(oldValue);
        }

        // If avoidDuplicate==true, we should check if this file is already linked:
        if (avoidDuplicate) {
            // For comparison, find the absolute filename:
            String[] dirs = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
            String absFilename = !new File(filename).isAbsolute() && (dirs.length > 0) ?
                    FileUtil.expandFilename(filename, dirs).getAbsolutePath() : filename;

            for (int i = 0; i < tm.getRowCount(); i++) {
                FileListEntry flEntry = tm.getEntry(i);
                // Find the absolute filename for this existing link:
                String absName = !new File(flEntry.getLink()).isAbsolute() && (dirs.length > 0) ?
                        FileUtil.expandFilename(flEntry.getLink(), dirs).getAbsolutePath() : flEntry.getLink();
                System.out.println("absName: " + absName);
                // If the filenames are equal, we don't need to link, so we simply return:
                if (absFilename.equals(absName)) {
                    return;
                }
            }
        }

        tm.addEntry(tm.getRowCount(), new FileListEntry("", filename, fileType));
        String newValue = tm.getStringRepresentation();
        UndoableFieldChange edit = new UndoableFieldChange(entry, Globals.FILE_FIELD,
                oldValue, newValue);
        entry.setField(Globals.FILE_FIELD, newValue);

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
     * @param fileName     The name of the source file.
     * @param fileType     The FileType associated with the file.
     * @param destFilename The destination filename.
     * @param edits        TODO we should be able to undo this action
     * @return true if the operation succeeded.
     */
    private boolean doMove(String fileName, ExternalFileType fileType, String destFilename,
                           NamedCompound edits) {
        String[] dirs = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
        int found = -1;
        for (int i = 0; i < dirs.length; i++) {
            if (new File(dirs[i]).exists()) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            // OOps, we don't know which directory to put it in, or the given
            // dir doesn't exist....
            // This should not happen!!
            return false;
        }
        File fromFile = new File(fileName);
        File toFile = new File(dirs[found] + System.getProperty("file.separator") + destFilename);
        if (toFile.exists()) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    toFile.getAbsolutePath() + " exists. Overwrite?", "Overwrite file?",
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.NO_OPTION) {
                return false;
            }
        }

        if (!fromFile.renameTo(toFile)) {
            JOptionPane.showMessageDialog(frame,
                    "There was an error moving the file. Please move the file manually and link in place.",
                    "Error moving file", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }

    }

    /**
     * Copy the given file to the base directory for its file type, and give it
     * the given name.
     *
     * @param fileName The name of the source file.
     * @param fileType The FileType associated with the file.
     * @param toFile   The destination filename. An existing path-component will be removed.
     * @param edits    TODO we should be able to undo this!
     * @return
     */
    private boolean doCopy(String fileName, ExternalFileType fileType, String toFile,
                           NamedCompound edits) {

        String[] dirs = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
        int found = -1;
        for (int i = 0; i < dirs.length; i++) {
            if (new File(dirs[i]).exists()) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            // OOps, we don't know which directory to put it in, or the given
            // dir doesn't exist....
            System.out.println("dir: " + dirs[found] + "\t ext: " + fileType.getExtension());
            return false;
        }
        toFile = new File(toFile).getName();

        File destFile = new File(dirs[found] + System.getProperty("file.separator") + toFile);
        if (destFile.equals(new File(fileName))) {
            // File is already in the correct position. Don't override!
            return true;
        }

        if (destFile.exists()) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    "'" + destFile.getPath() + "' " + Localization.lang("exists. Overwrite?"),
                    Localization.lang("File exists"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        try {
            FileUtil.copyFile(new File(fileName), destFile, true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
