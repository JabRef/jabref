package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
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
    private final JabRefFrame frame;
    private final StateManager stateManager;
    private final PreferencesService preferences;
    private final DialogService dialogService;

    public ExportCommand(ExportMethod exportMethod,
                         JabRefFrame frame,
                         StateManager stateManager,
                         DialogService dialogService,
                         PreferencesService preferences) {
        this.exportMethod = exportMethod;
        this.frame = frame;
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.dialogService = dialogService;

        this.executable.bind(exportMethod == ExportMethod.EXPORT_SELECTED
                ? ActionHelper.needsEntriesSelected(stateManager)
                : ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        List<TemplateExporter> customExporters = preferences.getCustomExportFormats(Globals.journalAbbreviationRepository);
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository);
        SavePreferences savePreferences = preferences.getSavePreferencesForExport();
        XmpPreferences xmpPreferences = preferences.getXmpPreferences();

        // Get list of exporters and sort before adding to file dialog
        List<Exporter> exporters = Globals.exportFactory.getExporters().stream()
                                                        .sorted(Comparator.comparing(Exporter::getName))
                                                        .collect(Collectors.toList());

        Globals.exportFactory = ExporterFactory.create(customExporters, layoutPreferences, savePreferences,
                xmpPreferences, preferences.getGeneralPreferences().getDefaultBibDatabaseMode(), Globals.entryTypesManager);
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.exporterToExtensionFilter(exporters))
                .withDefaultExtension(preferences.getImportExportPreferences().getLastExportExtension())
                .withInitialDirectory(preferences.getImportExportPreferences().getExportWorkingDirectory())
                .build();
        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> export(path, fileDialogConfiguration.getSelectedExtensionFilter(), exporters));
    }

    private void export(Path file, FileChooser.ExtensionFilter selectedExtensionFilter, List<Exporter> exporters) {
        String selectedExtension = selectedExtensionFilter.getExtensions().get(0).replace("*", "");
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

        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = stateManager.getActiveDatabase()
                                                       .map(db -> db.getFileDirectories(preferences.getFilePreferences()))
                                                       .orElse(List.of(preferences.getFilePreferences().getWorkingDirectory()));

        // Make sure we remember which filter was used, to set
        // the default for next time:
        preferences.getImportExportPreferences().setLastExportExtension(format.getName());
        preferences.getImportExportPreferences().setExportWorkingDirectory(file.getParent());

        final List<BibEntry> finEntries = entries;

        BackgroundTask
                .wrap(() -> {
                    format.export(stateManager.getActiveDatabase().get(),
                            file,
                            finEntries);
                    return null; // can not use BackgroundTask.wrap(Runnable) because Runnable.run() can't throw Exceptions
                })
                .onSuccess(save -> {
                    LibraryTab.DatabaseNotification notificationPane = frame.getCurrentLibraryTab().getNotificationPane();
                    notificationPane.notify(
                            IconTheme.JabRefIcons.FOLDER.getGraphicNode(),
                            Localization.lang("%0 export successful. Open the folder containing the saved file?", format.getName()),
                            List.of(new Action(Localization.lang("Open"), event -> {
                                try {
                                    JabRefDesktop.openFolderAndSelectFile(file, preferences);
                                } catch (IOException e) {
                                    LOGGER.error("Could not open export folder.", e);
                                }
                                notificationPane.hide();
                            })),
                            Duration.seconds(5));
                })
                .onFailure(this::handleError)
                .executeWith(Globals.TASK_EXECUTOR);
    }

    private void handleError(Exception ex) {
        LOGGER.warn("Problem exporting", ex);
        dialogService.notify(Localization.lang("Could not save file."));
        // Need to warn the user that saving failed!
        dialogService.showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file."), ex);
    }
}
