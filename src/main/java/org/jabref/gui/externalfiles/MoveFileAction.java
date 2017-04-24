package org.jabref.gui.externalfiles;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.fieldeditors.FileListEditor;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;

/**
 * Action for moving a file that is linked  from an entry in JabRef.
 */
public class MoveFileAction extends AbstractAction {

    private final JabRefFrame frame;
    private final EntryEditor eEditor;
    private final FileListEditor editor;

    public MoveFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor) {
        this.frame = frame;
        this.eEditor = eEditor;
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        int selected = editor.getSelectedRow();

        if (selected == -1) {
            return;
        }

        FileListEntry entry = editor.getTableModel().getEntry(selected);
        LinkedFile field = entry.toParsedFileField();

        if (field.isOnlineLink()) {
            // TODO: notify that this operation cannot be done on remote links
            return;
        }

        // Get an absolute path representation:
        Optional<Path> fileDir = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());
        if (!fileDir.isPresent()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File directory is not set or does not exist!"),
                    Localization.lang("Move file"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if the current file exists:
        Optional<Path> file = field.findIn(frame.getCurrentBasePanel().getBibDatabaseContext(), Globals.prefs.getFileDirectoryPreferences());
        if ((file.isPresent()) && Files.exists(file.get())) {

            MoveFilesCleanup moveFiles = new MoveFilesCleanup(frame.getCurrentBasePanel().getBibDatabaseContext(),
                    Globals.prefs.getCleanupPreferences(Globals.journalAbbreviationLoader).getFileDirPattern(),
                    Globals.prefs.getFileDirectoryPreferences(),
                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader), field);

            String[] options = {Localization.lang("Move file"), Localization.lang("Cancel")};

            int dialogResult = JOptionPane.showOptionDialog(frame,
                    Localization.lang("Move file to file directory?") + " " + fileDir.get(),
                    Localization.lang("Move file"),
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[0]);

            if (dialogResult == JOptionPane.YES_OPTION) {
                moveFiles.cleanup((eEditor.getEntry()));
            }

        } else {
            // File doesn't exist, so we can't move it.
            JOptionPane.showMessageDialog(frame, Localization.lang("Could not find file '%0'.", entry.getLink()),
                    Localization.lang("File not found"), JOptionPane.ERROR_MESSAGE);
        }

    }
}
