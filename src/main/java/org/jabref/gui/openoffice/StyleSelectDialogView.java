package org.jabref.gui.openoffice;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelTableRowFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class StyleSelectDialogView extends BaseDialog<OOBibStyle> {

    @FXML private TableColumn<StyleSelectItemViewModel, String> colName;
    @FXML private TableView<StyleSelectItemViewModel> tvStyles;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colJournals;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colFile;
    @FXML private TableColumn<StyleSelectItemViewModel, Boolean> colDeleteIcon;
    @FXML private Button add;
    @FXML private BorderPane contentPane;
    private final MenuItem edit = new MenuItem(Localization.lang("Edit"));
    private final MenuItem reload = new MenuItem(Localization.lang("Reload"));
    @Inject private PreferencesService preferencesService;

    private StyleSelectDialogViewModel viewModel;
    private final DialogService dialogService;
    private final StyleLoader loader;
    private PreviewPanel preview;

    public StyleSelectDialogView(DialogService dialogService, StyleLoader loader) {
        this.dialogService = dialogService;
        this.loader = loader;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return tvStyles.getSelectionModel().getSelectedItem().getStyle();
            }
            return null;

        });
    }

    @FXML
    private void initialize() {
        viewModel = new StyleSelectDialogViewModel(dialogService, loader, preferencesService);

        preview = new PreviewPanel(null, new BibDatabaseContext(), Globals.getKeyPrefs(), Globals.prefs.getPreviewPreferences(), dialogService, ExternalFileTypes.getInstance());
        preview.setEntry(TestEntry.getTestEntry());
        contentPane.setBottom(preview);

        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colJournals.setCellValueFactory(cellData -> cellData.getValue().journalsProperty());
        colFile.setCellValueFactory(cellData -> cellData.getValue().fileProperty());
        colDeleteIcon.setCellValueFactory(cellData -> cellData.getValue().internalStyleProperty());

        new ValueTableCellFactory<StyleSelectItemViewModel, Boolean>().withGraphic(internalStyle -> {
            if (!internalStyle) {
                return IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode();
            }
            return null;

        }).withOnMouseClickedEvent(item -> {
            return evt -> viewModel.deleteStyle();
        }).install(colDeleteIcon);

        edit.setOnAction(e -> viewModel.editStyle());

        new ViewModelTableRowFactory<StyleSelectItemViewModel>()
                                                                .withOnMouseClickedEvent((item, event) -> {
                                                                    if (event.getClickCount() == 2) {
                                                                        viewModel.viewStyle(item);
                                                                    }
                                                                }).withContextMenu(item -> createContextMenu())
                                                                .install(tvStyles);
        tvStyles.getSelectionModel().selectedItemProperty().addListener((observable, oldvalue, newvalue) -> {
            viewModel.selectedItemProperty().setValue(newvalue);
            //TODO: Fix NPE
            preview.setLayout(newvalue.getStyle().getReferenceFormat("default"));
        });

        tvStyles.setItems(viewModel.stylesProperty());

        add.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());

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
