package org.jabref.gui.collab.metedatachange;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.SplitDiffHighlighter;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.fxmisc.richtext.StyleClassedTextArea;

public final class MetadataChangeDetailsView extends DatabaseChangeDetailsView {

    public MetadataChangeDetailsView(MetadataChange metadataChange, GlobalCitationKeyPatterns globalCitationKeyPatterns) {
        VBox container = new VBox(15);

        Label header = new Label(Localization.lang("The following metadata changed:"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        // Add views for each detected difference
        for (MetaDataDiff.Difference diff : metadataChange.getMetaDataDiff().getDifferences(globalCitationKeyPatterns)) {
            addDifferenceView(container, diff, metadataChange);
        }

        this.setAllAnchorsAndAttachChild(container);
    }

    /**
     * Adds a view for a specific metadata difference to the container.
     * Default view if not a group diff.
     *
     * @param container      The parent container to add the difference view to
     * @param diff           The metadata difference to display
     * @param metadataChange The metadata change object containing all changes
     */
    private void addDifferenceView(VBox container, MetaDataDiff.Difference diff, MetadataChange metadataChange) {
        Label typeLabel = new Label(getDifferenceString(diff.differenceType()));
        typeLabel.getStyleClass().add("diff-type-label");
        container.getChildren().add(typeLabel);

        // Show appropriate view based on difference type
        if (diff.differenceType() == MetaDataDiff.DifferenceType.GROUPS) {
            container.getChildren().add(createGroupDiffSplitPane(metadataChange));
        } else {
            container.getChildren().add(createDefaultDiffScrollPane(diff));
        }
    }

    /**
     * Creates a scroll pane showing simple text differences.
     *
     * @param diff The difference to display
     * @return Configured ScrollPane showing the difference
     */
    private ScrollPane createDefaultDiffScrollPane(MetaDataDiff.Difference diff) {
        VBox diffContainer = new VBox(15);

        // Show both original and new values
        diffContainer.getChildren().add(new Label(diff.originalObject().toString()));
        diffContainer.getChildren().add(new Label(diff.newObject().toString()));

        ScrollPane scrollPane = new ScrollPane(diffContainer);
        scrollPane.setFitToWidth(true);
        return scrollPane;
    }

    /**
     * Creates a split pane showing differences in groups tree structure.
     *
     * @param metadataChange The metadata change containing groups differences
     * @return Configured SplitPane showing groups differences
     */
    private SplitPane createGroupDiffSplitPane(MetadataChange metadataChange) {
        StyleClassedTextArea jabrefTextArea = createConfiguredTextArea();
        StyleClassedTextArea diskTextArea = createConfiguredTextArea();

        String jabRefContent = getMetadataGroupsContent(metadataChange.getMetaDataDiff().getOriginalMetaData());
        String diskContent = getMetadataGroupsContent(metadataChange.getMetaDataDiff().getNewMetaData());

        jabrefTextArea.replaceText(jabRefContent);
        diskTextArea.replaceText(diskContent);

        SplitDiffHighlighter highlighter = new SplitDiffHighlighter(
                jabrefTextArea,
                diskTextArea,
                DiffHighlighter.BasicDiffMethod.CHARS
        );
        highlighter.highlight();

        ScrollPane leftScrollPane = createScrollPane(jabrefTextArea);
        ScrollPane rightScrollPane = createScrollPane(diskTextArea);

        Label inJabRef = new Label(Localization.lang("In JabRef"));
        inJabRef.getStyleClass().add("lib-change-header");
        Label onDisk = new Label(Localization.lang("On disk"));
        onDisk.getStyleClass().add("lib-change-header");

        VBox leftContainer = new VBox(5, inJabRef, leftScrollPane);
        VBox rightContainer = new VBox(5, onDisk, rightScrollPane);

        SplitPane splitPane = new SplitPane(leftContainer, rightContainer);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5);

        Label legendLabel = new Label(Localization.lang("Red: Removed, Blue: Changed, Green: Added"));
        legendLabel.getStyleClass().add("lib-change-legend");

        VBox resultContainer = new VBox(splitPane, legendLabel);
        resultContainer.setSpacing(5);

        return new SplitPane(resultContainer);
    }

    /**
     * Creates a configured scroll pane for a text area.
     *
     * @param textArea The text area to wrap in a scroll pane
     * @return Configured ScrollPane
     */
    private ScrollPane createScrollPane(StyleClassedTextArea textArea) {
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setAllAnchorsAndAttachChild(scrollPane);
        return scrollPane;
    }

    /**
     * Creates a configured text area for displaying diff content.
     *
     * @return Configured StyleClassedTextArea
     */
    private StyleClassedTextArea createConfiguredTextArea() {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setAutoHeight(true);
        return textArea;
    }

    /**
     * Extracts the groups tree content from metadata as a string.
     *
     * @param metadata The metadata containing groups
     * @return String representation of groups tree, or empty string if no groups
     */
    private String getMetadataGroupsContent(MetaData metadata) {
        return metadata.getGroups()
                       .map(this::convertGroupTreeToString)
                       .orElse("");
    }

    /**
     * Converts a group tree to a string representation with indentation.
     *
     * @param node The root node of the group tree
     * @return String representation of the group tree
     */
    private String convertGroupTreeToString(GroupTreeNode node) {
        StringBuilder builder = new StringBuilder();
        appendGroupTreeNode(node, builder, 0);
        return builder.toString();
    }

    /**
     * Recursively appends a group tree node to the string builder.
     *
     * @param node    The current node to append
     * @param builder The string builder to append to
     * @param level   The current depth level in the tree (for indentation)
     */
    private void appendGroupTreeNode(GroupTreeNode node, StringBuilder builder, int level) {
        builder.append("|  ".repeat(level))
               .append(node.getName())
               .append("\n");

        for (GroupTreeNode child : node.getChildren()) {
            appendGroupTreeNode(child, builder, level + 1);
        }
    }

    private String getDifferenceString(MetaDataDiff.DifferenceType changeType) {
        return switch (changeType) {
            case PROTECTED ->
                    Localization.lang("Library protection");
            case GROUPS ->
                    Localization.lang("Modified groups tree");
            case ENCODING ->
                    Localization.lang("Library encoding");
            case SAVE_SORT_ORDER ->
                    Localization.lang("Save sort order");
            case KEY_PATTERNS ->
                    Localization.lang("Key patterns");
            case USER_FILE_DIRECTORY ->
                    Localization.lang("User-specific file directory");
            case LATEX_FILE_DIRECTORY ->
                    Localization.lang("LaTeX file directory");
            case DEFAULT_KEY_PATTERN ->
                    Localization.lang("Default pattern");
            case SAVE_ACTIONS ->
                    Localization.lang("Save actions");
            case MODE ->
                    Localization.lang("Library mode");
            case LIBRARY_SPECIFIC_FILE_DIRECTORY ->
                    Localization.lang("Library-specific file directory");
            case CONTENT_SELECTOR ->
                    Localization.lang("Content selectors");
        };
    }
}
