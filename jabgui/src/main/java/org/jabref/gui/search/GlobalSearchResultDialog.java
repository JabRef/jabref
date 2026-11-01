package org.jabref.gui.search;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.TransferMode;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSearchResultDialog extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchResultDialog.class);

    @FXML private SplitPane container;
    @FXML private ToggleButton keepOnTop;
    @FXML private HBox searchBarContainer;

    private final LibraryTabContainer libraryTabContainer;

    // Reference needs to be kept, since java garbage collection would otherwise destroy the subscription
    @SuppressWarnings("FieldCanBeLocal") private Subscription keepOnTopSubscription;

    @Inject private GuiPreferences preferences;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private BibEntryTypesManager entryTypesManager;
    @Inject private ClipBoardManager clipBoardManager;

    public GlobalSearchResultDialog(LibraryTabContainer libraryTabContainer) {
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

        GlobalSearchBar searchBar = new GlobalSearchBar(libraryTabContainer, stateManager, preferences, dialogService, SearchType.GLOBAL_SEARCH);
        searchBarContainer.getChildren().addFirst(searchBar);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        PreviewViewer previewViewer = new PreviewViewer(dialogService, preferences, taskExecutor, stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH));
        previewViewer.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setDatabaseContext(viewModel.getSearchDatabaseContext());

        SearchResultsTableDataModel model = new SearchResultsTableDataModel(viewModel.getSearchDatabaseContext(), preferences, stateManager, taskExecutor);
        SearchResultsTable resultsTable = new SearchResultsTable(model, viewModel.getSearchDatabaseContext(), preferences, dialogService, stateManager, taskExecutor);

        resultsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        resultsTable.getColumns().removeIf(SpecialFieldColumn.class::isInstance);

        resultsTable.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                previewViewer.setEntry(newValue.getEntry());
            } else {
                previewViewer.clearEntry();
            }
        });

        Stage stage = (Stage) getDialogPane().getScene().getWindow();

        setupTableDoubleClickHandler(resultsTable, stage);
        setupCopyActionBindingHandler(resultsTable);

        container.getItems().addAll(resultsTable, previewViewer);

        keepOnTop.selectedProperty().bindBidirectional(viewModel.keepOnTop());

        keepOnTopSubscription = EasyBind.subscribe(viewModel.keepOnTop(), value -> {
            stage.setAlwaysOnTop(value);
            keepOnTop.setGraphic(value
                                 ? IconTheme.JabRefIcons.KEEP_ON_TOP.getGraphicNode()
                                 : IconTheme.JabRefIcons.KEEP_ON_TOP_OFF.getGraphicNode());
        });

        stage.setOnShown(_ -> {
            stage.setHeight(preferences.getSearchPreferences().getSearchWindowHeight());
            stage.setWidth(preferences.getSearchPreferences().getSearchWindowWidth());
            container.setDividerPositions(preferences.getSearchPreferences().getSearchWindowDividerPosition());
            searchBar.requestFocus();
        });

        stage.setOnHidden(_ -> {
            preferences.getSearchPreferences().setSearchWindowHeight(getHeight());
            preferences.getSearchPreferences().setSearchWindowWidth(getWidth());
            preferences.getSearchPreferences().setSearchWindowDividerPosition(container.getDividers().getFirst().getPosition());
        });
    }

    private void setupCopyActionBindingHandler(SearchResultsTable resultsTable) {
        resultsTable.addEventFilter(KeyEvent.KEY_PRESSED, event ->
                preferences.getKeyBindingRepository().mapToKeyBinding(event)
                           .filter(KeyBinding.COPY::equals)
                           .ifPresent(_ -> {
                               List<BibEntryTableViewModel> selected = resultsTable.getSelectionModel().getSelectedItems();
                               if (!selected.isEmpty()) {
                                   BibDatabaseContext ctx = selected.getFirst().getBibDatabaseContext();
                                   List<BibEntry> entries = selected.stream().map(BibEntryTableViewModel::getEntry).toList();
                                   List<BibtexString> strings = ctx.getDatabase().getUsedStrings(entries);

                                   try {
                                       clipBoardManager.setContent(TransferMode.COPY, ctx, entries, entryTypesManager, strings);
                                       dialogService.notify(Localization.lang("Copied %0 entry(s)", entries.size()));
                                   } catch (IOException ex) {
                                       LOGGER.error("Error while copying the selected entry to clipboard", ex);
                                   }
                                   event.consume();
                               }
                           }));
    }

    private void setupTableDoubleClickHandler(SearchResultsTable resultsTable, Stage stage) {
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
    }
}
