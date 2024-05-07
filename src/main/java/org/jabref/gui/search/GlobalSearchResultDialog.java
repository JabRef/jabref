package org.jabref.gui.search;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;

public class GlobalSearchResultDialog extends BaseDialog<Void> {

    @FXML private SplitPane container;
    @FXML private ToggleButton keepOnTop;
    @FXML private HBox searchBarContainer;

    private final UndoManager undoManager;
    private final LibraryTabContainer libraryTabContainer;

    @Inject private PreferencesService preferencesService;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;
    @Inject private TaskExecutor taskExecutor;

    private GlobalSearchResultDialogViewModel viewModel;

    public GlobalSearchResultDialog(UndoManager undoManager, LibraryTabContainer libraryTabContainer) {
        this.undoManager = undoManager;
        this.libraryTabContainer = libraryTabContainer;

        setTitle(Localization.lang("Search results from open libraries"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        initModality(Modality.NONE);
    }

    @FXML
    private void initialize() {
        viewModel = new GlobalSearchResultDialogViewModel(preferencesService);

        GlobalSearchBar searchBar = new GlobalSearchBar(libraryTabContainer, stateManager, preferencesService, undoManager, dialogService, SearchType.GLOBAL_SEARCH);
        searchBarContainer.getChildren().addFirst(searchBar);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        PreviewViewer previewViewer = new PreviewViewer(viewModel.getSearchDatabaseContext(), dialogService, preferencesService, stateManager, themeManager, taskExecutor);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());

        SearchResultsTableDataModel model = new SearchResultsTableDataModel(viewModel.getSearchDatabaseContext(), preferencesService, stateManager);
        SearchResultsTable resultsTable = new SearchResultsTable(model, viewModel.getSearchDatabaseContext(), preferencesService, undoManager, dialogService, stateManager, taskExecutor);

        resultsTable.getColumns().removeIf(SpecialFieldColumn.class::isInstance);

        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                previewViewer.setEntry(newValue.getEntry());
            } else {
                previewViewer.setEntry(oldValue.getEntry());
            }
        });

        Stage stage = (Stage) getDialogPane().getScene().getWindow();

        resultsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                var selectedEntry = resultsTable.getSelectionModel().getSelectedItem();
                libraryTabContainer.getLibraryTabs().stream()
                                   .filter(tab -> tab.getBibDatabaseContext().equals(selectedEntry.getBibDatabaseContext()))
                                   .findFirst()
                                   .ifPresent(libraryTabContainer::showLibraryTab);

                stateManager.clearSearchQuery();
                stateManager.activeTabProperty().get().ifPresent(tab -> tab.clearAndSelect(selectedEntry.getEntry()));
                stage.close();
            }
        });

        container.getItems().addAll(resultsTable, previewViewer);

        keepOnTop.selectedProperty().bindBidirectional(viewModel.keepOnTop());

        EasyBind.subscribe(viewModel.keepOnTop(), value -> {
            stage.setAlwaysOnTop(value);
            keepOnTop.setGraphic(value
                    ? IconTheme.JabRefIcons.KEEP_ON_TOP.getGraphicNode()
                    : IconTheme.JabRefIcons.KEEP_ON_TOP_OFF.getGraphicNode());
        });

        stage.setOnShown(event -> {
            stage.setHeight(preferencesService.getSearchPreferences().getSearchWindowHeight());
            stage.setWidth(preferencesService.getSearchPreferences().getSearchWindowWidth());
        });

        stage.setOnHidden(event -> {
            preferencesService.getSearchPreferences().setSearchWindowHeight(getHeight());
            preferencesService.getSearchPreferences().setSearchWindowWidth(getWidth());
        });
    }
}
