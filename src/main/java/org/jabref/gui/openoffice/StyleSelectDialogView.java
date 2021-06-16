package org.jabref.gui.openoffice;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.StyleLoader;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class StyleSelectDialogView extends BaseDialog<OOBibStyle> {

    private final MenuItem edit = new MenuItem(Localization.lang("Edit"));
    private final MenuItem reload = new MenuItem(Localization.lang("Reload"));
    private final StyleLoader loader;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colName;
    @FXML private TableView<StyleSelectItemViewModel> tvStyles;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colJournals;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colFile;
    @FXML private TableColumn<StyleSelectItemViewModel, Boolean> colDeleteIcon;
    @FXML private Button add;
    @FXML private VBox vbox;
    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
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
                return tvStyles.getSelectionModel().getSelectedItem().getStyle();
            }
            return null;
        });
        setTitle(Localization.lang("Style selection"));
    }

    @FXML
    private void initialize() {
        viewModel = new StyleSelectDialogViewModel(dialogService, loader, preferencesService);

        previewArticle = new PreviewViewer(new BibDatabaseContext(), dialogService, Globals.stateManager);
        previewArticle.setEntry(TestEntry.getTestEntry());
        vbox.getChildren().add(previewArticle);

        previewBook = new PreviewViewer(new BibDatabaseContext(), dialogService, Globals.stateManager);
        previewBook.setEntry(TestEntry.getTestEntryBook());
        vbox.getChildren().add(previewBook);

        previewArticle.setTheme(preferencesService.getTheme());
        previewBook.setTheme(preferencesService.getTheme());

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
            tvStyles.getSelectionModel().select(style);
            previewArticle.setLayout(new TextBasedPreviewLayout(style.getStyle().getReferenceFormat(StandardEntryType.Article)));
            previewBook.setLayout(new TextBasedPreviewLayout(style.getStyle().getReferenceFormat(StandardEntryType.Book)));
        });
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
}
