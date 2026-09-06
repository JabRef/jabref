package org.jabref.gui.collab.groupchange;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.logic.bibtex.comparator.GroupDiff;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class GroupChangeDetailsViewTest extends ApplicationTest {

    @ParameterizedTest
    @EnumSource(DiffHighlighter.BasicDiffMethod.class)
    void highlightsRenamedGroup(DiffHighlighter.BasicDiffMethod diffMethod) {
        interact(() -> {
            SplitPane comparison = comparison("Old", "New", diffMethod);
            assertEquals("Before", ((Label) ((VBox) comparison.getItems().getFirst()).getChildren().getFirst()).getText());
            assertEquals("After", ((Label) ((VBox) comparison.getItems().getLast()).getChildren().getFirst()).getText());
            assertEquals("Old\n", textArea(comparison, 0).getText());
            assertEquals("New\n", textArea(comparison, 1).getText());
            assertEquals(List.of("deletion"), List.copyOf(textArea(comparison, 0).getStyleOfChar(0)));
            assertEquals(List.of("updated"), List.copyOf(textArea(comparison, 1).getStyleOfChar(0)));
            assertFalse(textArea(comparison, 0).isEditable());
            assertFalse(textArea(comparison, 1).isEditable());
        });
    }

    @ParameterizedTest
    @CsvSource({
            "'', Added, 1, addition",
            "Removed, '', 0, deletion"
    })
    void highlightsAddedOrRemovedTree(String before, String after, int highlightedSide, String style) {
        interact(() -> {
            SplitPane comparison = comparison(before, after, DiffHighlighter.BasicDiffMethod.CHARS);
            assertEquals(before.isEmpty() ? "" : before + '\n', textArea(comparison, 0).getText());
            assertEquals(after.isEmpty() ? "" : after + '\n', textArea(comparison, 1).getText());
            assertEquals(List.of(style), List.copyOf(textArea(comparison, highlightedSide).getStyleOfChar(0)));
        });
    }

    private SplitPane comparison(String before, String after, DiffHighlighter.BasicDiffMethod diffMethod) {
        GroupChange change = mock(GroupChange.class);
        when(change.getGroupDiff()).thenReturn(GroupDiff.compare(metadata(before), metadata(after)).orElseThrow());
        GroupChangeDetailsView view = new GroupChangeDetailsView(change, "Groups", "Before", "After", diffMethod);
        VBox container = (VBox) view.getChildren().getFirst();
        SplitPane outerPane = (SplitPane) container.getChildren().getLast();
        VBox diffContainer = (VBox) outerPane.getItems().getFirst();
        return (SplitPane) diffContainer.getChildren().getFirst();
    }

    private MetaData metadata(String name) {
        MetaData metadata = new MetaData();
        if (!name.isEmpty()) {
            metadata.setGroups(new GroupTreeNode(new ExplicitGroup(name, GroupHierarchyType.INDEPENDENT, ',')));
        }
        return metadata;
    }

    private StyleClassedTextArea textArea(SplitPane comparison, int side) {
        VBox container = (VBox) comparison.getItems().get(side);
        ScrollPane scrollPane = (ScrollPane) container.getChildren().getLast();
        return (StyleClassedTextArea) scrollPane.getContent();
    }
}
