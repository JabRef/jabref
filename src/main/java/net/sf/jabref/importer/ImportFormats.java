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
package net.sf.jabref.importer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 22, 2006
 * Time: 12:06:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ImportFormats {

    private static JFileChooser createImportFileChooser(String currentDir) {

        SortedSet<ImportFormat> importers = Globals.importFormatReader.getImportFormats();

        String lastUsedFormat = Globals.prefs.get("lastUsedImport");
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        TreeSet<ImportFileFilter> filters = new TreeSet<ImportFileFilter>();
        for (ImportFormat format : importers) {
            ImportFileFilter filter = new ImportFileFilter(format);
            filters.add(filter);
            if (format.getFormatName().equals(lastUsedFormat)) {
                defaultFilter = filter;
            }
        }
        for (ImportFileFilter filter : filters) {
            fc.addChoosableFileFilter(filter);
        }

        if (defaultFilter != null) {
            fc.setFileFilter(defaultFilter);
        } else {
            fc.setFileFilter(fc.getAcceptAllFileFilter());
        }
        return fc;
    }

    /**
     * Create an AbstractAction for performing an Import operation.
     * @param frame The JabRefFrame of this JabRef instance.
     * @param openInNew Indicate whether the action should open into a new database or
     *  into the currently open one.
     * @return The action.
     */
    public static AbstractAction getImportAction(JabRefFrame frame, boolean openInNew) {

        class ImportAction extends MnemonicAwareAction {

            private final JabRefFrame frame;
            private final boolean openInNew;


            public ImportAction(JabRefFrame frame, boolean openInNew) {
                this.frame = frame;
                this.openInNew = openInNew;

                putValue(Action.NAME, openInNew ? Localization.menuTitle("Import into new database") :
                        Localization.menuTitle("Import into current database"));
                putValue(Action.ACCELERATOR_KEY, openInNew ? Globals.prefs.getKey("Import into new database") :
                        Globals.prefs.getKey("Import into current database"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = ImportFormats.createImportFileChooser
                        (Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY));
                fc.showOpenDialog(frame);
                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }
                FileFilter ff = fc.getFileFilter();
                ImportFormat format = null;
                if (ff instanceof ImportFileFilter) {
                    format = ((ImportFileFilter) ff).getImportFormat();
                }

                try {
                    if (!file.exists()) {
                        // Warn that the file doesn't exists:
                        JOptionPane.showMessageDialog(frame,
                                Localization.lang("File not found") +
                                        ": '" + file.getName() + "'.",
                                Localization.lang("Import"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ImportMenuItem imi = new ImportMenuItem(frame,
                            openInNew, format);
                    imi.automatedImport(new String[] {file.getAbsolutePath()});

                    // Make sure we remember which filter was used, to set the default
                    // for next time:
                    if (format != null) {
                        Globals.prefs.put("lastUsedImport", format.getFormatName());
                    } else {
                        Globals.prefs.put("lastUsedImport", "__all");
                    }
                    Globals.prefs.put(JabRefPreferences.IMPORT_WORKING_DIRECTORY, file.getParent());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }

        return new ImportAction(frame, openInNew);
    }
}
