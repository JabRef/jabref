package org.jabref.gui.importer.actions;

import java.nio.file.Path;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.directorylibrary.DirectoryLibraryScanner;
import org.jabref.logic.directorylibrary.DirectoryLibrarySynchronizer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DirectoryMonitor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;

/// Opens a directory as a library: the main table fills from the Hayagriva `.yml` sidecars and
/// `.pdf` files found in the directory tree (see [DirectoryLibraryScanner]).
public class OpenDirectoryLibraryAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final UndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    public OpenDirectoryLibraryAction(LibraryTabContainer tabContainer,
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
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(this::openDirectory);
    }

    private void openDirectory(Path root) {
        preferences.getFilePreferences().setWorkingDirectory(root);
        BackgroundTask.wrap(() -> new DirectoryLibraryScanner().scan(root))
                      .onSuccess(this::showLibraryTab)
                      .onFailure(exception -> dialogService.showErrorDialogAndWait(
                              Localization.lang("Open folder as library"),
                              Localization.lang("Could not open folder '%0' as library.", root.toString()),
                              exception))
                      .executeWith(taskExecutor);
    }

    private void showLibraryTab(DirectoryLibraryScanner.ScanResult scanResult) {
        // The synchronous factory keeps the DIRECTORY location: the ParserResult-based one
        // reconstructs a fresh (LOCAL) context from database + metadata on loading success
        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                scanResult.databaseContext(),
                tabContainer,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor);
        tabContainer.addTab(libraryTab, true);
        // No change event follows the synchronous tab creation, so set the initial title here
        libraryTab.updateTabTitle(false);

        BibDatabaseContext databaseContext = scanResult.databaseContext();
        DirectoryLibrarySynchronizer synchronizer = new DirectoryLibrarySynchronizer(
                databaseContext, scanResult.catalog(), UiTaskExecutor::runInJavaFXThread);
        databaseContext.attachDirectorySynchronizer(synchronizer);
        synchronizer.startWatching(Injector.instantiateModelOrService(DirectoryMonitor.class));

        if (!scanResult.warnings().isEmpty()) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Open folder as library"),
                    String.join("\n", scanResult.warnings()));
        }
    }
}
