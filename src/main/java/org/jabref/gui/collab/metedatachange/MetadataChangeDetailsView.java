package org.jabref.gui.collab.metedatachange;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public final class MetadataChangeDetailsView extends DatabaseChangeDetailsView {

    public MetadataChangeDetailsView(MetadataChange metadataChange, PreferencesService preferencesService) {
        VBox container = new VBox(15);

        Label header = new Label(Localization.lang("The following metadata changed:"));
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        for (MetaDataDiff.Difference diff : metadataChange.getMetaDataDiff().getDifferences(preferencesService)) {
            container.getChildren().add(new Label(getDifferenceString(diff.differenceType())));
            container.getChildren().add(new Label(diff.originalObject().toString()));
            container.getChildren().add(new Label(diff.newObject().toString()));
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        getChildren().setAll(scrollPane);

        setLeftAnchor(scrollPane, 8d);
        setTopAnchor(scrollPane, 8d);
        setRightAnchor(scrollPane, 8d);
        setBottomAnchor(scrollPane, 8d);
    }

    private String getDifferenceString(MetaDataDiff.DifferenceType changeType) {
        return switch (changeType) {
            case PROTECTED ->
                    Localization.lang("Library protection");
            case GROUPS_ALTERED ->
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
            case GENERAL_FILE_DIRECTORY ->
                    Localization.lang("General file directory");
            case CONTENT_SELECTOR ->
                    Localization.lang("Content selectors");
        };
    }
}
