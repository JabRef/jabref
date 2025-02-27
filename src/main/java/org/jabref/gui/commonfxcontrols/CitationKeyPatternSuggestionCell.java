package org.jabref.gui.commonfxcontrols;

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
        // Maximum number of entries that can be displayed in the popup menu.
        private static final int MAX_ENTRIES = 7;

        // Maximum number of entries that can be displayed in the popup menu.
        private static final int MAX_COMPOUND_PATTERNS = 10;

        private final List<String> citationKeyPatterns;
        private final ContextMenu suggestionsList;
        private String enteredText;

        public CitationKeyPatternSuggestionTextField(List<String> citationKeyPatterns) {
            this.citationKeyPatterns = new ArrayList<>(citationKeyPatterns);
            this.suggestionsList = new ContextMenu();

            setListener();
        }

        private void setListener() {
            textProperty().addListener((observable, oldValue, newValue) -> {
                this.enteredText = getText();
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
                    showCompoundPatternsMenu(enteredText);
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
            suggestionsList.getItems().add(showCompoundPatternsMenu(enteredText));
            suggestionsList.getItems().addAll(menuItems);

            if (!menuItems.isEmpty()) {
                menuItems.getFirst().getContent().requestFocus();
            }
        }

        public Menu showCompoundPatternsMenu(String enteredText) {
            return createCompoundPatternsSubMenu(generateCompoundPatterns(enteredText));
        }

        public List<String> generateCompoundPatterns(String enteredText) {
            List<String> patterns = List.of("auth", "edtr", "year", "title", "date");
            List<String> filteredPatterns = new ArrayList<>();

            for (String pattern : patterns) {
                if (pattern.contains(enteredText)) {
                    filteredPatterns.add(pattern);
                }
            }

            List<String> compoundPatterns = new ArrayList<>();

            for (String pattern : filteredPatterns) {
                for (String otherPattern : patterns) {
                    if (!pattern.equals(otherPattern)) {
                        compoundPatterns.add("[" + pattern + "]_" + "[" + otherPattern + "]");
                    }
                }
            }

            return compoundPatterns;
        }

        private Menu createCompoundPatternsSubMenu(List<String> compoundPatterns) {
            Menu compoundPatternsSubMenu = new Menu(Localization.lang("Compound patterns"));

            int count = Math.min(compoundPatterns.size(), MAX_COMPOUND_PATTERNS);
            for (int i = 0; i < count; i++) {
                final String compoundPattern = compoundPatterns.get(i);
                MenuItem menuItem = new MenuItem(compoundPattern.replace("_", "__"));
                menuItem.setOnAction(event -> {
                    setText(compoundPattern);
                    positionCaret(compoundPattern.length());
                    suggestionsList.hide();
                });
                compoundPatternsSubMenu.getItems().add(menuItem);
            }

            return compoundPatternsSubMenu;
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
                    CitationKeyPattern.Category.BIBENTRY_FIELDS, Localization.lang("BibEntry fields")
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
