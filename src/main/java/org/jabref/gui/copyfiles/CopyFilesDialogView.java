package org.jabref.gui.copyfiles;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class CopyFilesDialogView extends BaseDialog<Void> {

    @FXML private TableView<CopyFilesResultItemViewModel> tvResult;
    @FXML private TableColumn<CopyFilesResultItemViewModel, JabRefIcon> colStatus;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colFile;
    private final CopyFilesDialogViewModel viewModel;

    public CopyFilesDialogView(CopyFilesResultListDependency results) {
        this.setTitle(Localization.lang("Result"));

        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

        viewModel = new CopyFilesDialogViewModel(results);

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());
    }

    @FXML
    private void initialize() {
        setupTable();
    }

    private void setupTable() {
        colFile.setCellValueFactory(cellData -> cellData.getValue().getFile());
        colMessage.setCellValueFactory(cellData -> cellData.getValue().getMessage());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().getIcon());

        colFile.setCellFactory(new ValueTableCellFactory<CopyFilesResultItemViewModel, String>().withText(item -> item).withTooltip(item -> item));
        colStatus.setCellFactory(new ValueTableCellFactory<CopyFilesResultItemViewModel, JabRefIcon>().withGraphic(item -> {
            if (item == IconTheme.JabRefIcons.CHECK) {
                item = item.withColor(Color.GREEN);
            }
            if (item == IconTheme.JabRefIcons.WARNING) {
                item = item.withColor(Color.RED);
            }
            return item.getGraphicNode();
        }));

        tvResult.setItems(viewModel.copyFilesResultListProperty());
        tvResult.setColumnResizePolicy((param) -> true);
    }
}
