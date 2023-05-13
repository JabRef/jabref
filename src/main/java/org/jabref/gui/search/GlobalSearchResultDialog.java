package org.jabref.gui.search;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;

public class GlobalSearchResultDialog extends BaseDialog<Void> {

    @FXML private SplitPane container;
    @FXML private ToggleButton keepOnTop;

    private final UndoManager undoManager;

    @Inject private PreferencesService preferencesService;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;

    private GlobalSearchResultDialogViewModel viewModel;

    public GlobalSearchResultDialog(UndoManager undoManager) {
        this.undoManager = undoManager;

        setTitle(Localization.lang("Search results from open libraries"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        initModality(Modality.NONE);
    }

    @FXML
    private void initialize() {
        viewModel = new GlobalSearchResultDialogViewModel(preferencesService);

        PreviewViewer previewViewer = new PreviewViewer(viewModel.getSearchDatabaseContext(), dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());

        SearchResultsTableDataModel model = new SearchResultsTableDataModel(viewModel.getSearchDatabaseContext(), preferencesService, stateManager);
        SearchResultsTable resultsTable = new SearchResultsTable(model, viewModel.getSearchDatabaseContext(), preferencesService, undoManager, dialogService, stateManager);

        resultsTable.getColumns().removeIf(SpecialFieldColumn.class::isInstance);
        resultsTable.getSelectionModel().selectFirst();

        if (resultsTable.getSelectionModel().getSelectedItem() != null) {
            previewViewer.setEntry(resultsTable.getSelectionModel().getSelectedItem().getEntry());
        }

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                previewViewer.setEntry(newValue.getEntry());
            } else {
                previewViewer.setEntry(old.getEntry());
            }
        });

        container.getItems().addAll(resultsTable, previewViewer);

        keepOnTop.selectedProperty().bindBidirectional(viewModel.keepOnTop());
        EasyBind.subscribe(viewModel.keepOnTop(), value -> {
            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(value);
            keepOnTop.setGraphic(value
                    ? IconTheme.JabRefIcons.KEEP_ON_TOP.getGraphicNode()
                    : IconTheme.JabRefIcons.KEEP_ON_TOP_OFF.getGraphicNode());
        });

        getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            getDialogPane().setPrefHeight(preferencesService.getSearchPreferences().getSearchWindowHeight());
            getDialogPane().setPrefWidth(preferencesService.getSearchPreferences().getSearchWindowWidth());
        });

        getDialogPane().getScene().getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> {
            preferencesService.getSearchPreferences().setSearchWindowHeight(getHeight());
            preferencesService.getSearchPreferences().setSearchWindowWidth(getWidth());
        });
    }
}
