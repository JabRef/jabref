package org.jabref.gui.commonfxcontrols;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import org.jabref.gui.DialogService;
import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import jakarta.inject.Inject;

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

        private final int ALL_PATTERNS_POSITION = 0;

        private final List<String> citationKeyPatterns;
        private final ContextMenu suggestionsList;
        private int selectionCount = 0;
        private final StringBuilder selectedPatterns = new StringBuilder();

        @Inject private DialogService dialogService;

        public CitationKeyPatternSuggestionTextField(List<String> citationKeyPatterns) {
            Injector.registerExistingAndInject(this);

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
                    createCompoundPatternsSubMenu();
                }
            });

            focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    suggestionsList.hide();
                }
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
            suggestionsList.getItems().add(ALL_PATTERNS_POSITION, createPatternsSubMenu());
            suggestionsList.getItems().add(ALL_PATTERNS_POSITION + 1, createCompoundPatternsSubMenu());
            suggestionsList.getItems().addAll(menuItems);

            if (!menuItems.isEmpty()) {
                menuItems.getFirst().getContent().requestFocus();
            }
        }

        private Menu createPatternsSubMenu() {
            Menu patternsSubMenu = new Menu(Localization.lang("All patterns"));

            Map<CitationKeyPattern.Category, List<CitationKeyPattern>> categorizedPatterns =
                    CitationKeyPattern.getAllPatterns().stream()
                                      .collect(Collectors.groupingBy(CitationKeyPattern::getCategory));

            Map<CitationKeyPattern.Category, String> categoryNames = new LinkedHashMap<>();
            categoryNames.put(CitationKeyPattern.Category.AUTHOR_RELATED, Localization.lang("Author related"));
            categoryNames.put(CitationKeyPattern.Category.EDITOR_RELATED, Localization.lang("Editor related"));
            categoryNames.put(CitationKeyPattern.Category.TITLE_RELATED, Localization.lang("Title related"));
            categoryNames.put(CitationKeyPattern.Category.OTHER_FIELDS, Localization.lang("Other fields"));
            categoryNames.put(CitationKeyPattern.Category.BIBENTRY_FIELDS, Localization.lang("BibEntry fields"));

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

        private Menu createCompoundPatternsSubMenu() {
            Menu compoundPatternsSubMenu = new Menu(Localization.lang("Create compound pattern"));

            Map<CitationKeyPattern.Category, List<CitationKeyPattern>> categorizedPatterns =
                    CitationKeyPattern.getAllPatterns().stream()
                                      .collect(Collectors.groupingBy(CitationKeyPattern::getCategory));

            Map<CitationKeyPattern.Category, String> categoryNames = new LinkedHashMap<>();
            categoryNames.put(CitationKeyPattern.Category.AUTHOR_RELATED, Localization.lang("Author related"));
            categoryNames.put(CitationKeyPattern.Category.EDITOR_RELATED, Localization.lang("Editor related"));
            categoryNames.put(CitationKeyPattern.Category.TITLE_RELATED, Localization.lang("Title related"));
            categoryNames.put(CitationKeyPattern.Category.OTHER_FIELDS, Localization.lang("Other fields"));
            categoryNames.put(CitationKeyPattern.Category.BIBENTRY_FIELDS, Localization.lang("BibEntry fields"));

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

                        if (selectionCount == 0) {
                            selectionCount++;
                            selectedPatterns.append(pattern.stringRepresentation());
                            setText(selectedPatterns.toString());
                            positionCaret(selectedPatterns.length());
                            dialogService.notify(Localization.lang("Select one more item to create a compound pattern."));
                            if (getScene() != null) {
                                suggestionsList.getItems().remove(ALL_PATTERNS_POSITION);
                                double screenX = localToScreen(0, 0).getX();
                                double screenY = localToScreen(0, 0).getY() + getHeight();
                                suggestionsList.show(this, screenX, screenY);
                            }
                        } else if (selectionCount == 1) {
                            selectedPatterns.append("_").append(pattern.stringRepresentation());
                            setText(selectedPatterns.toString());
                            positionCaret(selectedPatterns.length());
                            suggestionsList.hide();
                            selectionCount = 0;
                            selectedPatterns.setLength(0);
                        }
                    });
                    categoryMenu.getItems().add(menuItem);
                }
                compoundPatternsSubMenu.getItems().add(categoryMenu);
            }

            return compoundPatternsSubMenu;
        }
    }
}
