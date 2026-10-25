package org.jabref.gui.entryeditor;

import javafx.geometry.Bounds;
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

import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(JavaFxExtension.class)
class FieldsEditorTabLayoutTest {

    @Test
    void keywordGrowthKeepsFollowingFieldsSeparateAndFileRowStableInShortEditor() {
        JavaFxExtension.invokeAndWait(() -> {
            LayoutMeasurements fewKeywords = layout(21, 126);
            LayoutMeasurements manyKeywords = layout(109, 126);

            assertEquals(8, fewKeywords.gapToEprint(), 0.1);
            assertEquals(8, manyKeywords.gapToEprint(), 0.1);
            assertTrue(manyKeywords.keywordsRowHeight() >= 109);
            assertEquals(fewKeywords.fileRowHeight(), manyKeywords.fileRowHeight(), 0.1);
        });
    }

    @Test
    void fileLabelAlignsWithInputInShortAndTallEditors() {
        JavaFxExtension.invokeAndWait(() -> {
            assertEquals(0, layout(21, 126).fileLabelInputCenterGap(), 5);
            assertEquals(0, layout(109, 126).fileLabelInputCenterGap(), 5);
            assertEquals(0, layout(21, 734).fileLabelInputCenterGap(), 5);
            assertEquals(0, layout(109, 734).fileLabelInputCenterGap(), 5);
        });
    }

    private LayoutMeasurements layout(double keywordHeight, double viewportHeight) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(4));
        grid.setVgap(8);

        Region tags = new Region();
        tags.setMinHeight(keywordHeight);
        tags.setPrefHeight(keywordHeight);
        HBox keywords = new HBox(tags);
        TextField fileInput = textField();
        Region fileContent = new Region();
        fileContent.setMinHeight(80);
        fileContent.setPrefHeight(80);

        Node[] editors = {
                keywords,
                textField(),
                textField(),
                textField(),
                new VBox(fileInput, fileContent),
                new HBox(new Region()),
                textField(),
                textField()
        };
        FieldNameLabel fileLabel = new FieldNameLabel(StandardField.FILE);
        for (int row = 0; row < editors.length; row++) {
            Label label = row == 4 ? fileLabel : new Label("Field " + row);
            label.setPrefHeight(Double.POSITIVE_INFINITY);
            grid.addRow(row, label, editors[row]);
        }

        FieldsEditorTab.setRegularRowLayout(grid, editors.length);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setManaged(false);
        Pane root = new Pane(scrollPane);
        new Scene(root, 800, viewportHeight);
        root.applyCss();
        scrollPane.resize(800, viewportHeight);
        root.layout();
        scrollPane.layout();
        grid.layout();

        double gap = editors[1].getBoundsInParent().getMinY() - keywords.getBoundsInParent().getMaxY();
        Bounds labelBounds = fileLabel.localToScene(fileLabel.getBoundsInLocal());
        Bounds inputBounds = fileInput.localToScene(fileInput.getBoundsInLocal());
        double fileLabelInputCenterGap = (labelBounds.getMinY() + labelBounds.getMaxY()
                - inputBounds.getMinY() - inputBounds.getMaxY()) / 2;
        return new LayoutMeasurements(
                grid.getCellBounds(1, 0).getHeight(),
                grid.getCellBounds(1, 4).getHeight(),
                gap,
                fileLabelInputCenterGap);
    }

    private TextField textField() {
        TextField field = new TextField();
        field.setPrefHeight(Double.POSITIVE_INFINITY);
        return field;
    }

    private record LayoutMeasurements(double keywordsRowHeight, double fileRowHeight, double gapToEprint,
                                      double fileLabelInputCenterGap) {
    }
}
