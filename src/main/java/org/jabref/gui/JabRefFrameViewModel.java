package org.jabref.gui;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ButtonType;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class JabRefFrameViewModel {

    private final PreferencesService prefs;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final LibraryTabContainer tabContainer;

    public JabRefFrameViewModel(PreferencesService preferencesService,
                                StateManager stateManager,
                                DialogService dialogService,
                                LibraryTabContainer tabContainer) {
        this.prefs = preferencesService;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.tabContainer = tabContainer;
    }

    void storeLastOpenedFiles(List<Path> filenames, Path focusedDatabase) {
        if (prefs.getWorkspacePreferences().shouldOpenLastEdited()) {
            // Here we store the names of all current files. If there is no current file, we remove any
            // previously stored filename.
            if (filenames.isEmpty()) {
                prefs.getGuiPreferences().getLastFilesOpened().clear();
            } else {
                prefs.getGuiPreferences().setLastFilesOpened(filenames);
                prefs.getGuiPreferences().setLastFocusedFile(focusedDatabase);
            }
        }
    }

    /**
     * Quit JabRef
     *
     * @return true if the user chose to quit; false otherwise
     */
    public boolean close() {
        // Ask if the user really wants to close, if there are still background tasks running
        // The background tasks may make changes themselves that need saving.
        if (stateManager.getAnyTasksThatWillNotBeRecoveredRunning().getValue()) {
            Optional<ButtonType> shouldClose = dialogService.showBackgroundProgressDialogAndWait(
                    Localization.lang("Please wait..."),
                    Localization.lang("Waiting for background tasks to finish. Quit anyway?"),
                    stateManager);
            if (!(shouldClose.isPresent() && (shouldClose.get() == ButtonType.YES))) {
                return false;
            }
        }

        // Read the opened and focused databases before closing them
        List<Path> openedLibraries = tabContainer.getLibraryTabs().stream()
                                          .map(LibraryTab::getBibDatabaseContext)
                                          .map(BibDatabaseContext::getDatabasePath)
                                          .flatMap(Optional::stream)
                                          .toList();
        Path focusedLibraries = Optional.ofNullable(tabContainer.getCurrentLibraryTab())
                                        .map(LibraryTab::getBibDatabaseContext)
                                        .flatMap(BibDatabaseContext::getDatabasePath)
                                        .orElse(null);

        // Then ask if the user really wants to close, if the library has not been saved since last save.
        if (!tabContainer.closeTabs(tabContainer.getLibraryTabs())) {
            return false;
        }

        storeLastOpenedFiles(openedLibraries, focusedLibraries); // store only if successfully having closed the libraries

        ProcessingLibraryDialog processingLibraryDialog = new ProcessingLibraryDialog(dialogService);
        processingLibraryDialog.showAndWait(tabContainer.getLibraryTabs());

        return true;
    }
}
