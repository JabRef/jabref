package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.directorylibrary.DirectoryLibraryConverter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Converts the current `.bib` library into a directory library (see
/// [DirectoryLibraryConverter]): sidecars are written next to the linked files, the `.bib`
/// moves into the root as the library's mirror, and the root is reopened as a directory
/// library. Only offered for saved local libraries; aborts with an explanation when linked
/// files do not all live under the designated root.
public class ConvertToDirectoryLibraryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToDirectoryLibraryAction.class);
    private static final int MAX_REPORTED_OBSTACLES = 10;

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final OpenDirectoryLibraryAction openDirectoryLibraryAction;

    public ConvertToDirectoryLibraryAction(LibraryTabContainer tabContainer,
                                           DialogService dialogService,
                                           GuiPreferences preferences,
                                           AiService aiService,
                                           StateManager stateManager,
                                           FileUpdateMonitor fileUpdateMonitor,
                                           BibEntryTypesManager entryTypesManager,
                                           UndoManager undoManager,
                                           ClipBoardManager clipBoardManager,
                                           TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.openDirectoryLibraryAction = new OpenDirectoryLibraryAction(tabContainer, dialogService, preferences,
                aiService, stateManager, fileUpdateMonitor, entryTypesManager, undoManager, clipBoardManager, taskExecutor);

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();
        BibDatabaseContext context = libraryTab.getBibDatabaseContext();
        Optional<Path> bibPath = context.getDatabasePath();
        Optional<Path> root = DirectoryLibraryConverter.determineRoot(context);
        if (bibPath.isEmpty() || root.isEmpty()) {
            return;
        }

        DirectoryLibraryConverter converter = new DirectoryLibraryConverter();
        List<String> obstacles = converter.obstacles(context, root.get(), preferences.getFilePreferences());
        if (!obstacles.isEmpty()) {
            String reported = obstacles.stream()
                                       .limit(MAX_REPORTED_OBSTACLES)
                                       .collect(Collectors.joining("\n"));
            if (obstacles.size() > MAX_REPORTED_OBSTACLES) {
                reported += "\n" + Localization.lang("... and %0 more", obstacles.size() - MAX_REPORTED_OBSTACLES);
            }
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Convert to folder library"),
                    Localization.lang("The library cannot be converted:") + "\n\n" + reported);
            return;
        }

        Path rootName = root.get().getFileName();
        Path mirrorTarget = root.get().resolve((rootName == null ? "library" : rootName.toString()) + ".bib");
        if (!mirrorTarget.equals(bibPath.get()) && Files.exists(mirrorTarget)) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Convert to folder library"),
                    Localization.lang("'%0' already exists and would be overwritten.", mirrorTarget.toString()));
            return;
        }

        boolean confirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Convert to folder library"),
                Localization.lang("Every entry gets a Markdown sidecar next to its linked file, and the library file moves to '%0', staying in sync with the folder from now on.", mirrorTarget.toString()));
        if (!confirmed) {
            return;
        }

        if (!new SaveDatabaseAction(libraryTab, dialogService, preferences, entryTypesManager, stateManager).save()) {
            return;
        }
        try {
            converter.writeSidecars(context, root.get(), preferences.getFilePreferences());
            if (!mirrorTarget.equals(bibPath.get())) {
                Files.move(bibPath.get(), mirrorTarget);
            }
        } catch (IOException e) {
            LOGGER.error("Could not convert {} to a folder library", bibPath.get(), e);
            dialogService.showErrorDialogAndWait(Localization.lang("Convert to folder library"), e);
            return;
        }
        tabContainer.closeTab(libraryTab);
        openDirectoryLibraryAction.openDirectory(root.get());
    }
}
