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
package net.sf.jabref.gui.importer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImportFormats {
    private static final Log LOGGER = LogFactory.getLog(ImportFormats.class);

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
            private final boolean newDatabase;

            public ImportAction(JabRefFrame frame, boolean newDatabase) {
                this.frame = frame;
                this.newDatabase = newDatabase;

                if (newDatabase) {
                    putValue(Action.NAME, Localization.menuTitle("Import into new database"));
                    putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_NEW_DATABASE));
                } else {
                    putValue(Action.NAME, Localization.menuTitle("Import into current database"));
                    putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_CURRENT_DATABASE));
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                SortedSet<ImportFormat> importers = Globals.IMPORT_FORMAT_READER.getImportFormats();
                List<FileExtensions> extensions = importers.stream().map(p -> p.getExtensions()).collect(Collectors.toList());
                FileDialog dialog = new FileDialog(frame, Globals.prefs.get(JabRefPreferences.IMPORT_WORKING_DIRECTORY));
                // Add file filter for all supported types
                List<String> flatExtensions = extensions.stream().flatMap(extList -> Stream.of(extList.getExtensions())).collect(Collectors.toList());
                dialog.setFileFilter(new FileNameExtensionFilter(Localization.lang("Available import formats"), flatExtensions.toArray(new String[flatExtensions.size()])));
                // Add filters for extensions
                dialog.withExtensions(extensions);

                Optional<Path> selectedFile = dialog.showDialogAndGetSelectedFile();

                selectedFile.ifPresent(sel -> {
                    try {
                        File file = sel.toFile();

                        if (!file.exists()) {
                            JOptionPane.showMessageDialog(frame,
                                    Localization.lang("File not found") + ": '" + file.getName() + "'.",
                                    Localization.lang("Import"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        Optional<ImportFormat> format = importers.stream()
                                .filter(i -> Objects.equals(i.getExtensions().getDescription(), dialog.getFileFilter().getDescription()))
                                .findFirst();
                        ImportMenuItem importMenu = new ImportMenuItem(frame, newDatabase, format.orElse(null));
                        importMenu.automatedImport(Collections.singletonList(file.getAbsolutePath()));
                        // Set last working dir for import
                        Globals.prefs.put(JabRefPreferences.IMPORT_WORKING_DIRECTORY, file.getParent());
                    } catch (Exception ex) {
                        LOGGER.warn("Cannot import file", ex);
                    }
                });
            }
        }

        return new ImportAction(frame, openInNew);
    }
}
