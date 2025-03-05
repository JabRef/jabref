package org.jabref.gui.linkedfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.linkedfile.LinkedFileNamePattern;

public class LinkedFileNamePatternsSuggestionCell extends TextFieldTableCell<LinkedFileNamePatternsItemModel, String> {
    private final LinkedFileNamePatternSuggestionTextField searchField;

    public LinkedFileNamePatternsSuggestionCell(List<String> linkedFileNamePatterns) {
        this.searchField = new LinkedFileNamePatternSuggestionTextField(linkedFileNamePatterns);
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

    static class LinkedFileNamePatternSuggestionTextField extends TextField {
        // Maximum number of entries that can be displayed in the popup menu.
        private static final int MAX_ENTRIES = 7;

        private final List<String> linkedFileNamePatterns;
        private final ContextMenu suggestionsList;

        public LinkedFileNamePatternSuggestionTextField(List<String> linkedFileNamePatterns) {
            this.linkedFileNamePatterns = new ArrayList<>(linkedFileNamePatterns);
            this.suggestionsList = new ContextMenu();

            setListener();
        }

        private void setListener() {
            textProperty().addListener((observable, oldValue, newValue) -> {
                String enteredText = getText();
                if (enteredText == null || enteredText.isEmpty()) {
                    suggestionsList.hide();
                } else {
                    List<String> filteredEntries = linkedFileNamePatterns.stream()
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
            int count = Math.min(searchResult.size(), MAX_ENTRIES);

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
            suggestionsList.getItems().add(createPatternsSubMenu());
            suggestionsList.getItems().addAll(menuItems);

            if (!menuItems.isEmpty()) {
                menuItems.getFirst().getContent().requestFocus();
            }
        }

        private Menu createPatternsSubMenu() {
            Menu patternsSubMenu = new Menu(Localization.lang("All patterns"));

            Map<LinkedFileNamePattern.Category, List<LinkedFileNamePattern>> categorizedPatterns =
                    LinkedFileNamePattern.getAllPatterns().stream()
                                      .collect(Collectors.groupingBy(LinkedFileNamePattern::getCategory));

            Map<LinkedFileNamePattern.Category, String> categoryNames = Map.of(
                    LinkedFileNamePattern.Category.AUTHOR_RELATED, Localization.lang("Author related"),
                    LinkedFileNamePattern.Category.EDITOR_RELATED, Localization.lang("Editor related"),
                    LinkedFileNamePattern.Category.TITLE_RELATED, Localization.lang("Title related"),
                    LinkedFileNamePattern.Category.OTHER_FIELDS, Localization.lang("Other fields"),
                    LinkedFileNamePattern.Category.BIBENTRY_FIELDS, Localization.lang("BibEntry fields")
            );

            for (Map.Entry<LinkedFileNamePattern.Category, String> entry : categoryNames.entrySet()) {
                LinkedFileNamePattern.Category category = entry.getKey();
                String categoryName = entry.getValue();

                Menu categoryMenu = new Menu(categoryName);
                List<LinkedFileNamePattern> patterns = categorizedPatterns.getOrDefault(category, List.of());

                for (LinkedFileNamePattern pattern : patterns) {
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
