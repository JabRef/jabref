package org.jabref.gui.externalfiles;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ImportResultsDialogView extends BaseDialog<Void> {

    @FXML private TableView<ImportFilesResultItemViewModel> tvResult;
    @FXML private TableColumn<ImportFilesResultItemViewModel, JabRefIcon> colStatus;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<ImportFilesResultItemViewModel, String> colFile;

    public ImportResultsDialogView(List<ImportFilesResultItemViewModel> results) {
        this.setTitle(Localization.lang("Result"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        tvResult.setItems(FXCollections.observableArrayList(results));
    }

    @FXML
    private void initialize() {
        setupTable();
    }

    private void setupTable() {
        colFile.setCellValueFactory(cellData -> cellData.getValue().file());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
        .withText(item -> item).withTooltip(item -> item)
        .install(colFile);

        colMessage.setCellValueFactory(cellData -> cellData.getValue().message());
        new ValueTableCellFactory<ImportFilesResultItemViewModel, String>()
        .withText(item->item).withTooltip(item->item)
        .install(colMessage);

        colStatus.setCellValueFactory(cellData -> cellData.getValue().getIcon());

        colStatus.setCellFactory(new ValueTableCellFactory<ImportFilesResultItemViewModel, JabRefIcon>().withGraphic(item -> {
            if (item == IconTheme.JabRefIcons.CHECK) {
                item = item.withColor(Color.GREEN);
            }
            if (item == IconTheme.JabRefIcons.WARNING) {
                item = item.withColor(Color.RED);
            }
            return item.getGraphicNode();
        }));

        tvResult.setColumnResizePolicy((param) -> true);
    }
}
