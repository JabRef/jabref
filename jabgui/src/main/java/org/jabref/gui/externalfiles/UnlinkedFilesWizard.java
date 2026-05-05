package org.jabref.gui.externalfiles;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import jakarta.inject.Inject;
import org.controlsfx.dialog.Wizard;

public class UnlinkedFilesWizard {
    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private UndoManager undoManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private FileUpdateMonitor fileUpdateMonitor;

    private UnlinkedFilesDialogViewModel viewModel;
    private BibDatabaseContext bibDatabaseContext;

    private Wizard wizard;
    private SearchConfigurationPage page1;
    private FileSelectionPage page2;
    private ImportResultsPage page3;

    public UnlinkedFilesWizard() {
    }

    public void show() {
        if (!initializeWizard()) {
            return;
        }

        Platform.runLater(() -> {
            if (page1.getScene() != null && page1.getScene().getWindow() instanceof javafx.stage.Stage stage) {
                stage.setResizable(true);
                stage.setWidth(650);
                stage.setHeight(550);
                stage.getIcons().addAll(IconTheme.getLogoSetFX());
            }
        });

        Optional<ButtonType> result = wizard.showAndWait();

        if (result.isPresent()) {
            if (result.get() == ButtonType.FINISH) {
                saveConfiguration();
            } else if (result.get() == ButtonType.CANCEL) {
                viewModel.cancelTasks();
            }
        } else {
            viewModel.cancelTasks();
        }
    }

    private boolean initializeWizard() {
        Optional<BibDatabaseContext> activeDatabase = stateManager.getActiveDatabase();
        if (activeDatabase.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library selected"),
                    Localization.lang("Please open or select a library before searching for unlinked files."));
            return false;
        }
        this.bibDatabaseContext = activeDatabase.get();

        viewModel = new UnlinkedFilesDialogViewModel(dialogService, undoManager, fileUpdateMonitor, preferences, stateManager, taskExecutor);

        page1 = new SearchConfigurationPage(viewModel, bibDatabaseContext, preferences);
        page2 = new FileSelectionPage(stateManager, viewModel);
        page3 = new ImportResultsPage(viewModel);

        page1.setPrefSize(650, 550);
        page2.setPrefSize(650, 550);
        page3.setPrefSize(650, 550);

        page1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        page2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        page3.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        wizard = new Wizard();
        wizard.setTitle(Localization.lang("Search for unlinked local files"));
        wizard.setFlow(new Wizard.LinearFlow(page1, page2, page3));

        return true;
    }

    private void saveConfiguration() {
        if (viewModel.selectedExtensionProperty().get() != null) {
            preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedExtension(viewModel.selectedExtensionProperty().get().getName());
        }
        if (viewModel.selectedDateProperty().get() != null) {
            preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedDateRange(viewModel.selectedDateProperty().get());
        }
        if (viewModel.selectedSortProperty().get() != null) {
            preferences.getUnlinkedFilesDialogPreferences().setUnlinkedFilesSelectedSort(viewModel.selectedSortProperty().get());
        }
    }
}
