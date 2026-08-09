package org.jabref.gui.preferences.forms;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.SearchableElement;

import com.dlsc.unitfx.IntegerInputField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
class PreferencesFormBuilderTest {

    /// The builder only needs its services for help buttons, which these forms do not use.
    private PreferencesFormBuilder form() {
        return new PreferencesFormBuilder(mock(DialogService.class), mock(GuiPreferences.class));
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
            columns[0] = (VBox) columnsRow.getChildren().getFirst();
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

    /// The escape hatch is the one place the builder did not place the text itself, so it reads the
    /// text back off the node. Otherwise every custom node would be a hole in the preferences search.
    @Test
    void customRegistersTheTextsOfItsSubtree() {
        HBox row = new HBox(new Label("Keyword separator"), new TextField(), new CheckBox("Overwrite"));

        PreferencesFormBuilder form = form();
        form.custom(row);

        assertEquals(List.of("Keyword separator", "Overwrite"), textsOf(form));
    }

    @Test
    void customHighlightsTheLabeledItselfRatherThanTheCustomNode() {
        Label caption = new Label("Cite command");

        PreferencesFormBuilder form = form();
        form.custom(new HBox(caption));

        assertSame(caption, form.getSearchableElements().getFirst().node());
    }

    /// A control captions itself; what is inside it belongs to its skin and is none of the search's
    /// business. A labeled without text captions nothing.
    @Test
    void customIgnoresControlInsidesAndBlankTexts() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("English", "Deutsch");

        PreferencesFormBuilder form = form();
        form.custom(new HBox(combo, new Label(""), new Label("Language")));

        assertEquals(List.of("Language"), textsOf(form));
    }

    @Test
    void searchableRegistersTextsThatAreInNoLabeled() {
        TextField field = new TextField();

        PreferencesFormBuilder form = form();
        form.custom(field, element -> element.searchable("Cite command", "LaTeX"));

        assertEquals(List.of("Cite command", "LaTeX"), textsOf(form));
        assertSame(field, form.getSearchableElements().getFirst().node());
    }

    private static List<String> textsOf(PreferencesFormBuilder form) {
        return form.getSearchableElements().stream().map(SearchableElement::text).toList();
    }
}
