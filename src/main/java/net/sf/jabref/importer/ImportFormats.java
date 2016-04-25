/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImportFormats {
    private static final Log LOGGER = LogFactory.getLog(ImportFormats.class);

    private static JFileChooser createImportFileChooser(String currentDir) {

        SortedSet<ImportFormat> importers = Globals.IMPORT_FORMAT_READER.getImportFormats();

        String lastUsedFormat = Globals.prefs.get(JabRefPreferences.LAST_USED_IMPORT);
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        Set<ImportFileFilter> filters = new TreeSet<>();
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

        if (defaultFilter == null) {
            fc.setFileFilter(fc.getAcceptAllFileFilter());
        } else {
            fc.setFileFilter(defaultFilter);
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

                putValue(Action.NAME, openInNew ? Localization.menuTitle("Import into new database") : Localization
                        .menuTitle("Import into current database"));
                putValue(Action.ACCELERATOR_KEY,
                        openInNew ? Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_NEW_DATABASE) : Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_CURRENT_DATABASE));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = createImportFileChooser(Globals.prefs.get(JabRefPreferences.IMPORT_WORKING_DIRECTORY));
                int result = fileChooser.showOpenDialog(frame);

                if (result != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                File file = fileChooser.getSelectedFile();
                if (file == null) {
                    return;
                }

                FileFilter ff = fileChooser.getFileFilter();
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
                    ImportMenuItem imi = new ImportMenuItem(frame, openInNew, format);
                    imi.automatedImport(Arrays.asList(file.getAbsolutePath()));

                    // Make sure we remember which filter was used, to set the default
                    // for next time:
                    if (format == null) {
                        Globals.prefs.put(JabRefPreferences.LAST_USED_IMPORT, "__all");
                    } else {
                        Globals.prefs.put(JabRefPreferences.LAST_USED_IMPORT, format.getFormatName());
                    }
                    Globals.prefs.put(JabRefPreferences.IMPORT_WORKING_DIRECTORY, file.getParent());
                } catch (Exception ex) {
                    LOGGER.warn("Problem with import format", ex);
                }

            }
        }

        return new ImportAction(frame, openInNew);
    }
}
