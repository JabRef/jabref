package org.jabref.gui.importer.actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.importer.ImportFormatReader;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/// Perform an import action
public class ImportCommand extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCommand.class);

    public enum ImportMethod { AS_NEW, TO_EXISTING }

    private final LibraryTabContainer tabContainer;
    private final ImportMethod importMethod;

    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;

    public ImportCommand(LibraryTabContainer tabContainer,
                         ImportMethod importMethod,
                         CliPreferences preferences,
                         StateManager stateManager,
                         FileUpdateMonitor fileUpdateMonitor,
                         TaskExecutor taskExecutor,
                         DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.importMethod = importMethod;
        this.preferences = preferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        if (importMethod == ImportMethod.TO_EXISTING) {
            this.executable.bind(needsDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        ImportFormatReader importFormatReader = new ImportFormatReader(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getCitationKeyPatternPreferences(),
                fileUpdateMonitor
        );
        SortedSet<Importer> importers = importFormatReader.getImportFormats();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.ANY_FILE)
                .addExtensionFilter(FileFilterConverter.forAllImporters(importers))
                .addExtensionFilter(FileFilterConverter.importerToExtensionFilter(importers))
                .withInitialDirectory(preferences.getImporterPreferences().getImportWorkingDirectory())
                .build();

        List<Path> selectedFiles = dialogService.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration);

        if (selectedFiles.isEmpty()) {
            return; // User canceled or no files selected
        }

        importMultipleFiles(selectedFiles, importers, fileDialogConfiguration.getSelectedExtensionFilter());
    }

    private void importMultipleFiles(List<Path> files, SortedSet<Importer> importers, FileChooser.ExtensionFilter selectedExtensionFilter) {
        List<Path> nonExistentFiles = files.stream()
                                           .filter(file -> !Files.exists(file))
                                           .toList();
        if (!nonExistentFiles.isEmpty()) {
            String fileNames = nonExistentFiles.stream()
                                               .map(path -> path.getFileName().toString())
                                               .collect(Collectors.joining(", "));
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Import"),
                    Localization.lang("File(s) %0 not found.", fileNames));
            return;
        }

        Optional<Importer> format;
        boolean isGeneralFilter = selectedExtensionFilter == FileFilterConverter.ANY_FILE
                || "Available import formats".equals(selectedExtensionFilter.getDescription());

        if (!isGeneralFilter) {
            // User picked a specific format
            format = FileFilterConverter.getImporter(selectedExtensionFilter, importers);
        } else if (files.size() == 1) {
            // Infer if only one file and no specific filter
            selectedExtensionFilter = FileFilterConverter.determineExtensionFilter(files.getFirst());
            format = FileFilterConverter.getImporter(selectedExtensionFilter, importers);
        } else {
            format = Optional.empty();
        }

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> (new ImportCommandHelper(preferences, dialogService, fileUpdateMonitor)).doImport(files, format.orElse(null)));

        LibraryTab tab = tabContainer.getCurrentLibraryTab();

        // If there is no open library tab, we fall back to importing as new
        // This prevents a crash in case the user selects "Import into current library"
        // while no library is currently open.
        if (importMethod == ImportMethod.AS_NEW || tab == null) {
            task.onSuccess(parserResult -> {
                    tabContainer.addTab(parserResult.getDatabaseContext(), true);
                    dialogService.notify(Localization.lang("%0 entry(s) imported", parserResult.getDatabase().getEntries().size()));
                })
                .onFailure(ex -> {
                    LOGGER.error("Error importing", ex);
                    dialogService.notify(Localization.lang("Error importing. See the error log for details."));
                })
                .executeWith(taskExecutor);
        } else {
            ImportEntriesDialog dialog = new ImportEntriesDialog(tab.getBibDatabaseContext(), task);
            dialog.setTitle(Localization.lang("Import"));
            dialogService.showCustomDialogAndWait(dialog);
        }

        // Set last working dir for import
        preferences.getImporterPreferences().setImportWorkingDirectory(files.getLast().getParent());
    }
}
