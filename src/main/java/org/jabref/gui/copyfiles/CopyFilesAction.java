package org.jabref.gui.copyfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;


import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;
import static org.jabref.gui.edit.automaticfiededitor.AbstractAutomaticFieldEditorTabViewModel.LOGGER;

public class CopyFilesAction extends SimpleCommand {

    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final StateManager stateManager;
    private final UiTaskExecutor uiTaskExecutor;

    public CopyFilesAction(DialogService dialogService,
                           CliPreferences preferences,
                           StateManager stateManager,
                           UiTaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.uiTaskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(stateManager).and(needsEntriesSelected(stateManager)));
    }

    private void showDialog(List<CopyFilesResultItemViewModel> data) {
        if (data.isEmpty()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Copy linked files to folder..."), Localization.lang("No linked files found for export."));
            return;
        }
        dialogService.showCustomDialogAndWait(new CopyFilesDialogView(new CopyFilesResultListDependency(data)));
    }

    @Override
    public void execute() {

        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        List<BibEntry> entries = stateManager.getSelectedEntries();

        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getExportPreferences().getExportWorkingDirectory())
                .build();
        Optional<Path> exportPath = dialogService.showDirectorySelectionDialog(dirDialogConfiguration);
        exportPath.ifPresent(path -> {
            Task<List<CopyFilesResultItemViewModel>> exportTask = new CopyFilesTask(database, entries, path, preferences);

            dialogService.showProgressDialog(
                    Localization.lang("Copy linked files to folder..."),
                    Localization.lang("Copy linked files to folder..."),
                    exportTask);

            LOGGER.info("Creating CopyFilesTask with path: " + path);
            for (BibEntry entry : entries) {
                List<LinkedFile> linkedFiles = entry.getFiles();
                for (LinkedFile file : linkedFiles) {
                    Optional<Path> sourcePath = file.findIn(List.of(path));
                    if (sourcePath.isPresent()) {
                        Path targetPath = path.resolve(sourcePath.get().getFileName());
                        try {
                            Files.copy(sourcePath.get(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                            LinkedFile newLinkedFile = new LinkedFile(file.getDescription(), targetPath.toString(), file.getFileType());
                            entry.addFile(newLinkedFile);
                            LOGGER.info("Successfully copied file: " + sourcePath.get() + " to " + targetPath);
                        } catch (IOException e) {
                            LOGGER.error("Failed to copy file: " + sourcePath.get() + " to " + targetPath, e);
                        }
                    }else{
                        LOGGER.warn("Source file not found or doesn't exist: {}", file.getLink());
                    }
                }
            }
            uiTaskExecutor.execute(exportTask);

            LOGGER.info("CopyFilesTask has been executed.");
            exportTask.setOnSucceeded(e -> showDialog(exportTask.getValue()));
        });
    }
}
