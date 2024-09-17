package org.jabref.gui.openoffice;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

public class StyleSelectDialogView extends BaseDialog<OOStyle> {

    private final MenuItem edit = new MenuItem(Localization.lang("Edit"));
    private final MenuItem reload = new MenuItem(Localization.lang("Reload"));
    private final StyleLoader loader;

    @FXML private TableColumn<StyleSelectItemViewModel, String> colName;
    @FXML private TableView<StyleSelectItemViewModel> tvStyles;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colJournals;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colFile;
    @FXML private TableColumn<StyleSelectItemViewModel, Boolean> colDeleteIcon;
    @FXML private Button add;
    @FXML private VBox jstylePreviewBox;
    @FXML private VBox cslPreviewBox;
    @FXML private ListView<CitationStylePreviewLayout> availableListView;
    private final AtomicBoolean initialScrollPerformed = new AtomicBoolean(false);
    @FXML private CustomTextField searchBox;
    @FXML private TabPane tabPane;
    @FXML private Label currentStyleNameLabel;

    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private BibEntryTypesManager bibEntryTypesManager;

    private StyleSelectDialogViewModel viewModel;
    private PreviewViewer previewArticle;
    private PreviewViewer previewBook;

    public StyleSelectDialogView(StyleLoader loader) {
        this.loader = loader;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.storePrefs();
                return viewModel.getSelectedStyle();
            }
            return null;
        });
        setTitle(Localization.lang("Style selection"));
    }

    @FXML
    private void initialize() {
        viewModel = new StyleSelectDialogViewModel(dialogService, loader, preferences, taskExecutor, bibEntryTypesManager);

        availableListView.setItems(viewModel.getAvailableLayouts());
        new ViewModelListCellFactory<CitationStylePreviewLayout>()
                .withText(CitationStylePreviewLayout::getDisplayName)
                .install(availableListView);

        this.setOnShown(this::onDialogShown);

        availableListView.getItems().addListener((ListChangeListener<CitationStylePreviewLayout>) c -> {
            if (c.next() && c.wasAdded() && !initialScrollPerformed.get()) {
                Platform.runLater(this::scrollToCurrentStyle);
            }
        });

        availableListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                viewModel.selectedLayoutProperty().set(newValue));

        PreviewViewer cslPreviewViewer = initializePreviewViewer(TestEntry.getTestEntry());
        EasyBind.subscribe(viewModel.selectedLayoutProperty(), cslPreviewViewer::setLayout);
        cslPreviewBox.getChildren().add(cslPreviewViewer);

        previewArticle = initializePreviewViewer(TestEntry.getTestEntry());
        jstylePreviewBox.getChildren().add(previewArticle);

        previewBook = initializePreviewViewer(TestEntry.getTestEntryBook());
        jstylePreviewBox.getChildren().add(previewBook);

        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colJournals.setCellValueFactory(cellData -> cellData.getValue().journalsProperty());
        colFile.setCellValueFactory(cellData -> cellData.getValue().fileProperty());
        colDeleteIcon.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<StyleSelectItemViewModel, Boolean>()
                .withGraphic(internalStyle -> {
                    if (!internalStyle) {
                        return IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode();
                    }
                    return null;
                })
                .withOnMouseClickedEvent(item -> evt -> viewModel.deleteStyle())
                .withTooltip(item -> Localization.lang("Remove style"))
                .install(colDeleteIcon);

        edit.setOnAction(e -> viewModel.editStyle());

        new ViewModelTableRowFactory<StyleSelectItemViewModel>()
                .withOnMouseClickedEvent((item, event) -> {
                    if (event.getClickCount() == 2) {
                        viewModel.viewStyle(item);
                    }
                })
                .withContextMenu(item -> createContextMenu())
                .install(tvStyles);

        tvStyles.getSelectionModel().selectedItemProperty().addListener((observable, oldvalue, newvalue) -> {
            if (newvalue == null) {
                viewModel.selectedItemProperty().setValue(oldvalue);
            } else {
                viewModel.selectedItemProperty().setValue(newvalue);
            }
        });

        tvStyles.setItems(viewModel.stylesProperty());

        add.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

        EasyBind.subscribe(viewModel.selectedItemProperty(), style -> {
            if (viewModel.getSelectedStyle() instanceof JStyle) {
                tvStyles.getSelectionModel().select(style);
                previewArticle.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Article)));
                previewBook.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Book)));
            }
        });

        availableListView.setItems(viewModel.getAvailableLayouts());
        searchBox.textProperty().addListener((observable, oldValue, newValue) ->
                viewModel.setAvailableLayoutsFilter(newValue));

        viewModel.setSelectedTab(tabPane.getSelectionModel().getSelectedItem());
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.setSelectedTab(newValue);
        });

        availableListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                viewModel.handleCslStyleSelection(); // Only CSL styles can be selected with a double click, JStyles show a style description instead
                this.setResult(viewModel.getSelectedStyle());
                this.close();
            }
        });

        OOStyle currentStyle = preferences.getOpenOfficePreferences().getCurrentStyle();
        if (currentStyle instanceof JStyle) {
            tabPane.getSelectionModel().select(1);
        } else {
            tabPane.getSelectionModel().select(0);
        }

        viewModel.setSelectedTab(tabPane.getSelectionModel().getSelectedItem());
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.setSelectedTab(newValue);
        });

        updateCurrentStyleLabel();
    }

    private ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(edit, reload);
        return contextMenu;
    }

    @FXML
    private void addStyleFile() {
        viewModel.addStyleFile();
    }

    private PreviewViewer initializePreviewViewer(BibEntry entry) {
        PreviewViewer viewer = new PreviewViewer(new BibDatabaseContext(), dialogService, preferences, themeManager, taskExecutor);
        viewer.setEntry(entry);
        return viewer;
    }

    private void updateCurrentStyleLabel() {
        currentStyleNameLabel.setText(viewModel.getSetStyle().getName());
    }

    /**
     * On a new run of JabRef, when Select Style dialog is opened for the first time, the CSL styles list takes a while to load.
     * This function takes care of the case when the list is empty due to the initial loading time.
     * If the list is empty, the ListChangeListener will handle scrolling when items are added.
     */
    private void onDialogShown(DialogEvent event) {
        if (!availableListView.getItems().isEmpty()) {
            Platform.runLater(this::scrollToCurrentStyle);
        }
    }

    private void scrollToCurrentStyle() {
        if (initialScrollPerformed.getAndSet(true)) {
            return; // Scroll has already been performed, exit early
        }

        OOStyle currentStyle = preferences.getOpenOfficePreferences().getCurrentStyle();
        if (currentStyle instanceof CitationStyle currentCitationStyle) {
            findIndexOfCurrentStyle(currentCitationStyle).ifPresent(index -> {
                int itemsPerPage = calculateItemsPerPage();
                int totalItems = availableListView.getItems().size();
                int scrollToIndex = Math.max(0, Math.min(index, totalItems - itemsPerPage));

                availableListView.scrollTo(scrollToIndex);
                availableListView.getSelectionModel().select(index);

                Platform.runLater(() -> {
                    availableListView.scrollTo(Math.max(0, index - 1));
                    availableListView.scrollTo(index);
                });
            });
        }
    }

    private int calculateItemsPerPage() {
        double cellHeight = 24.0; // Approximate height of a list cell
        return (int) (availableListView.getHeight() / cellHeight);
    }

    private Optional<Integer> findIndexOfCurrentStyle(CitationStyle currentStyle) {
        return IntStream.range(0, availableListView.getItems().size())
                        .boxed()
                        .filter(i -> availableListView.getItems().get(i).getFilePath().equals(currentStyle.getFilePath()))
                        .findFirst();
    }
}
