package org.jabref.gui.externalfiles;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.fieldeditors.FileListEditor;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.ParsedFileField;

public class RenameFileAction extends AbstractAction {

    private final JabRefFrame frame;
    private final EntryEditor eEditor;
    private final FileListEditor editor;
    private final CleanupPreferences prefs = Globals.prefs.getCleanupPreferences(new JournalAbbreviationLoader());

    private static final String MOVE_RENAME = Localization.lang("Move/Rename file");

    public RenameFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor) {
        this.frame = frame;
        this.eEditor = eEditor;
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        int selected = editor.getSelectedRow();

        if (selected == -1) {
            return;
        }

        FileListEntry entry = editor.getTableModel().getEntry(selected);

        ParsedFileField field = entry.toParsedFileField();
        System.out.println("Parsed file Field " + field);
        // Check if the current file exists:
        String ln = entry.link;
        boolean httpLink = ln.toLowerCase(Locale.ENGLISH).startsWith("http");
        if (httpLink) {
            // TODO: notify that this operation cannot be done on remote links
            return;
        }
        List<String> dirs = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFileDirectories(prefs.getFileDirectoryPreferences());
        Optional<Path> fileDir = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFirstExistingFileDir(prefs.getFileDirectoryPreferences());
        if (!fileDir.isPresent()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File_directory_is_not_set_or_does_not_exist!"),
                    MOVE_RENAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path file = Paths.get(ln);
        if (!file.isAbsolute()) {
            file = FileUtil.expandFilename(ln, dirs).map(File::toPath).orElse(null);
        }

        if ((file != null) && Files.exists(file)) {
            System.out.println("Cleanup Rename of file " + file);

            RenamePdfCleanup pdfCleanup = new RenamePdfCleanup(false,
                    frame.getCurrentBasePanel().getBibDatabaseContext(), prefs.getFileNamePattern(),
                    prefs.getLayoutFormatterPreferences(),
                    prefs.getFileDirectoryPreferences(), field);

            String targetFileName = pdfCleanup.getTargetFileName(field, eEditor.getEntry());
            System.out.println("TargetFileName " + targetFileName);

            pdfCleanup.cleanup(eEditor.getEntry());

        }
    }

}
