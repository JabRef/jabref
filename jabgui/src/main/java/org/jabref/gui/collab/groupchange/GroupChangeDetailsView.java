package org.jabref.gui.collab.groupchange;

import java.util.Optional;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GroupChangeDetailsView extends DatabaseChangeDetailsView {

    public GroupChangeDetailsView(GroupChange groupChange) {
        this(groupChange,
                Localization.lang("%0. Accepting the change replaces the complete groups tree with the externally modified groups tree.", groupChange.getName()),
                Localization.lang("In JabRef"),
                Localization.lang("On disk"),
                DiffHighlighter.BasicDiffMethod.CHARS);
    }

    public GroupChangeDetailsView(GroupChange groupChange, String labelValue, String leftLabelText, String rightLabelText, DiffHighlighter.BasicDiffMethod diffMethod) {
        Label label = new Label(labelValue);
        label.setWrapText(true);

        SplitPane diffPane = createGroupDiffSplitPane(
                Optional.ofNullable(groupChange.getGroupDiff().getOriginalGroupRoot()),
                Optional.ofNullable(groupChange.getGroupDiff().getNewGroupRoot()),
                leftLabelText, rightLabelText, diffMethod);
        VBox container = new VBox(12, label, diffPane);
        VBox.setVgrow(diffPane, Priority.ALWAYS);
        setAllAnchorsAndAttachChild(container);
    }

    /// Creates a split pane showing differences in groups tree structure.
    ///
    /// @return Configured SplitPane showing groups differences
    public static SplitPane createGroupDiffSplitPane(Optional<GroupTreeNode> originalGroups, Optional<GroupTreeNode> newGroups, String leftLabelText, String rightLabelText, DiffHighlighter.BasicDiffMethod diffMethod) {
        StyleClassedTextArea jabrefTextArea = createConfiguredTextArea();
        StyleClassedTextArea diskTextArea = createConfiguredTextArea();

        String jabRefContent = originalGroups.map(GroupChangeDetailsView::convertGroupTreeToString).orElse("");
        String diskContent = newGroups.map(GroupChangeDetailsView::convertGroupTreeToString).orElse("");

        jabrefTextArea.replaceText(jabRefContent);
        diskTextArea.replaceText(diskContent);

        if (originalGroups.isEmpty()) {
            diskTextArea.setStyleClass(0, diskContent.length(), "addition");
        } else {
            SplitDiffHighlighter highlighter = new SplitDiffHighlighter(jabrefTextArea, diskTextArea, diffMethod);
            highlighter.highlight();
        }

        ScrollPane leftScrollPane = createScrollPane(jabrefTextArea);
        ScrollPane rightScrollPane = createScrollPane(diskTextArea);

        Label inJabRef = new Label(leftLabelText);
        inJabRef.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_HEADER);
        Label onDisk = new Label(rightLabelText);
        onDisk.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_HEADER);

        VBox leftContainer = new VBox(4, inJabRef, leftScrollPane);
        VBox rightContainer = new VBox(4, onDisk, rightScrollPane);
        VBox.setVgrow(leftScrollPane, Priority.ALWAYS);
        VBox.setVgrow(rightScrollPane, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftContainer, rightContainer);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5);

        Label legendLabel = new Label(Localization.lang("Red: Removed, Blue: Changed, Green: Added"));
        legendLabel.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_LEGEND);

        VBox resultContainer = new VBox(splitPane, legendLabel);
        resultContainer.setSpacing(5);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        return new SplitPane(resultContainer);
    }

    /// Creates a configured scroll pane for a text area.
    ///
    /// @param textArea The text area to wrap in a scroll pane
    /// @return Configured ScrollPane
    private static ScrollPane createScrollPane(StyleClassedTextArea textArea) {
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("lib-change-scroll-pane");
        return scrollPane;
    }

    /// Creates a configured text area for displaying diff content.
    ///
    /// @return Configured StyleClassedTextArea
    private static StyleClassedTextArea createConfiguredTextArea() {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setAutoHeight(true);
        textArea.getStyleClass().addAll("lib-change-text-area");
        return textArea;
    }

    /// Converts a group tree to a string representation with indentation.
    ///
    /// @param node The root node of the group tree
    /// @return String representation of the group tree
    private static String convertGroupTreeToString(GroupTreeNode node) {
        StringBuilder builder = new StringBuilder();
        appendGroupTreeNode(node, builder, 0);
        return builder.toString();
    }

    /// Recursively appends a group tree node to the string builder.
    ///
    /// @param node    The current node to append
    /// @param builder The string builder to append to
    /// @param level   The current depth level in the tree (for indentation)
    private static void appendGroupTreeNode(GroupTreeNode node, StringBuilder builder, int level) {
        builder.repeat("|  ", level)
               .append(node.getName())
               .append("\n");

        for (GroupTreeNode child : node.getChildren()) {
            appendGroupTreeNode(child, builder, level + 1);
        }
    }
}
