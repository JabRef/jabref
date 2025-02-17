package org.jabref.gui.commonfxcontrols;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;

public class CitationKeyPatternSuggestionCell extends TextFieldTableCell<CitationKeyPatternsPanelItemModel, String> {
    private final CitationKeyPatternSuggestoinTextField searchField;

    public CitationKeyPatternSuggestionCell(List<String> citationKeyPatterns) {
        this.searchField = new CitationKeyPatternSuggestoinTextField(citationKeyPatterns);
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

    static class CitationKeyPatternSuggestoinTextField extends TextField {
        private final List<String> citationKeyPatterns;
        private final ContextMenu suggestionsList;

        public CitationKeyPatternSuggestoinTextField(List<String> citationKeyPatterns) {
            this.citationKeyPatterns = new ArrayList<>(citationKeyPatterns);
            this.suggestionsList = new ContextMenu();

            setListener();
        }

        private void setListener() {
            textProperty().addListener((observable, oldValue, newValue) -> {
                String enteredText = getText();
                if (enteredText == null || enteredText.isEmpty()) {
                    suggestionsList.hide();
                } else {
                    List<String> filteredEntries = citationKeyPatterns.stream()
                                                                      .filter(e -> e.toLowerCase().contains(enteredText.toLowerCase()))
                                                                      .collect(Collectors.toList());
                    if (!filteredEntries.isEmpty()) {
                        populatePopup(filteredEntries);
                        if (!suggestionsList.isShowing() && getScene() != null) {
                            double screenX = localToScreen(0, 0).getX();
                            double screenY = localToScreen(0, 0).getY() + getHeight();
                            suggestionsList.show(this, screenX, screenY);
                        }
                    } else {
                        suggestionsList.hide();
                    }
                }
            });

            focusedProperty().addListener((observable, oldValue, newValue) -> {
                suggestionsList.hide();
            });
        }

        private void populatePopup(List<String> searchResult) {
            List<CustomMenuItem> menuItems = new ArrayList<>();
            int maxEntries = 10;
            int count = Math.min(searchResult.size(), maxEntries);
            for (int i = 0; i < count; i++) {
                final String result = searchResult.get(i);
                Label entryLabel = new Label(result);
                entryLabel.setPrefWidth(getWidth());

                CustomMenuItem item = new CustomMenuItem(entryLabel, true);
                item.setHideOnClick(false);

                menuItems.add(item);

                item.setOnAction(actionEvent -> {
                    setText(result);
                    positionCaret(result.length());
                    suggestionsList.hide();
                });
            }

            suggestionsList.getItems().clear();
            suggestionsList.getItems().addAll(menuItems);

            if (!menuItems.isEmpty()) {
                menuItems.getFirst().getContent().requestFocus();
            }
        }
    }
}
