package org.jabref.gui.entryeditor.fileannotationtab;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.util.Theme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.FilePreferences;

public class FulltextSearchResultsTab extends EntryEditorTab {

    private final StateManager stateManager;
    private final FilePreferences filePreferences;

    private final TextFlow content;

    private BibEntry entry;

    public FulltextSearchResultsTab(StateManager stateManager, Theme theme, FilePreferences filePreferences) {
        this.stateManager = stateManager;
        this.filePreferences = filePreferences;
        content = new TextFlow();
        setContent(content);
        setText(Localization.lang("Search results"));
        this.stateManager.activeSearchQueryProperty().addListener((observable, oldValue, newValue) -> bindToEntry(entry));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return this.stateManager.activeSearchQueryProperty().isPresent().get() &&
                this.stateManager.activeSearchQueryProperty().get().isPresent() &&
                this.stateManager.activeSearchQueryProperty().get().get().getSearchFlags().contains(SearchRules.SearchFlags.FULLTEXT) &&
                this.stateManager.activeSearchQueryProperty().get().get().getQuery().length() > 0;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        this.entry = entry;
        if (!shouldShow(entry) || entry == null) {
            return;
        }
        PdfSearchResults searchResults = stateManager.activeSearchQueryProperty().get().get().getRule().getFulltextResults(stateManager.activeSearchQueryProperty().get().get().getQuery(), entry);

        content.getChildren().clear();

        if (searchResults.numSearchResults() == 0) {
            content.getChildren().add(new Text(Localization.lang("No search matches.")));
        }

        for (Map.Entry<String, HashMap<Integer, List<SearchResult>>> resultsPath : searchResults.getSearchResultsByPathAndPage().entrySet()) {
            LinkedFile linkedFile = new LinkedFile("", Path.of(resultsPath.getKey()), "pdf");
            Text fileLinkText = new Text(resultsPath.getKey() + "\n"); // tooltip with absolute path?
            content.getChildren().add(fileLinkText);
            for (Map.Entry<Integer, List<SearchResult>> resultsPage : resultsPath.getValue().entrySet()) {
                Text pageLinkText = new Text(Localization.lang("Page %0", resultsPage.getKey()) + "\n"); // tooltip with absolute path?
                content.getChildren().add(pageLinkText);
                for (SearchResult searchResult : resultsPage.getValue()) {
                    for (String resultTextHtml : searchResult.getResultStringsHtml()) {
                        content.getChildren().addAll(highlightResultString(resultTextHtml));
                        Separator hitSeparator = new Separator(Orientation.HORIZONTAL);
                        hitSeparator.prefWidthProperty().bind(content.widthProperty());
                        content.getChildren().add(hitSeparator);
                    }
                }
            }
        }
    }

    private List<Text> highlightResultString(String htmlHighlightedResult) {
        List<Text> highlightedResultsStrings = new ArrayList<>();

        for (String fragment : htmlHighlightedResult.split(SearchResult.HIGHLIGHTING_PRE_TAG)) {
            String[] splitPost = fragment.split(SearchResult.HIGHLIGHTING_POST_TAG);
            if (splitPost.length > 2) {
                throw new IllegalArgumentException("More than one POST_TAG for a PRE_TAG");
            }
            if (splitPost.length == 1) {
                highlightedResultsStrings.add(new Text(splitPost[0]));
            } else {
                Text highlightedText = new Text(splitPost[0]);
                highlightedText.setStyle("-fx-fill: -jr-green;");
                highlightedResultsStrings.add(highlightedText);
                highlightedResultsStrings.add(new Text(splitPost[1]));
            }
        }

        return highlightedResultsStrings;
    }
}
