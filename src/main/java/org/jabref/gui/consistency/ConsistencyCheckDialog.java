package org.jabref.gui.consistency;

import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.ConsistencyMessage;

import com.airhacks.afterburner.views.ViewLoader;

public class ConsistencyCheckDialog extends BaseDialog<Void> {

    @FXML private TableView<ConsistencyMessage> tableView;
    /* @FXML private TableColumn<ConsistencyMessage, String> entryTypeColumn;
    @FXML private TableColumn<ConsistencyMessage, String> keyColumn;*/

    private ConsistencyCheckDialogViewModel viewModel;

    public ConsistencyCheckDialog() {
        this.setTitle(Localization.lang("Check consistency"));
        this.initModality(Modality.NONE);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public ConsistencyCheckDialogViewModel getViewModel() {
        return viewModel;
    }

    /* @FXML
    private void initialize() {
        viewModel = new ConsistencyCheckDialogViewModel();

        ObservableList<ConsistencyMessage> data = FXCollections.observableArrayList(
                new ConsistencyMessage("Article", "Key1"),
                new ConsistencyMessage("Book", "Key2"),
                new ConsistencyMessage("Conference Paper", "Key3"),
                new ConsistencyMessage("Thesis", "Key4")
        );

        entryTypeColumn.setCellValueFactory(new PropertyValueFactory<>("entryType"));
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));

        tableView.setItems(data);
    }*/

    @FXML
    public void initialize() {
        List<String> columns = Arrays.asList("Entrytype", "Citationkey", "Address", "Booktitle", "Crossref", "DOI", "Pages", "Ranking", "Series", "URL", "Urldate", "Volume", "Year");

        for (String column : columns) {
            TableColumn<ConsistencyMessage, String> tableColumn = new TableColumn<>(column);
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(column.replace(" ", "").toLowerCase())); // Assuming properties in the data class match the column names
            // tableColumn.setCellValueFactory(new PropertyValueFactory<>("entryType")); // Assuming properties in the data class match the column names
            tableView.getColumns().add(tableColumn);
        }

        tableView.getItems().addAll(getSampleData());
    }

    private ObservableList<ConsistencyMessage> getSampleData() {
        return FXCollections.observableArrayList(
                new ConsistencyMessage("Article", "Beck1988", "-", "-", "-", "?", "o", "-", "-", "-", "-", "-", "x"),
                new ConsistencyMessage("Article", "Duranton2016", "-", "-", "-", "-", "o", "?", "-", "?", "?", "o", "x")
        );
    }
}

