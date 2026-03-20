package org.jabref.gui.preferences;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.ScopedPreferenceImporter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopedPreferencesImportCommand extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopedPreferencesImportCommand.class);
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final CliPreferences preferences;

    public ScopedPreferencesImportCommand(DialogService dialogService, StateManager stateManager, CliPreferences preferences) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;
    }

    @Override
    public void execute() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();

        Optional<Path> importPath = dialogService.showFileOpenDialog(fileDialogConfiguration);
        if (importPath.isEmpty()) {
            return;
        }

        Map<String, MetaData> libraries = new HashMap<>();
        for (BibDatabaseContext ctx : stateManager.getOpenDatabases()) {
            String name = ctx.getDatabasePath().map(Path::getFileName).map(Path::toString).orElse("untitled");
            libraries.put(name, ctx.getMetaData());
        }

        try {
            ScopedPreferenceImporter.importFromJson(importPath.get(), preferences, libraries);
            dialogService.notify("Imported scoped preferences successfully.");
        } catch (IOException e) {
            LOGGER.error("Could not import preferences", e);
            dialogService.showErrorDialogAndWait("Error importing preferences", e);
        }
    }
}
