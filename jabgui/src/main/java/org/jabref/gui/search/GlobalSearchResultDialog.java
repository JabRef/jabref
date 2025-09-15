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
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import jakarta.inject.Inject;

public class GlobalSearchResultDialog extends BaseDialog<Void> {

    @FXML private SplitPane container;
    @FXML private ToggleButton keepOnTop;
    @FXML private HBox searchBarContainer;

    private final UndoManager undoManager;
    private final LibraryTabContainer libraryTabContainer;

    // Reference needs to be kept, since java garbage collection would otherwise destroy the subscription
    @SuppressWarnings("FieldCanBeLocal") private Subscription keepOnTopSubscription;

    @Inject private GuiPreferences preferences;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;
    @Inject private TaskExecutor taskExecutor;

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
        GlobalSearchResultDialogViewModel viewModel = new GlobalSearchResultDialogViewModel(preferences.getSearchPreferences());

        GlobalSearchBar searchBar = new GlobalSearchBar(libraryTabContainer, stateManager, preferences, undoManager, dialogService, SearchType.GLOBAL_SEARCH);
        searchBarContainer.getChildren().addFirst(searchBar);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        PreviewViewer previewViewer = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor, stateManager.searchQueryProperty());
        previewViewer.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setDatabaseContext(viewModel.getSearchDatabaseContext());

        SearchResultsTableDataModel model = new SearchResultsTableDataModel(viewModel.getSearchDatabaseContext(), preferences, stateManager, taskExecutor);
        SearchResultsTable resultsTable = new SearchResultsTable(model, viewModel.getSearchDatabaseContext(), preferences, undoManager, dialogService, stateManager, taskExecutor);

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
                BibEntryTableViewModel selectedEntry = resultsTable.getSelectionModel().getSelectedItem();
                if (selectedEntry == null) {
                    return;
                }
                libraryTabContainer.getLibraryTabs().stream()
                                   .filter(tab -> tab.getBibDatabaseContext().equals(selectedEntry.getBibDatabaseContext()))
                                   .findFirst()
                                   .ifPresent(libraryTabContainer::showLibraryTab);

                stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).set(stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).get());
                stateManager.activeTabProperty().get().ifPresent(tab -> tab.clearAndSelect(selectedEntry.getEntry()));
                if (!keepOnTop.isSelected()) {
                    stage.hide();
                }
            }
        });

        container.getItems().addAll(resultsTable, previewViewer);

        keepOnTop.selectedProperty().bindBidirectional(viewModel.keepOnTop());

        keepOnTopSubscription = EasyBind.subscribe(viewModel.keepOnTop(), value -> {
            stage.setAlwaysOnTop(value);
            keepOnTop.setGraphic(value
                                 ? IconTheme.JabRefIcons.KEEP_ON_TOP.getGraphicNode()
                                 : IconTheme.JabRefIcons.KEEP_ON_TOP_OFF.getGraphicNode());
        });

        stage.setOnShown(event -> {
            stage.setHeight(preferences.getSearchPreferences().getSearchWindowHeight());
            stage.setWidth(preferences.getSearchPreferences().getSearchWindowWidth());
            container.setDividerPositions(preferences.getSearchPreferences().getSearchWindowDividerPosition());
            searchBar.requestFocus();
        });

        stage.setOnHidden(event -> {
            preferences.getSearchPreferences().setSearchWindowHeight(getHeight());
            preferences.getSearchPreferences().setSearchWindowWidth(getWidth());
            preferences.getSearchPreferences().setSearchWindowDividerPosition(container.getDividers().getFirst().getPosition());
        });
    }
}
