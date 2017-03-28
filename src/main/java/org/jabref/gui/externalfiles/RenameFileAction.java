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
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.ParsedFileField;

public class RenameFileAction extends AbstractAction {

    private final JabRefFrame frame;
    private final EntryEditor eEditor;
    private final FileListEditor editor;

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
        // Check if the current file exists:
        String ln = entry.getLink();
        boolean httpLink = ln.toLowerCase(Locale.ENGLISH).startsWith("http");
        if (httpLink) {
            // TODO: notify that this operation cannot be done on remote links
            return;
        }
        List<String> dirs = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
        Optional<Path> fileDir = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());
        if (!fileDir.isPresent()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File directory is not set or does not exist!"),
                    Localization.lang("Rename file"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path file = Paths.get(ln);
        if (!file.isAbsolute()) {
            file = FileUtil.expandFilename(ln, dirs).map(File::toPath).orElse(null);
        }

        if ((file != null) && Files.exists(file)) {

            RenamePdfCleanup pdfCleanup = new RenamePdfCleanup(false,
                    frame.getCurrentBasePanel().getBibDatabaseContext(),
                    Globals.prefs.getCleanupPreferences(Globals.journalAbbreviationLoader).getFileNamePattern(),
                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                    Globals.prefs.getFileDirectoryPreferences(), field);

            String targetFileName = pdfCleanup.getTargetFileName(field, eEditor.getEntry());

            String[] options = {Localization.lang("Rename file"), Localization.lang("Cancel")};
            int dialogResult = JOptionPane.showOptionDialog(frame,
                    Localization.lang("Rename file to") + " " + targetFileName,
                    Localization.lang("Rename file"),
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[0]);

            //indicates Rename pressed
            if (dialogResult == JOptionPane.YES_OPTION) {
                pdfCleanup.cleanup(eEditor.getEntry());
            }

        }
    }

}
