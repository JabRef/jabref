package org.jabref.http.server.cayw.gui;

import java.util.Arrays;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.logic.os.OS;
import org.jabref.model.strings.StringUtil;

public class SearchResultContainer extends ListView<CAYWEntry> {

    private final static int MAX_LINES = 3;
    private final static int ESTIMATED_CHARS_PER_LINE = 80;
    private final static int TOOLTIP_WIDTH = 400;
    private final static double PREF_WIDTH = 300;

    private ObservableList<CAYWEntry> selectedEntries = javafx.collections.FXCollections.observableArrayList();

    public SearchResultContainer(ObservableList<CAYWEntry> entries, ObservableList<CAYWEntry> selectedEntries) {
        super(entries);
        this.selectedEntries = selectedEntries;
        setup();
    }

    private void setup() {
        this.setCellFactory(listView -> {
            SearchResultCell searchResultCell = new SearchResultCell();
            searchResultCell.setOnMouseClicked(event -> {
                if (searchResultCell.getItem() == null || selectedEntries.contains(searchResultCell.getItem())) {
                    return;
                }
                selectedEntries.add(searchResultCell.getItem());
            });
            return searchResultCell;
        });

        this.setFocusTraversable(false);

        this.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            if (getParent() != null) {
                return getParent().getLayoutBounds().getWidth();
            }
            return PREF_WIDTH;
        }, parentProperty()));
    }

    private static class SearchResultCell extends ListCell<CAYWEntry> {
        private final VBox content;
        private final Text labelText;
        private final Text descriptionText;
        private final Tooltip tooltip;

        public SearchResultCell() {
            this.content = new VBox(5);
            this.labelText = new Text();
            this.descriptionText = new Text();
            this.tooltip = new Tooltip();

            labelText.getStyleClass().add("search-result-label");

            descriptionText.getStyleClass().add("search-result-description");

            content.getChildren().addAll(labelText, descriptionText);

            descriptionText.wrappingWidthProperty().bind(
                    widthProperty().subtract(20)
            );
        }

        @Override
        protected void updateItem(CAYWEntry item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setTooltip(null);
            } else {
                labelText.setText(item.label());

                String fullDescription = item.description();
                String truncatedDescription = truncateToThreeLines(fullDescription);

                descriptionText.setText(truncatedDescription);

                if (!fullDescription.equals(truncatedDescription)) {
                    tooltip.setText(fullDescription);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(TOOLTIP_WIDTH);
                    setTooltip(tooltip);
                } else {
                    setTooltip(null);
                }

                setGraphic(content);
            }
        }

        private String truncateToThreeLines(String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }

            String[] lines = text.split(OS.NEWLINE, MAX_LINES + 1);

            if (lines.length <= MAX_LINES) {
                return StringUtil.limitStringLength(text, ESTIMATED_CHARS_PER_LINE * MAX_LINES);
            } else {
                return String.join(OS.NEWLINE, Arrays.copyOf(lines, MAX_LINES)) + "...";
            }
        }
    }
}
