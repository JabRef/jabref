package org.jabref.gui.commonfxcontrols;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Window;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.l10n.Localization;

public class CitationKeyPatternSuggestionCell extends TextFieldTableCell<CitationKeyPatternsPanelItemModel, String> {
    private final CitationKeyPatternSuggestionTextField searchField;

    public CitationKeyPatternSuggestionCell(List<String> citationKeyPatterns) {
        this.searchField = new CitationKeyPatternSuggestionTextField(citationKeyPatterns);
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

    static class CitationKeyPatternSuggestionTextField extends TextField {
        private final List<String> citationKeyPatterns;
        private final ContextMenu suggestionsList;
        private int heightOfMenuItem;

        public CitationKeyPatternSuggestionTextField(List<String> citationKeyPatterns) {
            this.citationKeyPatterns = new ArrayList<>(citationKeyPatterns);
            this.suggestionsList = new ContextMenu();
            // Initial reasonable estimate before the menu items are populated. We overwrite this dynamically
            this.heightOfMenuItem = 30;

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

            double space = getAvailableSpaceBelow(this);
            int maxItems = (int) (space / heightOfMenuItem) - 1;

            int count = Math.min(searchResult.size(), maxItems);

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

            if (!menuItems.isEmpty()) {
                Platform.runLater(() -> heightOfMenuItem = (int) menuItems.getFirst().getContent().getBoundsInLocal().getHeight());
            }

            suggestionsList.getItems().clear();
            suggestionsList.getItems().add(createPatternsSubMenu());
            suggestionsList.getItems().addAll(menuItems);

            if (!menuItems.isEmpty()) {
                menuItems.getFirst().getContent().requestFocus();
            }
        }

        public static double getAvailableSpaceBelow(TextField textField) {
            if (textField.getScene() == null || textField.getScene().getWindow() == null) {
                return 0;
            }

            Window window = textField.getScene().getWindow();
            if (window == null) {
                return 0;
            }

            Bounds bounds = textField.localToScreen(textField.getBoundsInLocal());
            double screenHeight = window.getHeight();
            double textFieldBottom = bounds.getMinY() + textField.getHeight();

            return screenHeight - (textFieldBottom - window.getY());
        }

        private Menu createPatternsSubMenu() {
            Menu patternsSubMenu = new Menu(Localization.lang("All patterns"));

            Map<CitationKeyPattern.Category, List<CitationKeyPattern>> categorizedPatterns =
                    CitationKeyPattern.getAllPatterns().stream()
                                      .collect(Collectors.groupingBy(CitationKeyPattern::getCategory));

            Map<CitationKeyPattern.Category, String> categoryNames = Map.of(
                    CitationKeyPattern.Category.AUTHOR_RELATED, Localization.lang("Author related"),
                    CitationKeyPattern.Category.EDITOR_RELATED, Localization.lang("Editor related"),
                    CitationKeyPattern.Category.TITLE_RELATED, Localization.lang("Title related"),
                    CitationKeyPattern.Category.OTHER_FIELDS, Localization.lang("Other fields"),
                    CitationKeyPattern.Category.BIBENTRY_FIELDS, Localization.lang("Entry fields")
            );

            for (Map.Entry<CitationKeyPattern.Category, String> entry : categoryNames.entrySet()) {
                CitationKeyPattern.Category category = entry.getKey();
                String categoryName = entry.getValue();

                Menu categoryMenu = new Menu(categoryName);
                List<CitationKeyPattern> patterns = categorizedPatterns.getOrDefault(category, List.of());

                for (CitationKeyPattern pattern : patterns) {
                    MenuItem menuItem = new MenuItem(pattern.stringRepresentation());
                    menuItem.setOnAction(event -> {
                        setText(pattern.stringRepresentation());
                        positionCaret(pattern.stringRepresentation().length());
                        suggestionsList.hide();
                    });
                    categoryMenu.getItems().add(menuItem);
                }
                patternsSubMenu.getItems().add(categoryMenu);
            }
            return patternsSubMenu;
        }
    }
}
