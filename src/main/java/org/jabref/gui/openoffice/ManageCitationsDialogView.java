package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;

public class ManageCitationsDialogView extends BaseDialog<Void> {

    @FXML private TableView<ManageCitationsItemViewModel> citationsTableView;
    @FXML private TableColumn<ManageCitationsItemViewModel, String> citation;
    @FXML private TableColumn<ManageCitationsItemViewModel, String> extraInfo;

    private final DialogService dialogService;
    private final OOBibBase ooBase;

    private ManageCitationsDialogViewModel viewModel;

    public ManageCitationsDialogView(OOBibBase ooBase, DialogService dialogService) {
        this.ooBase = ooBase;
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                viewModel.storeSettings();
            }
            return null;
        });

        setTitle(Localization.lang("Manage citations"));
    }

    @FXML
    private void initialize() throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {

        viewModel = new ManageCitationsDialogViewModel(ooBase, dialogService);

        citation.setCellValueFactory(cellData -> cellData.getValue().citationProperty());
        new ValueTableCellFactory<ManageCitationsItemViewModel, String>().withGraphic(viewModel::getText).install(citation);

        extraInfo.setCellValueFactory(cellData -> cellData.getValue().extraInformationProperty());
        extraInfo.setEditable(true);

        citationsTableView.setEditable(true);

        citationsTableView.itemsProperty().bindBidirectional(viewModel.citationsProperty());

        extraInfo.setOnEditCommit((CellEditEvent<ManageCitationsItemViewModel, String> cell) -> {
            cell.getRowValue().setExtraInfo(cell.getNewValue());
        });
        extraInfo.setCellFactory(TextFieldTableCell.forTableColumn());

    }
}