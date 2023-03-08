package org.jabref.gui.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchHistoryItem;

import com.airhacks.afterburner.views.ViewLoader;
import org.controlsfx.control.textfield.CustomTextField;

public class HistoryDialogView extends BaseDialog<Void> {
    @FXML private TableView<SearchHistoryItem> searchHistoryTable;
    @FXML private TableColumn<SearchHistoryItem, String> searchHistoryStringColumn;
    @FXML private TableColumn<SearchHistoryItem, String> searchHistoryDateColumn;
    @FXML private TableColumn<SearchHistoryItem, String> searchHistoryRemoveColumn;
    private final StateManager stateManager;
    private HistoryDialogViewModel viewModel;
    private final CustomTextField searchField;

    public HistoryDialogView(StateManager stateManager, CustomTextField searchField) {
        this.setTitle(Localization.lang("Search History"));
        this.stateManager = stateManager;
        this.searchField = searchField;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public HistoryDialogViewModel getViewModel() {
        return viewModel;
    }

    @FXML
    private void initialize() {
        viewModel = new HistoryDialogViewModel(stateManager);

        searchHistoryTable.setRowFactory(tableView -> {
            TableRow<SearchHistoryItem> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (!row.isEmpty() && evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                    String searchStringClicked = row.getItem().searchStringProperty().get();
                    searchField.setText(searchStringClicked);
                    this.close();
                }
            });
            return row;
        });

        searchHistoryStringColumn.setEditable(false);
        searchHistoryStringColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        searchHistoryStringColumn.setCellValueFactory(param -> param.getValue().searchStringProperty());

        searchHistoryDateColumn.setEditable(false);
        searchHistoryDateColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        searchHistoryDateColumn.setCellValueFactory(param -> param.getValue().lastSearchedProperty());

        searchHistoryRemoveColumn.setEditable(false);
        searchHistoryRemoveColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().searchStringProperty().get()));

        new ValueTableCellFactory<SearchHistoryItem, String>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove"))
                .withOnMouseClickedEvent(item -> evt -> viewModel.remove(item))
                .install(searchHistoryRemoveColumn);

        searchHistoryTable.setItems(viewModel.getHistory());

        this.setResizable(false);
    }
}
