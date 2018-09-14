package org.jabref.gui.openoffice;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class StyleSelectDialogView extends BaseDialog<OOBibStyle> {

    @FXML private TableColumn<StyleSelectItemViewModel, String> colName;
    @FXML private TableView<StyleSelectItemViewModel> tvStyles;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colJournals;
    @FXML private TableColumn<StyleSelectItemViewModel, String> colFile;
    @FXML private TableColumn<StyleSelectItemViewModel, Node> colDeleteIcon;
    @FXML private Button add;
    @Inject private PreferencesService preferencesService;

    private StyleSelectDialogViewModel viewModel;
    private final DialogService dialogService;
    private final StyleLoader loader;

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

        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colJournals.setCellValueFactory(cellData -> cellData.getValue().journalsProperty());
        colFile.setCellValueFactory(cellData -> cellData.getValue().fileProperty());
        colDeleteIcon.setCellValueFactory(cellData -> cellData.getValue().iconProperty());

        tvStyles.setItems(viewModel.getStyles());
    }

    @FXML
    private void addStyleFile() {
        viewModel.addStyleFile();
    }

}
