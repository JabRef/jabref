package org.jabref.gui.importer;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import javafx.stage.FileChooser;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileExtensions;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImportFormats {

    private static final Log LOGGER = LogFactory.getLog(ImportFormats.class);

    private ImportFormats() {
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

            private final boolean newDatabase;

            public ImportAction(boolean newDatabase) {
                this.newDatabase = newDatabase;

                if (newDatabase) {
                    putValue(Action.NAME, Localization.menuTitle("Import into new library"));
                    putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_NEW_DATABASE));
                } else {
                    putValue(Action.NAME, Localization.menuTitle("Import into current library"));
                    putValue(Action.ACCELERATOR_KEY,
                            Globals.getKeyPrefs().getKey(KeyBinding.IMPORT_INTO_CURRENT_DATABASE));
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                SortedSet<Importer> importers = Globals.IMPORT_FORMAT_READER.getImportFormats();
                List<FileExtensions> extensions = importers.stream().map(Importer::getExtensions)
                        .collect(Collectors.toList());
                FileChooser.ExtensionFilter allImports = ImportFileFilter
                        .convert(Localization.lang("Available import formats"), importers);

                FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                        .addExtensionFilters(extensions)
                        .withInitialDirectory(Globals.prefs.get(JabRefPreferences.IMPORT_WORKING_DIRECTORY))
                        .build();
                DialogService ds = new FXDialogService();

                FileChooser fs = ds.getConfiguredFileChooser(fileDialogConfiguration);
                fs.setSelectedExtensionFilter(allImports);
                File f = DefaultTaskExecutor
                        .runInJavaFXThread(() -> fs.showOpenDialog(null));
                Optional<Path> selectedFile = Optional.ofNullable(f).map(File::toPath);
                FileChooser.ExtensionFilter selectedExtension = fs.getSelectedExtensionFilter();
                // Add file filter for all supported types

                selectedFile.ifPresent(file -> {
                    try {
                        if (!Files.exists(file)) {
                            JOptionPane.showMessageDialog(frame,
                                    Localization.lang("File not found") + ": '" + file.getFileName() + "'.",
                                    Localization.lang("Import"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        Optional<Importer> format = ImportFileFilter.convert(selectedExtension, importers);
                        ImportMenuItem importMenu = new ImportMenuItem(frame, newDatabase, format.orElse(null));
                        importMenu.automatedImport(Collections.singletonList(file.toString()));
                        // Set last working dir for import
                        Globals.prefs.put(JabRefPreferences.IMPORT_WORKING_DIRECTORY, file.getParent().toString());
                    } catch (Exception ex) {
                        LOGGER.warn("Cannot import file", ex);
                    }
                });
            }
        }

        return new ImportAction(openInNew);
    }
}
