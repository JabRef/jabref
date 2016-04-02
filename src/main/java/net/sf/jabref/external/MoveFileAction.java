/*  Copyright (C) 2003-2011 JabRef contributors.
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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FileListEditor;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Action for moving or renaming a file that is linked to from an entry in JabRef.
 */
public class MoveFileAction extends AbstractAction {

    private final JabRefFrame frame;
    private final EntryEditor eEditor;
    private final FileListEditor editor;
    private final boolean toFileDir;

    private static final String MOVE_RENAME = Localization.lang("Move/Rename file");

    private static final Log LOGGER = LogFactory.getLog(MoveFileAction.class);

    public MoveFileAction(JabRefFrame frame, EntryEditor eEditor, FileListEditor editor,
            boolean toFileDir) {
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
        FileListEntry flEntry = editor.getTableModel().getEntry(selected);
        // Check if the current file exists:
        String ln = flEntry.link;
        boolean httpLink = ln.toLowerCase(Locale.ENGLISH).startsWith("http");
        if (httpLink) {
            // TODO: notify that this operation cannot be done on remote links

        }

        // Get an absolute path representation:
        List<String> dirs = frame.getCurrentBasePanel().getBibDatabaseContext().getFileDirectory();
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
            String extension = null;
            if (flEntry.type.isPresent()) {
                extension = "." + flEntry.type.get().getExtension();
            }

            File newFile = null;
            boolean repeat = true;
            while (repeat) {
                repeat = false;
                String chosenFile;
                if (toFileDir) {
                    // Determine which name to suggest:
                    String suggName = Util
                            .getLinkedFileName(eEditor.getDatabase(), eEditor.getEntry(),
                                    Globals.journalAbbreviationLoader.getRepository())
                            .concat(flEntry.type.isPresent() ? "." + flEntry.type.get().getExtension() : "");
                    CheckBoxMessage cbm = new CheckBoxMessage(Localization.lang("Move file to file directory?"),
                            Localization.lang("Rename to '%0'", suggName),
                            Globals.prefs.getBoolean(JabRefPreferences.RENAME_ON_MOVE_FILE_TO_FILE_DIR));
                    int answer;
                    // Only ask about renaming file if the file doesn't have the proper name already:
                    if (suggName.equals(file.getName())) {
                        answer = JOptionPane.showConfirmDialog(frame, Localization.lang("Move file to file directory?"),
                                MOVE_RENAME, JOptionPane.YES_NO_OPTION);
                    } else {
                        answer = JOptionPane.showConfirmDialog(frame, cbm, MOVE_RENAME,
                                JOptionPane.YES_NO_OPTION);
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
                    chosenFile = FileDialogs.getNewFile(frame, file, extension, JFileChooser.SAVE_DIALOG, false);
                }
                if (chosenFile == null) {
                    return; // cancelled
                }
                newFile = new File(chosenFile);
                // Check if the file already exists:
                if (newFile.exists() && (JOptionPane.showConfirmDialog(frame,
                        Localization.lang("'%0' exists. Overwrite file?", newFile.getName()), MOVE_RENAME,
                        JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)) {
                    if (toFileDir) {
                        return;
                    } else {
                        repeat = true;
                    }
                }
            }

            if (!newFile.equals(file)) {
                try {
                    boolean success = file.renameTo(newFile);
                    if (!success) {
                        success = FileUtil.copyFile(file, newFile, true);
                    }
                    if (success) {
                        // Remove the original file:
                        if (!file.delete()) {
                            LOGGER.info("Cannot delete original file");
                        }
                        // Relativise path, if possible.
                        String canPath = new File(dirs.get(found)).getCanonicalPath();
                        if (newFile.getCanonicalPath().startsWith(canPath)) {
                            if ((newFile.getCanonicalPath().length() > canPath.length()) &&
                                    (newFile.getCanonicalPath().charAt(canPath.length()) == File.separatorChar)) {

                                String newLink = newFile.getCanonicalPath().substring(1 + canPath.length());
                                editor.getTableModel().setEntry(selected, new FileListEntry(flEntry.description, newLink, flEntry.type));
                            } else {
                                String newLink = newFile.getCanonicalPath().substring(canPath.length());
                                editor.getTableModel().setEntry(selected, new FileListEntry(flEntry.description, newLink, flEntry.type));
                            }

                        } else {
                            String newLink = newFile.getCanonicalPath();
                            editor.getTableModel().setEntry(selected, new FileListEntry(flEntry.description, newLink, flEntry.type));
                        }
                        eEditor.updateField(editor);
                        //JOptionPane.showMessageDialog(frame, Globals.lang("File moved"),
                        //        Globals.lang("Move/Rename file"), JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(frame, Localization.lang("Could not find file '%0'.", flEntry.link),
                    Localization.lang("File not found"), JOptionPane.ERROR_MESSAGE);

        }

    }
}
