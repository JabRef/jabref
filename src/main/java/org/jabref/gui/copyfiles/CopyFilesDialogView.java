package org.jabref.gui.copyfiles;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

public class CopyFilesDialogView extends BaseDialog<Void> {

    @FXML private TableView<CopyFilesResultItemViewModel> tvResult;
    @FXML private TableColumn<CopyFilesResultItemViewModel, MaterialDesignIcon> colStatus;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colMessage;
    @FXML private TableColumn<CopyFilesResultItemViewModel, String> colFile;
    private final CopyFilesDialogViewModel viewModel;

    public CopyFilesDialogView(BibDatabaseContext bibDatabaseContext, CopyFilesResultListDependency results) {
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
        colStatus.setCellFactory(new ValueTableCellFactory<CopyFilesResultItemViewModel, MaterialDesignIcon>().withGraphic(item -> {

            Text icon = MaterialDesignIconFactory.get().createIcon(item);
            if (item == MaterialDesignIcon.CHECK) {
                icon.setFill(Color.GREEN);
            }
            if (item == MaterialDesignIcon.ALERT) {
                icon.setFill(Color.RED);
            }
            return icon;
        }));

        tvResult.setItems(viewModel.copyFilesResultListProperty());
        tvResult.setColumnResizePolicy((param) -> true);
    }
}
