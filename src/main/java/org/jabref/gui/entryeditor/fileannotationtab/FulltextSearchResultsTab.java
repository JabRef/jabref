package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.scene.web.WebView;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.pdf.search.PdfSearchResults;
import org.jabref.model.pdf.search.SearchResult;

public class FulltextSearchResultsTab extends EntryEditorTab {

    private final StateManager stateManager;

    private final WebView webView;

    public FulltextSearchResultsTab(StateManager stateManager) {
        this.stateManager = stateManager;
        webView = new WebView();
        webView.getEngine().loadContent("<p>" + Localization.lang("Search results") + " </p>");
        setContent(webView);
        setText(Localization.lang("Search results"));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return this.stateManager.activeSearchQueryProperty().isPresent().get() &&
                this.stateManager.activeSearchQueryProperty().get().isPresent() &&
                this.stateManager.activeSearchQueryProperty().get().get().isFulltext() &&
                this.stateManager.activeSearchQueryProperty().get().get().getQuery().length() > 0;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!shouldShow(entry)) {
            return;
        }
        PdfSearchResults searchResults = stateManager.activeSearchQueryProperty().get().get().getRule().getFulltextResults(stateManager.activeSearchQueryProperty().get().get().getQuery(), entry);

        StringBuilder content = new StringBuilder("<p>" + Localization.lang("Search results") + " </p>");

        content.append("<table><tr>");
        content.append("<th>Author</th>");
        content.append("<th>Key</th>");
        content.append("<th>Keyword</th>");
        content.append("<th>Score</th>");
        content.append("<th>Subject</th>");
        content.append("<th>Path</th>");
        content.append("</tr>");

        for (SearchResult searchResult : searchResults.getSearchResults()) {

            content.append("<tr>");
            content.append("<td>" + searchResult.getAuthor() + "</td>");
            content.append("<td>" + searchResult.getKey() + "</td>");
            content.append("<td>" + searchResult.getKeyword() + "</td>");
            content.append("<td>" + searchResult.getLuceneScore() + "</td>");
            content.append("<td>" + searchResult.getSubject() + "</td>");
            content.append("<td>" + searchResult.getPath() + "</td>");
            content.append("</tr>");
        }

        webView.getEngine().loadContent(content.toString());
    }
}
