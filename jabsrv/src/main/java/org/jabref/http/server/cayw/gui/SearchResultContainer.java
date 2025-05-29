package org.jabref.http.server.cayw.gui;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class SearchResultContainer<T> extends ListView<CAYWEntry<T>> {

    public SearchResultContainer(ObservableList<CAYWEntry<T>> entries, ObservableList<CAYWEntry<T>> selectedEntries) {
        super(entries);

        this.setCellFactory(listView -> {
            SearchResultCell<T> searchResultCell = new SearchResultCell<T>();
            searchResultCell.setOnMouseClicked(event -> {
                if (selectedEntries.contains(searchResultCell.getItem())) {
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
            return 300.0;
        }, parentProperty()));
    }

    private static class SearchResultCell<T> extends ListCell<CAYWEntry<T>> {
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
        protected void updateItem(CAYWEntry<T> item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setTooltip(null);
            } else {
                labelText.setText(item.getLabel());

                String fullDescription = item.getDescription();
                String truncatedDescription = truncateToThreeLines(fullDescription);

                descriptionText.setText(truncatedDescription);

                if (!fullDescription.equals(truncatedDescription)) {
                    tooltip.setText(fullDescription);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(400);
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

            String[] lines = text.split("\n", 4);

            if (lines.length <= 3) {
                return estimateAndTruncate(text, 3);
            } else {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    result.append(lines[i]);
                    if (i < 2) {
                        result.append("\n");
                    }
                }
                result.append("...");
                return result.toString();
            }
        }

        private String estimateAndTruncate(String text, int maxLines) {
            int avgCharsPerLine = 80;
            int maxChars = avgCharsPerLine * maxLines;

            if (text.length() <= maxChars) {
                return text;
            }

            int cutoff = Math.min(text.length(), maxChars);

            while (cutoff > 0 && !Character.isWhitespace(text.charAt(cutoff))) {
                cutoff--;
            }

            if (cutoff == 0) {
                cutoff = maxChars;
            }

            return text.substring(0, cutoff).trim() + "...";
        }
    }
}
