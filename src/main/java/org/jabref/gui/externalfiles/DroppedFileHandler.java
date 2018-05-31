package org.jabref.gui.externalfiles;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.IdGenerator;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(DroppedFileHandler.class);

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
        layout.setRowGroups(new int[][] {{1, 2, 3, 4, 5}});
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
     * @param mainTable The MainTable the file was dragged to.
     * @param dropRow   The row where the file was dropped.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, MainTable mainTable, int dropRow) {

        BibEntry entry = mainTable.getEntryAt(dropRow);
        handleDroppedfile(fileName, fileType, entry);
    }

    /**
     * @param fileName  The name of the dragged file.
     * @param fileType  The FileType associated with the file.
     * @param entry     The target entry for the drop.
     */
    public void handleDroppedfile(String fileName, ExternalFileType fileType, BibEntry entry) {
        NamedCompound edits = new NamedCompound(Localization.lang("Drop %0", fileType.getExtension()));

        if (tryXmpImport(fileName, fileType, edits)) {
            edits.end();
            panel.getUndoManager().addEdit(edits);
            return;
        }

        // Show dialog
        if (!showLinkMoveCopyRenameDialog(fileName, fileType, entry, panel.getDatabase())) {
            return;
        }

        /*
         * Ok, we're ready to go. See first if we need to do a file copy before
         * linking:
         */

        boolean success = true;
        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(Paths.get(fileName),
                    panel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()))
                    .toString();
        } else {
            destFilename = renameCheckBox.isSelected() ? renameToTextBox.getText() : Paths.get(fileName).toString();
            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, destFilename, edits);
            }
        }

        if (success) {
            doLink(entry, fileType, destFilename, false, edits);
            panel.markBaseChanged();
            panel.updateEntryEditorIfShowing();
        }
        edits.end();
        panel.getUndoManager().addEdit(edits);

    }

    // Done by MrDlib
    public void linkPdfToEntry(String fileName, MainTable entryTable, int dropRow) {
        BibEntry entry = entryTable.getEntryAt(dropRow);
        linkPdfToEntry(fileName, entry);
    }

    public void linkPdfToEntry(String fileName, BibEntry entry) {
        Optional<ExternalFileType> optFileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("pdf");

        if (!optFileType.isPresent()) {
            LOGGER.warn("No file type with extension 'pdf' registered.");
            return;
        }

        ExternalFileType fileType = optFileType.get();
        // Show dialog
        if (!showLinkMoveCopyRenameDialog(fileName, fileType, entry, panel.getDatabase())) {
            return;
        }

        /*
         * Ok, we're ready to go. See first if we need to do a file copy before
         * linking:
         */

        boolean success = true;
        String destFilename;
        NamedCompound edits = new NamedCompound(Localization.lang("Drop %0", fileType.getExtension()));

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(Paths.get(fileName),
                    panel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()))
                    .toString();
        } else {
            destFilename = renameCheckBox.isSelected() ? renameToTextBox.getText() : new File(fileName).getName();
            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, destFilename, edits);
            }
        }

        if (success) {
            doLink(entry, fileType, destFilename, false, edits);
            panel.markBaseChanged();
        }
        edits.end();
        panel.getUndoManager().addEdit(edits);
    }

    // Done by MrDlib

    private boolean tryXmpImport(String fileName, ExternalFileType fileType, NamedCompound edits) {

        if (!"pdf".equals(fileType.getExtension())) {
            return false;
        }

        List<BibEntry> xmpEntriesInFile;
        try {
            xmpEntriesInFile = XmpUtilReader.readXmp(fileName, Globals.prefs.getXMPPreferences());
        } catch (IOException e) {
            LOGGER.warn("Problem reading XMP", e);
            return false;
        }

        if ((xmpEntriesInFile == null) || xmpEntriesInFile.isEmpty()) {
            return false;
        }

        JLabel confirmationMessage = new JLabel(Localization.lang("The PDF contains one or several BibTeX-records.")
                + "\n" + Localization.lang("Do you want to import these as new entries into the current library?"));
        JPanel entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        xmpEntriesInFile.forEach(entry -> {
            JTextArea entryArea = new JTextArea(entry.toString());
            entryArea.setEditable(false);
            entriesPanel.add(entryArea);
        });

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(confirmationMessage, BorderLayout.NORTH);
        contentPanel.add(entriesPanel, BorderLayout.CENTER);

        int reply = JOptionPane.showConfirmDialog(frame, contentPanel,
                Localization.lang("XMP-metadata found in PDF: %0", fileName), JOptionPane.YES_NO_CANCEL_OPTION,
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
        BibEntry single = isSingle ? xmpEntriesInFile.get(0) : null;

        boolean success = true;

        String destFilename;

        if (linkInPlace.isSelected()) {
            destFilename = FileUtil.shortenFileName(Paths.get(fileName),
                    panel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences()))
                    .toString();
        } else {
            if (renameCheckBox.isSelected() || (single == null)) {
                destFilename = fileName;
            } else {
                destFilename = single.getCiteKey() + "." + fileType.getExtension();
            }

            if (copyRadioButton.isSelected()) {
                success = doCopy(fileName, destFilename, edits);
            } else if (moveRadioButton.isSelected()) {
                success = doMove(fileName, destFilename, edits);
            }
        }
        if (success) {

            for (BibEntry aXmpEntriesInFile : xmpEntriesInFile) {

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
    // @return true if user pushed "OK", false otherwise
    //
    private boolean showLinkMoveCopyRenameDialog(String linkFileName, ExternalFileType fileType, BibEntry entry,
            BibDatabase database) {

        String dialogTitle = Localization.lang("Link to file %0", linkFileName);

        Optional<Path> dir = panel.getBibDatabaseContext()
                .getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());

        if (!dir.isPresent()) {
            destDirLabel.setText(Localization.lang("File directory is not set or does not exist!"));
            copyRadioButton.setEnabled(false);
            moveRadioButton.setEnabled(false);
            renameToTextBox.setEnabled(false);
            renameCheckBox.setEnabled(false);
            linkInPlace.setSelected(true);
        } else {
            destDirLabel.setText(Localization.lang("File directory is '%0':", dir.get().toString()));
            copyRadioButton.setEnabled(true);
            moveRadioButton.setEnabled(true);
            renameToTextBox.setEnabled(true);
            renameCheckBox.setEnabled(true);
        }

        ChangeListener cl = arg0 -> {
            renameCheckBox.setEnabled(!linkInPlace.isSelected());
            renameToTextBox.setEnabled(!linkInPlace.isSelected());
        };

        linkInPlace.setText(Localization.lang("Leave file in its current directory"));
        copyRadioButton.setText(Localization.lang("Copy file to file directory"));
        moveRadioButton.setText(Localization.lang("Move file to file directory"));
        renameCheckBox.setText(Localization.lang("Rename file to").concat(": "));

        // Determine which name to suggest:
        String targetName = FileUtil.createFileNameFromPattern(database, entry,
                Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));

        String fileDirPattern = Globals.prefs.get(JabRefPreferences.IMPORT_FILEDIRPATTERN);

        String targetDirName = "";
        if (!fileDirPattern.isEmpty()) {
            targetDirName = FileUtil.createDirNameFromPattern(database, entry, fileDirPattern);
        }

        if (targetDirName.isEmpty()) {
            renameToTextBox.setText(targetName.concat(".").concat(fileType.getExtension()));
        } else {
            renameToTextBox
                    .setText(targetDirName.concat("/").concat(targetName.concat(".").concat(fileType.getExtension())));
        }
        linkInPlace.setSelected(frame.prefs().getBoolean(JabRefPreferences.DROPPEDFILEHANDLER_LEAVE));
        copyRadioButton.setSelected(frame.prefs().getBoolean(JabRefPreferences.DROPPEDFILEHANDLER_COPY));
        moveRadioButton.setSelected(frame.prefs().getBoolean(JabRefPreferences.DROPPEDFILEHANDLER_MOVE));
        renameCheckBox.setSelected(frame.prefs().getBoolean(JabRefPreferences.DROPPEDFILEHANDLER_RENAME));

        linkInPlace.addChangeListener(cl);
        cl.stateChanged(new ChangeEvent(linkInPlace));

        try {
            Object[] messages = {Localization.lang("How would you like to link to '%0'?", linkFileName), optionsPanel};
            int reply = JOptionPane.showConfirmDialog(frame, messages, dialogTitle, JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (reply == JOptionPane.OK_OPTION) {
                // store user's choice
                frame.prefs().putBoolean(JabRefPreferences.DROPPEDFILEHANDLER_LEAVE, linkInPlace.isSelected());
                frame.prefs().putBoolean(JabRefPreferences.DROPPEDFILEHANDLER_COPY, copyRadioButton.isSelected());
                frame.prefs().putBoolean(JabRefPreferences.DROPPEDFILEHANDLER_MOVE, moveRadioButton.isSelected());
                frame.prefs().putBoolean(JabRefPreferences.DROPPEDFILEHANDLER_RENAME, renameCheckBox.isSelected());
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
    private void doLink(BibEntry entry, ExternalFileType fileType, String filename, boolean avoidDuplicate,
            NamedCompound edits) {

        Optional<String> oldValue = entry.getField(FieldName.FILE);
        FileListTableModel tm = new FileListTableModel();
        oldValue.ifPresent(tm::setContent);

        // If avoidDuplicate==true, we should check if this file is already linked:
        if (avoidDuplicate) {
            // For comparison, find the absolute filename:
            List<Path> dirs = panel.getBibDatabaseContext()
                    .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
            String absFilename;
            if (new File(filename).isAbsolute() || dirs.isEmpty()) {
                absFilename = filename;
            } else {
                Optional<Path> file = FileHelper.expandFilenameAsPath(filename, dirs);
                if (file.isPresent()) {
                    absFilename = file.get().toAbsolutePath().toString();
                } else {
                    absFilename = ""; // This shouldn't happen based on the old code, so maybe one should set it something else?
                }
            }

            LOGGER.debug("absFilename: " + absFilename);

            for (int i = 0; i < tm.getRowCount(); i++) {
                FileListEntry flEntry = tm.getEntry(i);
                // Find the absolute filename for this existing link:
                String absName = flEntry.toParsedFileField()
                        .findIn(dirs)
                        .map(Path::toAbsolutePath)
                        .map(Path::toString)
                        .orElse(null);

                LOGGER.debug("absName: " + absName);
                // If the filenames are equal, we don't need to link, so we simply return:
                if (absFilename.equals(absName)) {
                    return;
                }
            }
        }

        tm.addEntry(tm.getRowCount(), new FileListEntry("", filename, fileType));
        String newValue = tm.getStringRepresentation();
        UndoableFieldChange edit = new UndoableFieldChange(entry, FieldName.FILE, oldValue.orElse(null), newValue);

        // make sure that the update runs in the Java FX thread to avoid exception in listeners
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            entry.setField(FieldName.FILE, newValue);
        });

        if (edits == null) {
            panel.getUndoManager().addEdit(edit);
        } else {
            edits.addEdit(edit);
        }
    }

    /**
     * Move the given file to the base directory for its file type, and rename
     * it to the given filename.
     *
     * @param fileName     The name of the source file.
     * @param destFilename The destination filename.
     * @param edits        TODO we should be able to undo this action
     * @return true if the operation succeeded.
     */
    private boolean doMove(String fileName, String destFilename, NamedCompound edits) {
        Optional<Path> dir = panel.getBibDatabaseContext()
                .getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());

        if (dir.isPresent()) {
            Path destFile = dir.get().resolve(destFilename);

            if (Files.exists(destFile)) {
                int answer = JOptionPane.showConfirmDialog(frame,
                        Localization.lang("'%0' exists. Overwrite file?", destFile.toString()),
                        Localization.lang("Overwrite file?"), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.NO_OPTION) {
                    return false;
                }
            }

            Path fromFile = Paths.get(fileName);
            try {
                if (!Files.exists(destFile)) {
                    Files.createDirectories(destFile);
                }
            } catch (IOException e) {
                LOGGER.error("Problem creating target directories", e);
            }
            if (FileUtil.renameFile(fromFile, destFile, true)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(frame,
                        Localization.lang("Could not move file '%0'.", destFile.toString())
                                + Localization.lang("Please move the file manually and link in place."),
                        Localization.lang("Move file failed"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }

    /**
     * Copy the given file to the base directory for its file type, and give it
     * the given name.
     *
     * @param fileName The name of the source file.
     * @param toFile   The destination filename. An existing path-component will be removed.
     * @param edits    TODO we should be able to undo this!
     * @return true if the operation succeeded.
     */
    private boolean doCopy(String fileName, String toFile, NamedCompound edits) {

        List<String> dirs = panel.getBibDatabaseContext()
                .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
        int found = -1;
        for (int i = 0; i < dirs.size(); i++) {
            if (new File(dirs.get(i)).exists()) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            // OOps, we don't know which directory to put it in, or the given
            // dir doesn't exist....
            // This should not happen!!
            LOGGER.warn("Cannot determine destination directory or destination directory does not exist");
            return false;
        }

        Path destFile = Paths.get(dirs.get(found)).resolve(toFile);
        if (destFile.toString().equals(fileName)) {
            // File is already in the correct position. Don't override!
            return true;
        }

        if (Files.exists(destFile)) {
            int answer = JOptionPane.showConfirmDialog(frame,
                    Localization.lang("'%0' exists. Overwrite file?", destFile.toString()),
                    Localization.lang("File exists"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        try {
            //copy does not create directories, therefore we have to create them manually
            if (!Files.exists(destFile)) {
                Files.createDirectories(destFile);
            }
            FileUtil.copyFile(Paths.get(fileName), destFile, true);
        } catch (IOException e) {
            LOGGER.error("Problem copying file", e);
            return false;
        }
        return true;
    }

}
