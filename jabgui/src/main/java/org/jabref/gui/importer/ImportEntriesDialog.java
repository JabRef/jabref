package org.jabref.gui.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.BibEntryView;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.CheckListView;
import org.fxmisc.richtext.CodeArea;

public class ImportEntriesDialog extends BaseDialog<Boolean> {
    @FXML private HBox paginationBox;
    @FXML private Label pageNumberLabel;
    @FXML private CheckListView<BibEntry> entriesListView;
    @FXML private ComboBox<BibDatabaseContext> libraryListView;
    @FXML private ComboBox<GroupTreeNode> groupListView;
    @FXML private ButtonType importButton;
    @FXML private Label totalItems;
    @FXML private Label selectedItems;
    @FXML private Label bibTeXDataLabel;
    @FXML private CheckBox downloadLinkedOnlineFiles;
    @FXML private CheckBox showEntryInformation;
    @FXML private CodeArea bibTeXData;
    @FXML private VBox bibTeXDataBox;
    @FXML private Button nextPageButton;
    @FXML private Button prevPageButton;
    @FXML private Label statusLabel;

    private final BackgroundTask<ParserResult> task;
    private final BibDatabaseContext database;
    private ImportEntriesViewModel viewModel;
    private final Optional<SearchBasedFetcher> searchBasedFetcher;
    private final Optional<String> query;

    @Inject private TaskExecutor taskExecutor;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;
    @Inject private GuiPreferences preferences;
    @Inject private StateManager stateManager;
    @Inject private BibEntryTypesManager entryTypesManager;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    @Inject private DirectoryUpdateMonitor directoryUpdateMonitor;

    /**
     * Creates an import dialog for entries from file sources.
     * This constructor is used for importing entries from local files, BibTeX files,
     * or other file-based sources that don't require pagination or search functionality.
     *
     * @param database the database to import into
     * @param task     the task executed for parsing the selected files(s).
     */
    public ImportEntriesDialog(BibDatabaseContext database, BackgroundTask<ParserResult> task) {
        this.database = database;
        this.task = task;
        this.searchBasedFetcher = Optional.empty();
        this.query = Optional.empty();

        initializeDialog();
    }

    /**
     * Creates an import dialog for entries from web-based search sources.
     * This constructor is used for importing entries that support pagination and require search queries.
     *
     * @param database database where the imported entries will be added
     * @param task     task that handles parsing and loading entries from the search results
     * @param fetcher  the search-based fetcher implementation used to retrieve entries from the web source
     * @param query    the search string used to find relevant entries
     */
    public ImportEntriesDialog(BibDatabaseContext database, BackgroundTask<ParserResult> task, SearchBasedFetcher fetcher, String query) {
        this.database = database;
        this.task = task;
        this.searchBasedFetcher = Optional.of(fetcher);
        this.query = Optional.of(query);

        initializeDialog();
    }

    @FXML
    private void initialize() {
        viewModel = new ImportEntriesViewModel(task, taskExecutor, database, dialogService, undoManager, preferences, stateManager, entryTypesManager, fileUpdateMonitor, directoryUpdateMonitor, searchBasedFetcher, query);
        Label placeholder = new Label();
        placeholder.textProperty().bind(viewModel.messageProperty());
        entriesListView.setPlaceholder(placeholder);
        entriesListView.setItems(viewModel.getEntries());
        entriesListView.getCheckModel().getCheckedItems().addListener((ListChangeListener<BibEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (BibEntry entry : change.getAddedSubList()) {
                        viewModel.getCheckedEntries().add(entry);
                    }
                }
                if (change.wasRemoved()) {
                    for (BibEntry entry : change.getRemoved()) {
                        viewModel.getCheckedEntries().remove(entry);
                    }
                }
            }
        });

        libraryListView.setEditable(false);
        groupListView.setEditable(false);
        libraryListView.getItems().addAll(stateManager.getOpenDatabases());
        new ViewModelListCellFactory<BibDatabaseContext>()
                .withText(database -> {
                    Optional<String> dbOpt = Optional.empty();
                    if (database.getDatabasePath().isPresent()) {
                        dbOpt = FileUtil.getUniquePathFragment(stateManager.getAllDatabasePaths(), database.getDatabasePath().get());
                    }
                    if (database.getLocation() == DatabaseLocation.SHARED) {
                        return database.getDBMSSynchronizer().getDBName() + " [" + Localization.lang("shared") + "]";
                    }

                    return dbOpt.orElseGet(() -> Localization.lang("untitled"));
                })
                .install(libraryListView);
        viewModel.selectedDbProperty().bind(libraryListView.getSelectionModel().selectedItemProperty());
        stateManager.getActiveDatabase().ifPresent(database1 -> libraryListView.getSelectionModel().select(database1));
        setupGroupListView();

        PseudoClass entrySelected = PseudoClass.getPseudoClass("selected");
        new ViewModelListCellFactory<BibEntry>()
                .withGraphic(entry -> {
                    ToggleButton addToggle = IconTheme.JabRefIcons.ADD.asToggleButton();
                    EasyBind.subscribe(addToggle.selectedProperty(), selected -> {
                        if (selected) {
                            addToggle.setGraphic(IconTheme.JabRefIcons.ADD_FILLED.withColor(IconTheme.SELECTED_COLOR).getGraphicNode());
                        } else {
                            addToggle.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
                        }
                    });
                    addToggle.getStyleClass().add("addEntryButton");
                    addToggle.selectedProperty().bindBidirectional(entriesListView.getItemBooleanProperty(entry));
                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = BibEntryView.getEntryNode(entry);
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox container = new HBox(entryNode, separator, addToggle);
                    container.getStyleClass().add("entry-container");
                    container.prefWidthProperty().bind(entriesListView.widthProperty().subtract(25));

                    BackgroundTask.wrap(() -> viewModel.hasDuplicate(entry)).onSuccess(duplicateFound -> {
                        if (duplicateFound) {
                            Node icon = IconTheme.JabRefIcons.ERROR.getGraphicNode();
                            Tooltip tooltip = new Tooltip(Localization.lang("Possible duplicate of existing entry. Will be resolved on import."));
                            Tooltip.install(icon, tooltip);
                            container.getChildren().add(icon);
                        }
                    }).executeWith(taskExecutor);

                    /*
                    inserted the if-statement here, since a Platform.runLater() call did not work.
                    also tried to move it to the end of the initialize method, but it did not select the entry.
                    */
                    if (entriesListView.getItems().size() == 1) {
                        selectAllNewEntries();
                    }

                    return container;
                })
                .withOnMouseClickedEvent((entry, event) -> {
                    entriesListView.getCheckModel().toggleCheckState(entry);
                    displayBibTeX(entry, viewModel.getSourceString(entry));
                })
                .withPseudoClass(entrySelected, entriesListView::getItemBooleanProperty)
                .install(entriesListView);

        selectedItems.textProperty().bind(Bindings.size(viewModel.getCheckedEntries()).asString());
        totalItems.textProperty().bind(Bindings.size(viewModel.getAllEntries()).asString());
        entriesListView.setSelectionModel(new NoSelectionModel<>());
        initBibTeX();
        if (searchBasedFetcher.isPresent()) {
            updatePageUI();
            setupPaginationBindings();
        }
    }

    private void setupGroupListView() {
        groupListView.setVisibleRowCount(5);
        updateGroupList();
        libraryListView.getSelectionModel().selectedItemProperty()
                       .addListener((_, _, _) -> {
                           updateGroupList();
                       });

        new ViewModelListCellFactory<GroupTreeNode>()
                .withText(group -> group != null ? group.getName() : Localization.lang("No group"))
                .install(groupListView);
    }

    private void updateGroupList() {
        groupListView.getItems().clear();

        BibDatabaseContext selectedDb = libraryListView.getSelectionModel().getSelectedItem();
        if (selectedDb.getMetaData().getGroups().isPresent()) {
            GroupTreeNode rootGroup = selectedDb.getMetaData().getGroups().get();
            groupListView.getItems().add(rootGroup);

            List<GroupTreeNode> allGroups = new ArrayList<>();
            collectGroupsFromTree(rootGroup, allGroups);
            allGroups.sort((g1, g2) -> g1.getName().compareToIgnoreCase(g2.getName()));

            groupListView.getItems().addAll(allGroups);
            groupListView.getSelectionModel().select(stateManager.getSelectedGroups(selectedDb).getFirst());
        } else {
            // No groups defined -> only "All entries"
            GroupTreeNode noGroup = new GroupTreeNode(new AllEntriesGroup(Localization.lang("All entries")));
            groupListView.getItems().add(noGroup);
            groupListView.getSelectionModel().select(noGroup);
        }
    }

    private void collectGroupsFromTree(GroupTreeNode parent, List<GroupTreeNode> groupList) {
        for (GroupTreeNode child : parent.getChildren()) {
            groupList.add(child);
            collectGroupsFromTree(child, groupList);
        }
    }

    private void initializeDialog() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        paginationBox.setVisible(searchBasedFetcher.isPresent());
        paginationBox.setManaged(searchBasedFetcher.isPresent());

        BooleanBinding booleanBind = Bindings.isEmpty(entriesListView.getCheckModel().getCheckedItems());
        Button btn = (Button) this.getDialogPane().lookupButton(importButton);
        btn.disableProperty().bind(booleanBind);

        downloadLinkedOnlineFiles.setSelected(preferences.getFilePreferences().shouldDownloadLinkedFiles());

        setResultConverter(button -> {
            if (button == importButton) {
                if (groupListView.getItems().size() > 1) {
                    // 1 is the "All entries" group, so if more than 1, we have groups defined
                    GroupTreeNode prevSelectedGroup = stateManager.getSelectedGroups(stateManager.getActiveDatabase().orElse(null)).getFirst();
                    stateManager.setSelectedGroups(libraryListView.getSelectionModel().getSelectedItem(), List.of(groupListView.getSelectionModel().getSelectedItem()));
                    viewModel.importEntries(viewModel.getCheckedEntries().stream().toList(), downloadLinkedOnlineFiles.isSelected());
                    stateManager.setSelectedGroups(stateManager.getActiveDatabase().orElse(null), List.of(prevSelectedGroup));
                } else {
                    viewModel.importEntries(viewModel.getCheckedEntries().stream().toList(), downloadLinkedOnlineFiles.isSelected());
                }
            } else {
                dialogService.notify(Localization.lang("Import canceled"));
            }

            return false;
        });
    }

    private void setupPaginationBindings() {
        BooleanProperty loading = viewModel.loadingProperty();
        BooleanProperty initialLoadComplete = viewModel.initialLoadCompleteProperty();

        BooleanBinding isOnLastPage = Bindings.createBooleanBinding(() -> {
            int currentPage = viewModel.currentPageProperty().get();
            int totalPages = viewModel.totalPagesProperty().get();
            return currentPage >= totalPages - 1;
        }, viewModel.currentPageProperty(), viewModel.totalPagesProperty());

        BooleanBinding isPagedFetcher = Bindings.createBooleanBinding(() ->
                searchBasedFetcher.isPresent() && searchBasedFetcher.get() instanceof PagedSearchBasedFetcher
        );

        // Disable: during loading OR when on the last page for non-paged fetchers
        // OR when the initial load is not complete for paged fetchers
        nextPageButton.disableProperty().bind(
                loading.or(isOnLastPage.and(isPagedFetcher.not()))
                       .or(isPagedFetcher.and(initialLoadComplete.not()))
        );
        prevPageButton.disableProperty().bind(loading.or(viewModel.currentPageProperty().isEqualTo(0)));

        prevPageButton.textProperty().bind(
                Bindings.when(loading)
                        .then("< " + Localization.lang("Loading..."))
                        .otherwise("< " + Localization.lang("Previous"))
        );

        nextPageButton.textProperty().bind(
                Bindings.when(loading)
                        .then(Localization.lang("Loading...") + " >")
                        .otherwise(
                                Bindings.when(initialLoadComplete.not().and(isPagedFetcher))
                                        .then(Localization.lang("Loading initial entries..."))
                                        .otherwise(
                                                Bindings.when(isOnLastPage)
                                                        .then(
                                                                Bindings.when(isPagedFetcher)
                                                                        .then(Localization.lang("Load More") + " >>")
                                                                        .otherwise(Localization.lang("No more entries"))
                                                        )
                                                        .otherwise(Localization.lang("Next") + " >")
                                        )
                        )
        );

        statusLabel.textProperty().bind(
                Bindings.when(loading)
                        .then(Localization.lang("Fetching more entries..."))
                        .otherwise(
                                Bindings.when(initialLoadComplete.not().and(isPagedFetcher))
                                        .then(Localization.lang("Loading initial results..."))
                                        .otherwise(
                                                Bindings.when(isOnLastPage)
                                                        .then(
                                                                Bindings.when(isPagedFetcher)
                                                                        .then(Localization.lang("Click 'Load More' to fetch additional entries"))
                                                                        .otherwise(Bindings.createStringBinding(() -> {
                                                                            int totalEntries = viewModel.getAllEntries().size();
                                                                            return totalEntries > 0 ?
                                                                                   Localization.lang("All %0 entries loaded", String.valueOf(totalEntries)) :
                                                                                   Localization.lang("No entries available");
                                                                        }, viewModel.getAllEntries()))
                                                        )
                                                        .otherwise("")
                                        )
                        )
        );

        loading.addListener((_, _, newVal) -> {
            getDialogPane().getScene().setCursor(newVal ? Cursor.WAIT : Cursor.DEFAULT);
        });

        isOnLastPage.addListener((_, oldVal, newVal) -> {
            if (newVal && !oldVal) {
                statusLabel.getStyleClass().add("info-message");
            } else if (!newVal && oldVal) {
                statusLabel.getStyleClass().remove("info-message");
            }
        });
    }

    private void updatePageUI() {
        pageNumberLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            int totalPages = viewModel.totalPagesProperty().get();
            int currentPage = viewModel.currentPageProperty().get() + 1;
            if (totalPages != 0) {
                return Localization.lang("%0 of %1", currentPage, totalPages);
            }
            return "";
        }, viewModel.currentPageProperty(), viewModel.totalPagesProperty()));

        viewModel.getAllEntries().addListener((ListChangeListener<BibEntry>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    viewModel.updateTotalPages();
                }
            }
        });
    }

    private void displayBibTeX(BibEntry entry, String bibTeX) {
        if (viewModel.getCheckedEntries().contains(entry)) {
            bibTeXData.clear();
            bibTeXData.appendText(bibTeX);
            bibTeXData.moveTo(0);
            bibTeXData.requestFollowCaret();
        } else {
            bibTeXData.clear();
        }
    }

    private void initBibTeX() {
        bibTeXDataLabel.setText(Localization.lang("%0 source", "BibTeX"));
        bibTeXData.setBorder(new Border(new BorderStroke(Color.GREY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        bibTeXData.setPadding(new Insets(5.0));
        showEntryInformation.selectedProperty().addListener((observableValue, old_val, new_val) -> {
            bibTeXDataBox.setVisible(new_val);
            bibTeXDataBox.setManaged(new_val);
        });
    }

    public void unselectAll() {
        viewModel.getCheckedEntries().clear();
        entriesListView.getItems().forEach(entry -> entriesListView.getCheckModel().clearCheck(entry));
    }

    public void selectAllNewEntries() {
        unselectAll();
        for (BibEntry entry : viewModel.getAllEntries()) {
            if (!viewModel.hasDuplicate(entry)) {
                entriesListView.getCheckModel().check(entry);
                viewModel.getCheckedEntries().add(entry);
                displayBibTeX(entry, viewModel.getSourceString(entry));
            }
        }
    }

    public void selectAllEntries() {
        unselectAll();
        entriesListView.getCheckModel().checkAll();
        viewModel.getCheckedEntries().addAll(viewModel.getAllEntries());
    }

    private boolean isOnLastPageAndPagedFetcher() {
        if (searchBasedFetcher.isEmpty() || !(searchBasedFetcher.get() instanceof PagedSearchBasedFetcher)) {
            return false;
        }

        int currentPage = viewModel.currentPageProperty().get();
        int totalPages = viewModel.totalPagesProperty().get();
        return currentPage >= totalPages - 1;
    }

    @FXML
    private void onPrevPage() {
        viewModel.goToPrevPage();
        restoreCheckedEntries();
    }

    @FXML
    private void onNextPage() {
        if (isOnLastPageAndPagedFetcher()) {
            viewModel.fetchMoreEntries();
        } else {
            viewModel.goToNextPage();
        }
        restoreCheckedEntries();
    }

    private void restoreCheckedEntries() {
        for (BibEntry entry : viewModel.getEntries()) {
            if (viewModel.getCheckedEntries().contains(entry)) {
                entriesListView.getCheckModel().check(entry);
            }
        }
    }
}
