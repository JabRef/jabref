package org.jabref.gui.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;

import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * Perform import operation
 */
public class ImportCommand extends SimpleCommand {

    private final JabRefFrame frame;
    private final boolean openInNew;
    private final DialogService dialogService;
    private final PreferencesService preferences;

    /**
     * @param openInNew Indicate whether the entries should import into a new database or into the currently open one.
     */
    public ImportCommand(JabRefFrame frame, boolean openInNew, PreferencesService preferences, StateManager stateManager) {
        this.frame = frame;
        this.openInNew = openInNew;
        this.preferences = preferences;

        if (!openInNew) {
            this.executable.bind(needsDatabase(stateManager));
        }

        this.dialogService = frame.getDialogService();
    }

    @Override
    public void execute() {
        SortedSet<Importer> importers = Globals.IMPORT_FORMAT_READER.getImportFormats();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.ANY_FILE)
                .addExtensionFilter(FileFilterConverter.forAllImporters(importers))
                .addExtensionFilter(FileFilterConverter.importerToExtensionFilter(importers))
                .withInitialDirectory(preferences.getImportExportPreferences().getImportWorkingDirectory())
                .build();
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(path -> doImport(path, importers, fileDialogConfiguration.getSelectedExtensionFilter()));
    }

    private void doImport(Path file, SortedSet<Importer> importers, FileChooser.ExtensionFilter selectedExtensionFilter) {
        if (!Files.exists(file)) {
            dialogService.showErrorDialogAndWait(Localization.lang("Import"),
                    Localization.lang("File not found") + ": '" + file.getFileName() + "'.");

            return;
        }
        Optional<Importer> format = FileFilterConverter.getImporter(selectedExtensionFilter, importers);
        ImportAction importMenu = new ImportAction(frame, openInNew, format.orElse(null), preferences);
        importMenu.automatedImport(Collections.singletonList(file.toString()));
        // Set last working dir for import
        preferences.getImportExportPreferences().setImportWorkingDirectory(file.getParent());
    }
}
