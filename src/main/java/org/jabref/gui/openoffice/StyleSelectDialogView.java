package org.jabref.gui.openoffice;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
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
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
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
    private final JStyleLoader loader;

    // JStyles TableView
    @FXML private TableColumn<StyleSelectItemViewModel, String> colName;
    @FXML private TableView<StyleSelectItemViewModel> tvStyles;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colJournals;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colFile;
    @FXML private TableColumn<StyleSelectItemViewModel, Boolean> colDeleteIcon;
    @FXML private Button add;

    // CSL Styles TableView
    @FXML private TableView<CitationStyleViewModel> cslStylesTable;
    @FXML private TableColumn<CitationStyleViewModel, String> cslNameColumn;
    @FXML private TableColumn<CitationStyleViewModel, String> cslPathColumn;
    @FXML private TableColumn<CitationStyleViewModel, Boolean> cslDeleteColumn;

    @FXML private VBox jstylePreviewBox;
    @FXML private VBox cslPreviewBox;
    private final AtomicBoolean initialScrollPerformed = new AtomicBoolean(false);
    @FXML private CustomTextField searchBox;
    @FXML private TabPane tabPane;
    @FXML private Label currentStyleNameLabel;
    @FXML private Button addCslButton;

    @Inject private GuiPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private ThemeManager themeManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private BibEntryTypesManager bibEntryTypesManager;

    private StyleSelectDialogViewModel viewModel;
    private PreviewViewer previewArticle;
    private PreviewViewer previewBook;

    /**
     * ViewModel for the CitationStyle entries in the TableView
     */
    public static class CitationStyleViewModel {
        private final CitationStylePreviewLayout layout;
        private final StringProperty nameProperty = new SimpleStringProperty();
        private final StringProperty pathProperty = new SimpleStringProperty();
        private final BooleanProperty internalStyleProperty = new SimpleBooleanProperty();

        public CitationStyleViewModel(CitationStylePreviewLayout layout) {
            this.layout = layout;
            this.nameProperty.set(layout.getDisplayName());
            if (layout.getCitationStyle().isInternalStyle()) {
                this.pathProperty.set(Localization.lang("Internal style"));
            } else {
                this.pathProperty.set(layout.getFilePath());
            }
            this.internalStyleProperty.set(layout.getCitationStyle().isInternalStyle());
        }

        public StringProperty nameProperty() {
            return nameProperty;
        }

        public StringProperty pathProperty() {
            return pathProperty;
        }

        public BooleanProperty internalStyleProperty() {
            return internalStyleProperty;
        }

        public CitationStylePreviewLayout getLayout() {
            return layout;
        }
    }

    public StyleSelectDialogView(JStyleLoader loader) {
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

        setupJStylesTab();
        setupCslStylesTab();

        OOStyle currentStyle = preferences.getOpenOfficePreferences().getCurrentStyle();
        if (currentStyle instanceof JStyle) {
            tabPane.getSelectionModel().select(1);
        } else {
            tabPane.getSelectionModel().select(0);
        }

        viewModel.setSelectedTab(tabPane.getSelectionModel().getSelectedItem());
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> viewModel.setSelectedTab(newValue));

        updateCurrentStyleLabel();
        addCslButton.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

        this.setOnShown(this::onDialogShown);
    }

    private void setupJStylesTab() {
        // Setup JStyles table columns
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colJournals.setCellValueFactory(cellData -> cellData.getValue().journalsProperty());
        colFile.setCellValueFactory(cellData -> cellData.getValue().fileProperty());
        colDeleteIcon.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<StyleSelectItemViewModel, Boolean>()
                .withGraphic(internalStyle -> internalStyle ? null : IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent(item -> evt -> viewModel.deleteJStyle())
                .withTooltip(item -> Localization.lang("Remove style"))
                .install(colDeleteIcon);

        edit.setOnAction(e -> viewModel.editJStyle());

        new ViewModelTableRowFactory<StyleSelectItemViewModel>()
                .withOnMouseClickedEvent((item, event) -> {
                    if (event.getClickCount() == 2) {
                        viewModel.viewJStyle(item);
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

        tvStyles.setItems(viewModel.jStylesProperty());

        add.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

        // JStyle previews
        previewArticle = initializePreviewViewer(TestEntry.getTestEntry());
        jstylePreviewBox.getChildren().add(previewArticle);

        previewBook = initializePreviewViewer(TestEntry.getTestEntryBook());
        jstylePreviewBox.getChildren().add(previewBook);

        EasyBind.subscribe(viewModel.selectedItemProperty(), style -> {
            if (viewModel.getSelectedStyle() instanceof JStyle) {
                tvStyles.getSelectionModel().select(style);
                previewArticle.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Article)));
                previewBook.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Book)));
            }
        });
    }

    private void setupCslStylesTab() {
        // Set up CSL styles table columns
        cslNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        cslPathColumn.setCellValueFactory(cellData -> cellData.getValue().pathProperty());
        cslDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<CitationStyleViewModel, Boolean>()
                .withGraphic(internalStyle -> internalStyle ? null : IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent(item -> evt -> {
                    CitationStyleViewModel selectedStyle = cslStylesTable.getSelectionModel().getSelectedItem();
                    if (selectedStyle != null) {
                        viewModel.deleteCslStyle(selectedStyle.getLayout().getCitationStyle());
                    }
                })
                .withTooltip(item -> Localization.lang("Remove style"))
                .install(cslDeleteColumn);

        // Double-click to select a style (Only CSL styles can be selected with a double click, JStyles show a style description instead)
        new ViewModelTableRowFactory<CitationStyleViewModel>()
                .withOnMouseClickedEvent((item, event) -> {
                    if (event.getClickCount() == 2) {
                        viewModel.selectedLayoutProperty().set(item.getLayout());
                        viewModel.handleCslStyleSelection();
                        this.setResult(viewModel.getSelectedStyle());
                        this.close();
                    }
                })
                .install(cslStylesTable);

        cslStylesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                viewModel.selectedLayoutProperty().set(newValue.getLayout());
            }
        });

        searchBox.textProperty().addListener((observable, oldValue, newValue) -> {
            viewModel.setAvailableLayoutsFilter(newValue);
            updateCslStylesTable();
        });

        PreviewViewer cslPreviewViewer = initializePreviewViewer(TestEntry.getTestEntry());
        EasyBind.subscribe(viewModel.selectedLayoutProperty(), cslPreviewViewer::setLayout);
        cslPreviewBox.getChildren().add(cslPreviewViewer);

        viewModel.getAvailableLayouts().addListener((ListChangeListener<CitationStylePreviewLayout>) c -> {
            updateCslStylesTable();
            if (c.next() && c.wasAdded() && !initialScrollPerformed.get()) {
                Platform.runLater(this::scrollToCurrentStyle); // taking care of slight delay in table population
            }
        });

        updateCslStylesTable();
    }

    private void updateCslStylesTable() {
        cslStylesTable.getItems().clear();
        for (CitationStylePreviewLayout layout : viewModel.getAvailableLayouts()) {
            cslStylesTable.getItems().add(new CitationStyleViewModel(layout));
        }

        if (viewModel.selectedLayoutProperty().get() != null) {
            for (CitationStyleViewModel model : cslStylesTable.getItems()) {
                if (model.getLayout().equals(viewModel.selectedLayoutProperty().get())) {
                    cslStylesTable.getSelectionModel().select(model);
                    break;
                }
            }
        }
    }

    private ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(edit, reload);
        return contextMenu;
    }

    @FXML
    private void modifyBibliographyTitle() {
        ModifyCSLBibliographyTitleDialogView modifyBibliographyTitleDialogView = new ModifyCSLBibliographyTitleDialogView(preferences.getOpenOfficePreferences());
        dialogService.showCustomDialog(modifyBibliographyTitleDialogView);
    }

    @FXML
    private void addStyleFile() {
        viewModel.addJStyleFile();
    }

    private PreviewViewer initializePreviewViewer(BibEntry entry) {
        PreviewViewer viewer = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);
        viewer.setDatabaseContext(new BibDatabaseContext());
        viewer.setEntry(entry);
        return viewer;
    }

    private void updateCurrentStyleLabel() {
        currentStyleNameLabel.setText(viewModel.getSetStyle().getName());
    }

    /**
     * When Select Style dialog is first opened, there is a slight delay in population of CSL styles table.
     * This function scrolls to the last selected style, while taking care of the delay.
     */
    private void onDialogShown(DialogEvent event) {
        if (!cslStylesTable.getItems().isEmpty()) {
            Platform.runLater(this::scrollToCurrentStyle);
        }
    }

    private void scrollToCurrentStyle() {
        if (initialScrollPerformed.getAndSet(true)) {
            return; // Scroll has already been performed, exit early
        }

        OOStyle currentStyle = preferences.getOpenOfficePreferences().getCurrentStyle();
        if (currentStyle instanceof CitationStyle currentCitationStyle) {
            for (int i = 0; i < cslStylesTable.getItems().size(); i++) {
                CitationStyleViewModel item = cslStylesTable.getItems().get(i);
                if (item.getLayout().getFilePath().equals(currentCitationStyle.getFilePath())) {
                    cslStylesTable.scrollTo(i);
                    cslStylesTable.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }

    /**
     * Method to handle the "Add CSL file" button click in the CSL Styles tab
     */
    @FXML
    private void addCslStyleFile() {
        viewModel.addCslStyleFile();
    }
}
