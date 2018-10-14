package org.jabref.gui.openoffice;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;

import com.airhacks.afterburner.views.ViewLoader;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;

public class ManageCitationsDialogView extends BaseDialog<Void> {

    @FXML private TableView<ManageCitationsItemViewModel> tvCitations;
    @FXML private TableColumn<ManageCitationsItemViewModel, String> colCitation;
    @FXML private TableColumn<ManageCitationsItemViewModel, String> colExtraInfo;

    private final DialogService dialogService;
    private final OOBibBase ooBase;

    private ManageCitationsDialogViewModel viewModel;

    public ManageCitationsDialogView(OOBibBase ooBase, DialogService dialogService) {
        this.ooBase = ooBase;
        this.dialogService = dialogService;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

    }

    @FXML
    private void initialize() throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {

        viewModel = new ManageCitationsDialogViewModel(ooBase, dialogService);

        colCitation.setCellValueFactory(cellData -> cellData.getValue().citationProperty());
        colExtraInfo.setCellValueFactory(cellData -> cellData.getValue().extraInformationProperty());

    }
}
