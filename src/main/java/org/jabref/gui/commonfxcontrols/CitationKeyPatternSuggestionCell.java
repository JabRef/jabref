package org.jabref.gui.commonfxcontrols;

import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Popup;

public class CitationKeyPatternSuggestionCell extends TextFieldTableCell<CitationKeyPatternsPanelItemModel, String> {
    private final TextField searchField = new TextField();
    private final ListView<String> suggestionList = new ListView<>();
    private final Popup popup = new Popup();

    private final List<String> fullData;

    public CitationKeyPatternSuggestionCell(List<String> fullData) {
        this.fullData = fullData;
        popup.getContent().add(suggestionList);
        popup.setAutoHide(true);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<String> filtered = fullData.stream()
                                            .filter(item -> item.toLowerCase().contains(newVal.toLowerCase()))
                                            .collect(Collectors.toList());

            suggestionList.setItems(FXCollections.observableArrayList(filtered));
            if (!filtered.isEmpty()) {
                popup.show(searchField, searchField.localToScreen(0, searchField.getHeight()).getX(),
                        searchField.localToScreen(0, searchField.getHeight() * 2).getY());
            } else {
                popup.hide();
            }
        });

        suggestionList.setOnMouseClicked(event -> {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                searchField.setText(selected);
                popup.hide();
            }
        });

        searchField.setOnAction(event -> commitEdit(searchField.getText()));
    }

    @Override
    public void startEdit() {
        super.startEdit();
        searchField.setText(getItem());
        setGraphic(searchField);
        searchField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        popup.hide();
    }

    @Override
    public void commitEdit(String newValue) {
        super.commitEdit(newValue);
        getTableView().getItems().get(getIndex()).setPattern(newValue);
        setGraphic(null);
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
        } else {
            setText(item);
        }
    }
}
