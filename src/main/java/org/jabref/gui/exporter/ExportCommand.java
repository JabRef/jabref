package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs an export action
 */
public class ExportCommand extends SimpleCommand {

    public enum ExportMethod { EXPORT_ALL, EXPORT_SELECTED }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportCommand.class);

    private final ExportMethod exportMethod;
    private final Supplier<LibraryTab> tabSupplier;
    private final StateManager stateManager;
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final BibEntryTypesManager entryTypesManager;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;

    public ExportCommand(ExportMethod exportMethod,
                         Supplier<LibraryTab> tabSupplier,
                         StateManager stateManager,
                         DialogService dialogService,
                         PreferencesService preferences,
                         BibEntryTypesManager entryTypesManager,
                         JournalAbbreviationRepository abbreviationRepository,
                         TaskExecutor taskExecutor) {
        this.exportMethod = exportMethod;
        this.tabSupplier = tabSupplier;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.entryTypesManager = entryTypesManager;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;

        this.executable.bind(exportMethod == ExportMethod.EXPORT_SELECTED
                ? ActionHelper.needsEntriesSelected(stateManager)
                : ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        // Get list of exporters and sort before adding to file dialog
        ExporterFactory exporterFactory = ExporterFactory.create(
                preferences,
                entryTypesManager);
        List<Exporter> exporters = exporterFactory.getExporters().stream()
                                                  .sorted(Comparator.comparing(Exporter::getName))
                                                  .collect(Collectors.toList());

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.exporterToExtensionFilter(exporters))
                .withDefaultExtension(preferences.getExportPreferences().getLastExportExtension())
                .withInitialDirectory(preferences.getExportPreferences().getExportWorkingDirectory())
                .build();
        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> export(path, fileDialogConfiguration.getSelectedExtensionFilter(), exporters));
    }

    private void export(Path file, FileChooser.ExtensionFilter selectedExtensionFilter, List<Exporter> exporters) {
        String selectedExtension = selectedExtensionFilter.getExtensions().getFirst().replace("*", "");
        if (!file.endsWith(selectedExtension)) {
            FileUtil.addExtension(file, selectedExtension);
        }

        final Exporter format = FileFilterConverter.getExporter(selectedExtensionFilter, exporters)
                                                   .orElseThrow(() -> new IllegalStateException("User didn't selected a file type for the extension"));
        List<BibEntry> entries;
        if (exportMethod == ExportMethod.EXPORT_SELECTED) {
            // Selected entries
            entries = stateManager.getSelectedEntries();
        } else {
            // All entries
            entries = stateManager.getActiveDatabase()
                                  .map(BibDatabaseContext::getEntries)
                                  .orElse(Collections.emptyList());
        }

        List<Path> fileDirForDatabase = stateManager.getActiveDatabase()
                                                       .map(db -> db.getFileDirectories(preferences.getFilePreferences()))
                                                       .orElse(List.of(preferences.getFilePreferences().getWorkingDirectory()));

        // Make sure we remember which filter was used, to set
        // the default for next time:
        preferences.getExportPreferences().setLastExportExtension(format.getName());
        preferences.getExportPreferences().setExportWorkingDirectory(file.getParent());

        final List<BibEntry> finEntries = entries;

        BackgroundTask
                .wrap(() -> {
                    format.export(stateManager.getActiveDatabase().get(),
                            file,
                            finEntries,
                            fileDirForDatabase,
                            abbreviationRepository);
                    return null; // can not use BackgroundTask.wrap(Runnable) because Runnable.run() can't throw Exceptions
                })
                .onSuccess(save -> {
                    LibraryTab.DatabaseNotification notificationPane = tabSupplier.get().getNotificationPane();
                    notificationPane.notify(
                            IconTheme.JabRefIcons.FOLDER.getGraphicNode(),
                            Localization.lang("Export operation finished successfully."),
                            List.of(new Action(Localization.lang("Reveal in File Explorer"), event -> {
                                try {
                                    JabRefDesktop.openFolderAndSelectFile(file, preferences.getExternalApplicationsPreferences(), dialogService);
                                } catch (IOException e) {
                                    LOGGER.error("Could not open export folder.", e);
                                }
                                notificationPane.hide();
                            })),
                            Duration.seconds(5));
                })
                .onFailure(this::handleError)
                .executeWith(taskExecutor);
    }

    private void handleError(Exception ex) {
        LOGGER.warn("Problem exporting", ex);
        dialogService.notify(Localization.lang("Could not save file."));
        // Need to warn the user that saving failed!
        dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
    }
}
