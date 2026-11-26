package org.jabref.gui.preferences.forms;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.SearchableElement;
import org.jabref.gui.testutils.JavaFxExtension;

import com.dlsc.unitfx.IntegerInputField;
import org.controlsfx.control.ToggleSwitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(JavaFxExtension.class)
class PreferencesFormBuilderTest {

    /// The builder only needs its services for help buttons, which these forms do not use.
    private PreferencesFormBuilder form() {
        return new PreferencesFormBuilder(mock(DialogService.class), mock(GuiPreferences.class), mock(NativeDesktop.class));
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
    void stackedFieldsInColumnsAreLaidOut() {
        JavaFxExtension.invokeAndWait(() -> {
            IntegerInputField integerField = new IntegerInputField();
            TextField textField = new TextField();
            VBox root = form()
                    .group(expert -> expert
                            .columns(columns -> columns
                                    .group(left -> left.stackedField("Context window size", integerField))
                                    .group(right -> right.stackedField("Temperature", textField))))
                    .build();
            new Scene(root, 800, 600);
            root.applyCss();
            root.layout();
            assertTrue(integerField.getWidth() > 0 && integerField.getHeight() > 0,
                    "the integer field is laid out, but was " + integerField.getWidth() + "x" + integerField.getHeight());
            assertTrue(textField.getWidth() > 0 && textField.getHeight() > 0,
                    "the text field is laid out, but was " + textField.getWidth() + "x" + textField.getHeight());
        });
    }

    /// Columns are of equal width whatever they contain: a column claiming width in proportion to
    /// its longest caption is what the hand-built grids used percentage constraints to avoid.
    @Test
    void columnsAreEquallyWideRegardlessOfContent() {
        JavaFxExtension.invokeAndWait(() -> {
            VBox root = form()
                    .columns(row -> row
                            .group(left -> left.stackedField("A very much longer caption than the other", new TextField()))
                            .group(right -> right.stackedField("Short", new TextField())))
                    .build();
            new Scene(root, 900, 700);
            root.applyCss();
            root.layout();

            HBox columnsRow = (HBox) root.getChildren().getFirst();
            VBox leftColumn = (VBox) columnsRow.getChildren().getFirst();
            VBox rightColumn = (VBox) columnsRow.getChildren().get(1);
            assertEquals(leftColumn.getWidth(), rightColumn.getWidth(),
                    "columns are equally wide, but were " + leftColumn.getWidth() + " and " + rightColumn.getWidth());
        });
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
                                        .filter(node -> node instanceof HBox && !(node instanceof SettingTile))
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

    /// The section's tile group comes right after its header row.
    private static Node sectionGroup(VBox root) {
        return root.getChildren().get(1);
    }

    /// The tiles among the children of `container`, without the dividers between them.
    private static List<SettingTile> tilesIn(Node container) {
        return childrenOf(container).stream()
                                    .filter(SettingTile.class::isInstance)
                                    .map(SettingTile.class::cast)
                                    .toList();
    }

    private static SettingTile onlyTileIn(Node container) {
        List<SettingTile> tiles = tilesIn(container);
        assertEquals(1, tiles.size());
        return tiles.getFirst();
    }

    @Test
    void checkboxInSectionIsTileWithSwitch() {
        SimpleBooleanProperty enabled = new SimpleBooleanProperty(true);
        VBox root = form()
                .section("Section", section -> section.checkbox("Autosave", enabled))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertEquals("Autosave", tile.title().getText());
        ToggleSwitch toggle = assertInstanceOf(ToggleSwitch.class, tile.control());
        toggle.setSelected(false);
        assertFalse(enabled.get());
    }

    @Test
    void attachedFieldStartsTheControlsAndFollowsTheSwitch() {
        SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
        VBox root = form()
                .section("Section", section -> section
                        .checkWithField("Enable server on port", enabled, new SimpleStringProperty("6050")))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertSame(tile.control(), childrenOf(tile.control().getParent()).getLast(), "the switch ends the row");
        TextField port = assertInstanceOf(TextField.class, tile.controls().getFirst());
        assertTrue(port.isDisabled());
        enabled.set(true);
        assertFalse(port.isDisabled());
    }

    @Test
    void attachmentsFollowInTheOrderTheyWereAttached() {
        Label first = new Label("first");
        Label second = new Label("second");
        VBox root = form()
                .section("Section", section -> section
                        .stringField("Engine path", new SimpleStringProperty(""),
                                path -> path.attach(first).attach(second)))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertEquals(List.of(tile.control(), first, second), tile.controls());
    }

    @Test
    void grownFieldOfSwitchFillsTheRowBeforeItsBrowseButton() {
        VBox root = form()
                .section("Section", section -> section
                        .checkWithField("Custom theme", new SimpleBooleanProperty(true), new SimpleStringProperty(""),
                                path -> path.browse(() -> {
                                            })
                                            .grow()))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertEquals(2, tile.controls().size(), "the path field and its browse button");
        TextField path = assertInstanceOf(TextField.class, tile.controls().getFirst());
        assertEquals(Priority.ALWAYS, HBox.getHgrow(path));
    }

    @Test
    void whenOnShowsDependentSettingsOnlyWhileSwitchIsOn() {
        SimpleBooleanProperty overwrite = new SimpleBooleanProperty(false);
        VBox root = form()
                .section("Section", section -> section
                        .checkbox("Overwrite existing keys", overwrite,
                                keys -> keys.whenOn(dependent -> dependent
                                        .checkbox("Warn before overwriting", new SimpleBooleanProperty()))))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertEquals("Warn before overwriting", onlyTileIn(tile.details()).title().getText());
        assertFalse(tile.details().isVisible());
        overwrite.set(true);
        assertTrue(tile.details().isVisible());
    }

    @Test
    void enabledWhenOnKeepsDependentSettingsInSightButDisabledWhileSwitchIsOff() {
        SimpleBooleanProperty overwrite = new SimpleBooleanProperty(false);
        VBox root = form()
                .section("Section", section -> section
                        .checkbox("Overwrite existing keys", overwrite,
                                keys -> keys.enabledWhenOn(dependent -> dependent
                                        .checkbox("Warn before overwriting", new SimpleBooleanProperty()))))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertTrue(tile.details().isVisible());
        assertTrue(tile.details().isDisabled());
        overwrite.set(true);
        assertFalse(tile.details().isDisabled());
    }

    @Test
    void expanderBuildsItsContentIntoTheClosedDetails() {
        Label summary = new Label("Light");
        VBox root = form()
                .section("Section", section -> section
                        .expander("Theme", summary, theme -> theme.label("Gallery")))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertEquals(List.of(summary), tile.controls());
        assertFalse(tile.details().isVisible());
        assertEquals(1, tile.details().getChildren().size());
    }

    @Test
    void descriptionAddsALineBelowTheCaption() {
        PreferencesFormBuilder form = form();
        form.section("Section", section -> section
                .checkbox("Enable AI functionality", new SimpleBooleanProperty(),
                        ai -> ai.description("Chatting with entries.")));
        VBox root = form.build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        List<Node> captions = childrenOf(tile.title().getParent());
        assertEquals("Chatting with entries.", ((Label) captions.get(1)).getText());
        assertEquals(List.of("Section", "Enable AI functionality", "Chatting with entries."), textsOf(form));
    }

    @Test
    void consecutiveNonTileElementsInSectionShareOneCard() {
        VBox root = form()
                .section("Section", section -> section
                        .label("Order")
                        .radioGroup(order -> order
                                .radio("Ascending", new SimpleBooleanProperty(true))
                                .radio("Descending", new SimpleBooleanProperty(false)))
                        .checkbox("Autosave", new SimpleBooleanProperty()))
                .build();

        List<Node> contents = childrenOf(sectionGroup(root));
        VBox card = assertInstanceOf(VBox.class, contents.getFirst());
        assertEquals(List.of("setting-card"), card.getStyleClass());
        assertEquals(3, card.getChildren().size(), "the label and both radios");
        assertEquals(1, tilesIn(sectionGroup(root)).size());
    }

    @Test
    void neighboringRowsAreDividedByALine() {
        VBox root = form()
                .section("Section", section -> section
                        .checkbox("Autosave", new SimpleBooleanProperty())
                        .checkbox("Backup", new SimpleBooleanProperty()))
                .build();

        List<Node> contents = childrenOf(sectionGroup(root));
        assertEquals(3, contents.size());
        assertEquals(List.of("setting-row-divider"), contents.get(1).getStyleClass());
    }

    @Test
    void consecutiveTilesOutsideASectionShareOneBlock() {
        VBox root = form()
                .checkbox("Open editor", new SimpleBooleanProperty())
                .checkbox("Show validation messages", new SimpleBooleanProperty())
                .label("Interruption")
                .checkbox("Show source", new SimpleBooleanProperty())
                .build();

        assertEquals(3, root.getChildren().size(), "a block, the label, a second block");
        assertEquals(2, tilesIn(root.getChildren().getFirst()).size());
        assertEquals(1, tilesIn(root.getChildren().get(2)).size());
    }

    @Test
    void hidingCheckboxHidesItsWholeTile() {
        SimpleBooleanProperty supported = new SimpleBooleanProperty(false);
        VBox root = form()
                .section("Section", section -> section
                        .checkbox("Move to trash", new SimpleBooleanProperty(), trash -> trash.visibleWhen(supported)))
                .build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        assertFalse(tile.isVisible());
        assertFalse(tile.isManaged());
    }

    @Test
    void groupInSectionKeepsItsTilesOutOfCards() {
        VBox root = form()
                .section("Section", section -> section
                        .group(group -> group
                                .checkbox("Autosave", new SimpleBooleanProperty())
                                .stringField("Name", new SimpleStringProperty(""))))
                .build();

        assertEquals(2, tilesIn(childrenOf(sectionGroup(root)).getFirst()).size());
    }

    @Test
    void searchHighlightsTileCaptionOfCheckbox() {
        PreferencesFormBuilder form = form();
        form.section("Section", section -> section.checkbox("Autosave", new SimpleBooleanProperty()));
        VBox root = form.build();

        SettingTile tile = onlyTileIn(sectionGroup(root));
        SearchableElement autosave = form.getSearchableElements().get(1);
        assertEquals("Autosave", autosave.text());
        assertSame(tile.title(), autosave.node());
    }

    private static List<String> textsOf(PreferencesFormBuilder form) {
        return form.getSearchableElements().stream().map(SearchableElement::text).toList();
    }
}
