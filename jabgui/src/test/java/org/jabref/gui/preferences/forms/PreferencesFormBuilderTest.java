package org.jabref.gui.preferences.forms;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.dlsc.unitfx.IntegerInputField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class PreferencesFormBuilderTest {

    /// The builder only needs its services for help buttons, which these forms do not use.
    private PreferencesFormBuilder form() {
        return new PreferencesFormBuilder(null, null);
    }

    private static List<Node> childrenOf(Node node) {
        return ((javafx.scene.Parent) node).getChildrenUnmodifiable();
    }

    @Test
    void stackedFieldPlacesCaptionAboveControl() {
        TextField field = new TextField();
        VBox root = form().stackedField("Caption", field).build();

        Node cell = root.getChildren().getFirst();
        assertEquals(2, childrenOf(cell).size());
        assertSame(field, childrenOf(childrenOf(cell).get(1)).getFirst());
    }

    @Test
    void columnsOfGroupsKeepEveryCell() {
        VBox root = form()
                .columns(columns -> columns
                        .group(left -> left
                                .stackedField("Left one", new TextField())
                                .stackedField("Left two", new TextField()))
                        .group(right -> right
                                .stackedField("Right one", new TextField())))
                .build();

        Node columns = root.getChildren().getFirst();
        assertEquals(HBox.class, columns.getClass());
        assertEquals(2, childrenOf(columns).size(), "both column groups are present");
        assertEquals(2, childrenOf(childrenOf(columns).getFirst()).size(), "left column keeps both cells");
        assertEquals(1, childrenOf(childrenOf(columns).get(1)).size(), "right column keeps its cell");
    }

    /// A stacked field must end up with a real size: the AI tab's expert settings are built this
    /// way, and a zero-sized cell would leave that block looking empty.
    @Test
    void stackedFieldsInColumnsAreLaidOut() throws Exception {
        CountDownLatch laidOut = new CountDownLatch(1);
        IntegerInputField integerField = new IntegerInputField();
        TextField textField = new TextField();

        Platform.runLater(() -> {
            VBox root = form()
                    .group(expert -> expert
                            .columns(columns -> columns
                                    .group(left -> left.stackedField("Context window size", integerField))
                                    .group(right -> right.stackedField("Temperature", textField))))
                    .build();
            new Scene(root, 800, 600);
            root.applyCss();
            root.layout();
            laidOut.countDown();
        });

        assertTrue(laidOut.await(10, TimeUnit.SECONDS), "layout pass ran");
        assertTrue(integerField.getWidth() > 0 && integerField.getHeight() > 0,
                "the integer field is laid out, but was " + integerField.getWidth() + "x" + integerField.getHeight());
        assertTrue(textField.getWidth() > 0 && textField.getHeight() > 0,
                "the text field is laid out, but was " + textField.getWidth() + "x" + textField.getHeight());
    }

    /// Columns are of equal width whatever they contain: a column claiming width in proportion to
    /// its longest caption is what the hand-built grids used percentage constraints to avoid.
    @Test
    void columnsAreEquallyWideRegardlessOfContent() throws Exception {
        CountDownLatch laidOut = new CountDownLatch(1);
        VBox[] columns = new VBox[2];

        Platform.runLater(() -> {
            VBox root = form()
                    .columns(row -> row
                            .group(left -> left.stackedField("A very much longer caption than the other", new TextField()))
                            .group(right -> right.stackedField("Short", new TextField())))
                    .build();
            new Scene(root, 900, 700);
            root.applyCss();
            root.layout();

            HBox columnsRow = (HBox) root.getChildren().getFirst();
            columns[0] = (VBox) columnsRow.getChildren().get(0);
            columns[1] = (VBox) columnsRow.getChildren().get(1);
            laidOut.countDown();
        });

        assertTrue(laidOut.await(10, TimeUnit.SECONDS), "layout pass ran");
        assertEquals(columns[0].getWidth(), columns[1].getWidth(),
                "columns are equally wide, but were " + columns[0].getWidth() + " and " + columns[1].getWidth());
    }

    /// The shape of the AI tab's expert settings: a group holding a labelled field and then the
    /// two-column block of stacked fields.
    @Test
    void columnsInsideAGroupKeepTheirCells() {
        VBox root = form()
                .group(expert -> expert
                        .stringField("API base URL", new SimpleStringProperty(""))
                        .columns(columns -> columns
                                .group(left -> left.stackedField("Context window size", new TextField()))
                                .group(right -> right.stackedField("Temperature", new TextField()))))
                .build();

        Node group = root.getChildren().getFirst();
        Node columns = childrenOf(group).stream()
                                        .filter(HBox.class::isInstance)
                                        .findFirst()
                                        .orElseThrow(() -> new AssertionError("the columns block is missing from the group"));
        assertEquals(2, childrenOf(columns).size(), "both column groups are present");
        assertEquals(1, childrenOf(childrenOf(columns).getFirst()).size());
    }
}
