package org.jabref.gui.entryeditor;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class FieldsEditorTabLayoutTest {

    @Test
    void keywordGrowthKeepsFollowingFieldsSeparateAndFileRowStableInShortEditor() {
        JavaFxExtension.invokeAndWait(() -> {
            LayoutMeasurements fewKeywords = layout(21);
            LayoutMeasurements manyKeywords = layout(109);

            assertEquals(8, fewKeywords.gapToEprint(), 0.1);
            assertEquals(8, manyKeywords.gapToEprint(), 0.1);
            assertTrue(manyKeywords.keywordsRowHeight() >= 109);
            assertEquals(fewKeywords.fileRowHeight(), manyKeywords.fileRowHeight(), 0.1);
        });
    }

    private LayoutMeasurements layout(double keywordHeight) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(4));
        grid.setVgap(8);

        Region tags = new Region();
        tags.setMinHeight(keywordHeight);
        tags.setPrefHeight(keywordHeight);
        HBox keywords = new HBox(tags);

        Node[] editors = {
                keywords,
                textField(),
                textField(),
                textField(),
                new VBox(textField()),
                new HBox(new Region()),
                textField(),
                textField()
        };
        for (int row = 0; row < editors.length; row++) {
            Label label = new Label("Field " + row);
            label.setPrefHeight(Double.POSITIVE_INFINITY);
            grid.addRow(row, label, editors[row]);
        }

        FieldsEditorTab.setRegularRowLayout(grid, editors.length);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setManaged(false);
        Pane root = new Pane(scrollPane);
        new Scene(root, 800, 126);
        root.applyCss();
        scrollPane.resize(800, 126);
        root.layout();
        scrollPane.layout();
        grid.layout();

        double gap = editors[1].getBoundsInParent().getMinY() - keywords.getBoundsInParent().getMaxY();
        return new LayoutMeasurements(
                grid.getCellBounds(1, 0).getHeight(),
                grid.getCellBounds(1, 4).getHeight(),
                gap);
    }

    private TextField textField() {
        TextField field = new TextField();
        field.setPrefHeight(Double.POSITIVE_INFINITY);
        return field;
    }

    private record LayoutMeasurements(double keywordsRowHeight, double fileRowHeight, double gapToEprint) {
    }
}
