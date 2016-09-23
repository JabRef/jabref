package net.sf.jabref.gui.externalfiles;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FileListEditor;
import net.sf.jabref.gui.filelist.FileListEntry;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Action for moving or renaming a file that is linked to from an entry in JabRef.
 */
public class MoveFileAction extends AbstractAction {

    private static final Log LOGGER = LogFactory.getLog(MoveFileAction.class);

    private final JabRefFrame frame;
    private final EntryEditor eEditor;
    private final FileListEditor editor;

    private final boolean toFileDir;

    private static final String MOVE_RENAME = Localization.lang("Move/Rename file");


    public MoveFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor, boolean toFileDir) {
        this.frame = frame;
        this.eEditor = eEditor;
        this.editor = editor;
        this.toFileDir = toFileDir;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        int selected = editor.getSelectedRow();

        if (selected == -1) {
            return;
        }

        FileListEntry entry = editor.getTableModel().getEntry(selected);

        // Check if the current file exists:
        String ln = entry.link;
        boolean httpLink = ln.toLowerCase(Locale.ENGLISH).startsWith("http");
        if (httpLink) {
            // TODO: notify that this operation cannot be done on remote links
            return;
        }

        // Get an absolute path representation:
        List<String> dirs = frame.getCurrentBasePanel().getBibDatabaseContext()
                .getFileDirectory(Globals.prefs.getFileDirectoryPreferences());
        int found = -1;
        for (int i = 0; i < dirs.size(); i++) {
            if (new File(dirs.get(i)).exists()) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            JOptionPane.showMessageDialog(frame, Localization.lang("File_directory_is_not_set_or_does_not_exist!"),
                    MOVE_RENAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        File file = new File(ln);
        if (!file.isAbsolute()) {
            file = FileUtil.expandFilename(ln, dirs).orElse(null);
        }
        if ((file != null) && file.exists()) {
            // Ok, we found the file. Now get a new name:

            File newFile = null;
            boolean repeat = true;
            while (repeat) {
                repeat = false;
                String chosenFile;
                if (toFileDir) {
                    // Determine which name to suggest:
                    String suggName = FileUtil
                            .createFileNameFromPattern(eEditor.getDatabase(), eEditor.getEntry(),
                                    Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN),
                                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
                            .concat(entry.type.isPresent() ? "." + entry.type.get().getExtension() : "");
                    CheckBoxMessage cbm = new CheckBoxMessage(Localization.lang("Move file to file directory?"),
                            Localization.lang("Rename to '%0'", suggName),
                            Globals.prefs.getBoolean(JabRefPreferences.RENAME_ON_MOVE_FILE_TO_FILE_DIR));
                    int answer;
                    // Only ask about renaming file if the file doesn't have the proper name already:
                    if (suggName.equals(file.getName())) {
                        answer = JOptionPane.showConfirmDialog(frame, Localization.lang("Move file to file directory?"),
                                MOVE_RENAME, JOptionPane.YES_NO_OPTION);
                    } else {
                        answer = JOptionPane.showConfirmDialog(frame, cbm, MOVE_RENAME, JOptionPane.YES_NO_OPTION);
                    }
                    if (answer != JOptionPane.YES_OPTION) {
                        return;
                    }
                    Globals.prefs.putBoolean(JabRefPreferences.RENAME_ON_MOVE_FILE_TO_FILE_DIR, cbm.isSelected());
                    StringBuilder sb = new StringBuilder(dirs.get(found));
                    if (!dirs.get(found).endsWith(File.separator)) {
                        sb.append(File.separator);
                    }
                    if (cbm.isSelected()) {
                        // Rename:
                        sb.append(suggName);
                    } else {
                        // Do not rename:
                        sb.append(file.getName());
                    }
                    chosenFile = sb.toString();
                } else {
                    Optional<Path> path = new FileDialog(frame, file.getPath()).saveNewFile();
                    if (path.isPresent()) {
                        chosenFile = path.get().toString();
                    } else {
                        return;
                    }
                }
                newFile = new File(chosenFile);

            }

            if ((newFile != null) && !newFile.equals(file)) {
                try {
                    boolean success = file.renameTo(newFile);
                    if (!success) {
                        success = FileUtil.copyFile(Paths.get(file.toURI()), Paths.get(newFile.toURI()), true);
                    }
                    if (success) {
                        // Remove the original file:
                        Files.deleteIfExists(file.toPath());

                        // Relativise path, if possible.
                        String canPath = new File(dirs.get(found)).getCanonicalPath();
                        if (newFile.getCanonicalPath().startsWith(canPath)) {
                            if ((newFile.getCanonicalPath().length() > canPath.length())
                                    && (newFile.getCanonicalPath().charAt(canPath.length()) == File.separatorChar)) {

                                String newLink = newFile.getCanonicalPath().substring(1 + canPath.length());
                                editor.getTableModel().setEntry(selected,
                                        new FileListEntry(entry.description, newLink, entry.type));
                            } else {
                                String newLink = newFile.getCanonicalPath().substring(canPath.length());
                                editor.getTableModel().setEntry(selected,
                                        new FileListEntry(entry.description, newLink, entry.type));
                            }

                        } else {
                            String newLink = newFile.getCanonicalPath();
                            editor.getTableModel().setEntry(selected,
                                    new FileListEntry(entry.description, newLink, entry.type));
                        }
                        eEditor.updateField(editor);
                        frame.output(Localization.lang("File moved"));
                    } else {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Move file failed"), MOVE_RENAME,
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (SecurityException | IOException ex) {
                    LOGGER.warn("Could not move file", ex);
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("Could not move file '%0'.", file.getAbsolutePath()) + ex.getMessage(),
                            MOVE_RENAME, JOptionPane.ERROR_MESSAGE);
                }

            }
        } else {
            // File doesn't exist, so we can't move it.
            JOptionPane.showMessageDialog(frame, Localization.lang("Could not find file '%0'.", entry.link),
                    Localization.lang("File not found"), JOptionPane.ERROR_MESSAGE);
        }

    }
}
