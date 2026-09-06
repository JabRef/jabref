package org.jabref.gui.git;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.DiffHighlighter;
import org.jabref.gui.mergeentries.threewaymerge.diffhighlighter.SplitDiffHighlighter;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.theme.StyleClasses;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.fxmisc.richtext.StyleClassedTextArea;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class GitEntryChangeDetailsView extends AnchorPane {

    public GitEntryChangeDetailsView(BibEntry oldEntry,
                                     BibEntry newEntry,
                                     BibDatabaseContext oldDatabaseContext,
                                     BibDatabaseContext newDatabaseContext,
                                     GuiPreferences preferences,
                                     BibEntryTypesManager entryTypesManager,
                                     String oldVersionLabel,
                                     String newVersionLabel,
                                     DiffHighlighter.BasicDiffMethod diffMethod) {
        Label committedVersion = new Label(oldVersionLabel);
        committedVersion.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_HEADER);
        Label savedFile = new Label(newVersionLabel);
        savedFile.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_HEADER);

        StyleClassedTextArea oldSourceArea = createConfiguredTextArea(oldEntry, oldDatabaseContext, preferences, entryTypesManager);
        StyleClassedTextArea newSourceArea = createConfiguredTextArea(newEntry, newDatabaseContext, preferences, entryTypesManager);
        new SplitDiffHighlighter(oldSourceArea, newSourceArea, diffMethod).highlight();

        ScrollPane leftScrollPane = createScrollPane(oldSourceArea);
        ScrollPane rightScrollPane = createScrollPane(newSourceArea);
        VBox leftContainer = new VBox(5, committedVersion, leftScrollPane);
        VBox rightContainer = new VBox(5, savedFile, rightScrollPane);
        SplitPane splitPane = new SplitPane(leftContainer, rightContainer);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5);

        Label legendLabel = new Label(Localization.lang("Red: Removed, Blue: Changed, Green: Added"));
        legendLabel.getStyleClass().addAll(StyleClasses.CHANGE_VIEW_LEGEND);

        VBox resultContainer = new VBox(splitPane, legendLabel);
        resultContainer.setSpacing(5);
        setAllAnchorsAndAttachChild(resultContainer);
    }

    private StyleClassedTextArea createConfiguredTextArea(BibEntry entry,
                                                          BibDatabaseContext databaseContext,
                                                          GuiPreferences preferences,
                                                          BibEntryTypesManager entryTypesManager) {
        StyleClassedTextArea textArea = new StyleClassedTextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setAutoHeight(true);
        textArea.getStyleClass().add("lib-change-text-area");
        textArea.replaceText(entry.getStringRepresentation(entry, databaseContext.getMode(), entryTypesManager, preferences.getFieldPreferences()));
        textArea.moveTo(0, 0);
        textArea.showParagraphAtTop(0);
        return textArea;
    }

    private ScrollPane createScrollPane(StyleClassedTextArea textArea) {
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("lib-change-scroll-pane");
        Platform.runLater(() -> {
            scrollPane.setHvalue(0);
            scrollPane.setVvalue(0);
        });
        return scrollPane;
    }

    private void setAllAnchorsAndAttachChild(javafx.scene.Node child) {
        double anchorPaneOffset = 8D;
        setLeftAnchor(child, anchorPaneOffset);
        setTopAnchor(child, anchorPaneOffset);
        setRightAnchor(child, anchorPaneOffset);
        setBottomAnchor(child, anchorPaneOffset);
        getChildren().setAll(child);
    }
}
