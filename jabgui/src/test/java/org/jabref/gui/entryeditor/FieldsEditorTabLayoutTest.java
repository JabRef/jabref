package org.jabref.gui.entryeditor;

import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class FieldsEditorTabLayoutTest {

    private static final List<Field> FIELDS = List.of(
            StandardField.KEYWORDS,
            StandardField.EPRINT,
            StandardField.EPRINTTYPE,
            StandardField.URL,
            StandardField.FILE,
            StandardField.GROUPS,
            StandardField.OWNER,
            StandardField.TIMESTAMP);

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

    @Test
    void urlAndOwnerInputsFillTheirRowsAndAlignWithLabels() {
        JavaFxExtension.invokeAndWait(() -> {
            for (double viewportHeight : new double[] {126, 734}) {
                LayoutMeasurements measurements = layout(109, viewportHeight);
                assertEquals(0, measurements.urlTopGap(), 1);
                assertEquals(0, measurements.ownerTopGap(), 1);
                assertEquals(measurements.urlRowHeight(), measurements.urlInputHeight(), 1);
                assertEquals(measurements.ownerRowHeight(), measurements.ownerInputHeight(), 1);
            }
        });
    }

    @Test
    void fileRowKeepsItsContentHeightWhenTheEditorGrows() {
        JavaFxExtension.invokeAndWait(() -> {
            for (double keywordHeight : new double[] {21, 109}) {
                LayoutMeasurements shortEditor = layout(keywordHeight, 126);
                LayoutMeasurements tallEditor = layout(keywordHeight, 734);

                assertEquals(shortEditor.fileRowHeight(), tallEditor.fileRowHeight(), 0.1);
                assertTrue(tallEditor.urlRowHeight() > shortEditor.urlRowHeight());
            }
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
        TextField urlInput = new TextField();
        urlInput.setPrefHeight(0);
        HBox urlEditor = new HBox(urlInput, new Button());
        urlEditor.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(urlInput, Priority.ALWAYS);
        TextField ownerInput = new TextField();
        ownerInput.setPrefHeight(0);
        HBox ownerEditor = new HBox(ownerInput, new Button());
        ownerEditor.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(ownerInput, Priority.ALWAYS);
        TextField fileInput = textField();
        Region fileContent = new Region();
        fileContent.setMinHeight(80);
        fileContent.setPrefHeight(80);

        Node[] editors = {
                keywords,
                textField(),
                textField(),
                urlEditor,
                new VBox(fileInput, fileContent),
                new HBox(new Region()),
                ownerEditor,
                textField()
        };
        FieldNameLabel fileLabel = new FieldNameLabel(StandardField.FILE);
        FieldNameLabel urlLabel = new FieldNameLabel(StandardField.URL);
        FieldNameLabel ownerLabel = new FieldNameLabel(StandardField.OWNER);
        for (int row = 0; row < editors.length; row++) {
            Label label = switch (row) {
                case 3 -> urlLabel;
                case 4 -> fileLabel;
                case 6 -> ownerLabel;
                default -> new Label("Field " + row);
            };
            label.setPrefHeight(Double.POSITIVE_INFINITY);
            grid.addRow(row, label, editors[row]);
        }

        FieldsEditorTab.setRegularRowLayout(grid, FIELDS);

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
        Bounds urlInputBounds = urlInput.localToScene(urlInput.getBoundsInLocal());
        Bounds ownerInputBounds = ownerInput.localToScene(ownerInput.getBoundsInLocal());
        return new LayoutMeasurements(
                grid.getCellBounds(1, 0).getHeight(),
                grid.getCellBounds(1, 4).getHeight(),
                gap,
                fileLabelInputCenterGap,
                urlInputBounds.getMinY() - urlLabel.localToScene(urlLabel.getBoundsInLocal()).getMinY(),
                grid.getCellBounds(1, 3).getHeight(),
                urlInputBounds.getHeight(),
                ownerInputBounds.getMinY() - ownerLabel.localToScene(ownerLabel.getBoundsInLocal()).getMinY(),
                grid.getCellBounds(1, 6).getHeight(),
                ownerInputBounds.getHeight());
    }

    private TextField textField() {
        TextField field = new TextField();
        field.setPrefHeight(Double.POSITIVE_INFINITY);
        return field;
    }

    private record LayoutMeasurements(double keywordsRowHeight, double fileRowHeight, double gapToEprint,
                                      double fileLabelInputCenterGap, double urlTopGap,
                                      double urlRowHeight, double urlInputHeight, double ownerTopGap,
                                      double ownerRowHeight, double ownerInputHeight) {
    }
}
