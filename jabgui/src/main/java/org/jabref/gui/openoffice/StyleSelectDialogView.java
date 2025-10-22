package org.jabref.gui.openoffice;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
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
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.journals.JournalAbbreviationRepository;
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

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import jakarta.inject.Inject;
import org.controlsfx.control.textfield.CustomTextField;

public class StyleSelectDialogView extends BaseDialog<OOStyle> {

    private final MenuItem edit = new MenuItem(Localization.lang("Edit"));
    private final MenuItem reload = new MenuItem(Localization.lang("Reload"));

    private final CSLStyleLoader cslStyleLoader;
    private final JStyleLoader jStyleLoader;

    @FXML private Tab cslStyleTab;
    @FXML private Tab jStyleTab;

    // CSL Styles TableView
    @FXML private TableView<CSLStyleSelectViewModel> cslStylesTable;
    @FXML private TableColumn<CSLStyleSelectViewModel, String> cslNameColumn;
    @FXML private TableColumn<CSLStyleSelectViewModel, String> cslPathColumn;
    @FXML private TableColumn<CSLStyleSelectViewModel, Boolean> cslDeleteColumn;

    // JStyles TableView
    @FXML private TableView<JStyleSelectViewModel> jStylesTable;
    @FXML private TableColumn<JStyleSelectViewModel, String> jStyleNameColumn;
    @FXML private TableColumn<JStyleSelectViewModel, String> jStyleJournalColumn;
    @FXML private TableColumn<JStyleSelectViewModel, String> jStyleFileColumn;
    @FXML private TableColumn<JStyleSelectViewModel, Boolean> jStyleDeleteColumn;

    @FXML private Button addCslButton;
    @FXML private Button addJStyleButton;

    @FXML private VBox cslPreviewBox;
    @FXML private VBox jStylePreviewBox;

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

    /**
     * ViewModel for the CitationStyle entries in the TableView
     */

    public StyleSelectDialogView(CSLStyleLoader cslStyleLoader, JStyleLoader jStyleLoader) {
        this.cslStyleLoader = cslStyleLoader;
        this.jStyleLoader = jStyleLoader;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.storeStylePreferences();
                return viewModel.getSelectedStyle();
            }
            return null;
        });
        setTitle(Localization.lang("Style selection"));
    }

    @FXML
    private void initialize() {
        viewModel = new StyleSelectDialogViewModel(dialogService, cslStyleLoader, jStyleLoader, preferences, taskExecutor, bibEntryTypesManager);

        setupCslStylesTab();
        setupJStylesTab();

        OOStyle currentStyle = preferences.getOpenOfficePreferences(Injector.instantiateModelOrService(JournalAbbreviationRepository.class)).getCurrentStyle();
        if (currentStyle instanceof CitationStyle) {
            tabPane.getSelectionModel().select(cslStyleTab);
        } else {
            tabPane.getSelectionModel().select(jStyleTab);
        }

        viewModel.setSelectedTab(tabPane.getSelectionModel().getSelectedItem());
        tabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> viewModel.setSelectedTab(newValue));

        updateCurrentStyleLabel();
        addCslButton.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

        this.setOnShown(this::onDialogShown);
    }

    private void setupCslStylesTab() {
        // Set up CSL styles table columns
        cslNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        cslPathColumn.setCellValueFactory(cellData -> cellData.getValue().pathProperty());
        cslDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<CSLStyleSelectViewModel, Boolean>()
                .withGraphic(internalStyle -> internalStyle ? null : IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent(item -> evt -> {
                    CSLStyleSelectViewModel selectedStyle = cslStylesTable.getSelectionModel().getSelectedItem();
                    if (selectedStyle != null) {
                        viewModel.deleteCslStyle(selectedStyle.getLayout().citationStyle());
                    }
                })
                .withTooltip(item -> Localization.lang("Remove style"))
                .install(cslDeleteColumn);

        new ViewModelTableRowFactory<CSLStyleSelectViewModel>()
                .withOnMouseClickedEvent((item, event) -> {
                    if (event.getClickCount() == 2) {
                        viewModel.selectedCslLayoutProperty().set(item.getLayout());
                        viewModel.storeStylePreferences();
                        this.setResult(viewModel.getSelectedStyle());
                        this.close();
                    }
                })
                .install(cslStylesTable);

        cslStylesTable.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                viewModel.selectedCslLayoutProperty().set(newValue.getLayout());
            }
        });

        searchBox.textProperty().addListener((_, _, newValue) -> {
            viewModel.setAvailableCslLayoutsFilter(newValue);
            updateCslStylesTable();
        });

        PreviewViewer cslPreviewViewer = initializePreviewViewer(TestEntry.getTestEntry());
        EasyBind.subscribe(viewModel.selectedCslLayoutProperty(), cslPreviewViewer::setLayout);
        cslPreviewBox.getChildren().add(cslPreviewViewer);

        viewModel.getAvailableCslLayouts().addListener((ListChangeListener<CitationStylePreviewLayout>) c -> {
            updateCslStylesTable();
            if (c.next() && c.wasAdded() && !initialScrollPerformed.get()) {
                Platform.runLater(this::scrollToCurrentStyle); // taking care of slight delay in table population
            }
        });

        updateCslStylesTable();
    }

    private void setupJStylesTab() {
        // Setup JStyles table columns
        jStyleNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        jStyleJournalColumn.setCellValueFactory(cellData -> cellData.getValue().journalsProperty());
        jStyleFileColumn.setCellValueFactory(cellData -> cellData.getValue().fileProperty());
        jStyleDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<JStyleSelectViewModel, Boolean>()
                .withGraphic(internalStyle -> internalStyle ? null : IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withOnMouseClickedEvent(item -> evt -> viewModel.deleteJStyle())
                .withTooltip(item -> Localization.lang("Remove style"))
                .install(jStyleDeleteColumn);

        edit.setOnAction(e -> viewModel.editJStyle());

        new ViewModelTableRowFactory<JStyleSelectViewModel>()
                .withOnMouseClickedEvent((item, event) -> {
                    if (event.getClickCount() == 2) {
                        viewModel.selectedJStyleProperty().setValue(item);
                        viewModel.storeStylePreferences();
                        this.setResult(viewModel.getSelectedStyle());
                        this.close();
                    }
                })
                .withContextMenu(item -> createContextMenu())
                .install(jStylesTable);

        jStylesTable.getSelectionModel().selectedItemProperty().addListener((_, oldValue, newValue) -> {
            if (newValue == null) {
                viewModel.selectedJStyleProperty().setValue(oldValue);
            } else {
                viewModel.selectedJStyleProperty().setValue(newValue);
            }
        });

        jStylesTable.setItems(viewModel.jStylesProperty());

        addJStyleButton.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

        // JStyle previews
        previewArticle = initializePreviewViewer(TestEntry.getTestEntry());
        jStylePreviewBox.getChildren().add(previewArticle);

        previewBook = initializePreviewViewer(TestEntry.getTestEntryBook());
        jStylePreviewBox.getChildren().add(previewBook);

        EasyBind.subscribe(viewModel.selectedJStyleProperty(), style -> {
            if (viewModel.getSelectedStyle() instanceof JStyle) {
                jStylesTable.getSelectionModel().select(style);
                previewArticle.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Article)));
                previewBook.setLayout(new TextBasedPreviewLayout(style.getJStyle().getReferenceFormat(StandardEntryType.Book)));
            }
        });
    }

    private PreviewViewer initializePreviewViewer(BibEntry entry) {
        PreviewViewer viewer = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);
        viewer.setDatabaseContext(BibDatabaseContext.empty());
        viewer.setEntry(entry);
        return viewer;
    }

    private ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(edit, reload);
        return contextMenu;
    }

    private void updateCurrentStyleLabel() {
        currentStyleNameLabel.setText(viewModel.getSetStyle().getName());
    }

    private void updateCslStylesTable() {
        cslStylesTable.getItems().clear();
        for (CitationStylePreviewLayout layout : viewModel.getAvailableCslLayouts()) {
            cslStylesTable.getItems().add(new CSLStyleSelectViewModel(layout));
        }

        if (viewModel.selectedCslLayoutProperty().get() != null) {
            for (CSLStyleSelectViewModel model : cslStylesTable.getItems()) {
                if (model.getLayout().equals(viewModel.selectedCslLayoutProperty().get())) {
                    cslStylesTable.getSelectionModel().select(model);
                    break;
                }
            }
        }
    }

    @FXML
    private void addCslStyleFile() {
        viewModel.addCslStyleFile();
    }

    @FXML
    private void addJStyleFile() {
        viewModel.addJStyleFile();
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

        OOStyle currentStyle = preferences.getOpenOfficePreferences(Injector.instantiateModelOrService(JournalAbbreviationRepository.class)).getCurrentStyle();
        if (currentStyle instanceof CitationStyle currentCitationStyle) {
            for (int i = 0; i < cslStylesTable.getItems().size(); i++) {
                CSLStyleSelectViewModel item = cslStylesTable.getItems().get(i);
                if (item.getLayout().getFilePath().equals(currentCitationStyle.getFilePath())) {
                    cslStylesTable.scrollTo(i);
                    cslStylesTable.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
}
