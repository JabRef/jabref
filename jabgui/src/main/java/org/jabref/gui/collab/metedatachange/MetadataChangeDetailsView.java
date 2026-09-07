package org.jabref.gui.collab.metedatachange;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.l10n.Localization;

public final class MetadataChangeDetailsView extends DatabaseChangeDetailsView {

    public MetadataChangeDetailsView(MetadataChange metadataChange, GlobalCitationKeyPatterns globalCitationKeyPatterns) {
        this(metadataChange, globalCitationKeyPatterns, Localization.lang("In JabRef"), Localization.lang("On disk"), DiffHighlighter.BasicDiffMethod.CHARS);
    }

    public MetadataChangeDetailsView(MetadataChange metadataChange,
                                     GlobalCitationKeyPatterns globalCitationKeyPatterns,
                                     String leftLabelText,
                                     String rightLabelText,
                                     DiffHighlighter.BasicDiffMethod diffMethod) {
        VBox container = new VBox(15);

        Label header = new Label(Localization.lang("The following metadata changed:"));
        header.getStyleClass().addAll(StyleClasses.SECTION_HEADER);
        container.getChildren().add(header);

        // Add views for each detected difference
        for (MetaDataDiff.Difference diff : metadataChange.getMetaDataDiff().getDifferences(globalCitationKeyPatterns)) {
            addDifferenceView(container, diff, metadataChange, leftLabelText, rightLabelText, diffMethod);
        }

        this.setAllAnchorsAndAttachChild(container);
    }

    /// Adds a view for a specific metadata difference to the container.
    /// Default view if not a group diff.
    ///
    /// @param container      The parent container to add the difference view to
    /// @param diff           The metadata difference to display
    /// @param metadataChange The metadata change object containing all changes
    private void addDifferenceView(VBox container,
                                   MetaDataDiff.Difference diff,
                                   MetadataChange metadataChange,
                                   String leftLabelText,
                                   String rightLabelText,
                                   DiffHighlighter.BasicDiffMethod diffMethod) {
        Label typeLabel = new Label(getDifferenceString(diff.differenceType()));
        typeLabel.getStyleClass().add("diff-type-label");
        container.getChildren().add(typeLabel);

        // Show appropriate view based on difference type
        if (diff.differenceType() == MetaDataDiff.DifferenceType.GROUPS) {
            container.getChildren().add(GroupChangeDetailsView.createGroupDiffSplitPane(
                    metadataChange.getMetaDataDiff().getOriginalMetaData().getGroups(),
                    metadataChange.getMetaDataDiff().getNewMetaData().getGroups(),
                    leftLabelText, rightLabelText, diffMethod));
        } else {
            container.getChildren().add(createDefaultDiffScrollPane(diff));
        }
    }

    /// Creates a scroll pane showing simple text differences.
    ///
    /// @param diff The difference to display
    /// @return Configured ScrollPane showing the difference
    private ScrollPane createDefaultDiffScrollPane(MetaDataDiff.Difference diff) {
        VBox diffContainer = new VBox(12);

        // Show both original and new values
        diffContainer.getChildren().add(new Label(diff.originalObject().toString()));
        diffContainer.getChildren().add(new Label(diff.newObject().toString()));

        ScrollPane scrollPane = new ScrollPane(diffContainer);
        scrollPane.setFitToWidth(true);
        return scrollPane;
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
