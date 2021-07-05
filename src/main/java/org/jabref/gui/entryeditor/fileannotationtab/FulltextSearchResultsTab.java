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
        PdfSearchResults searchResults = stateManager.activeSearchQueryProperty().get().get().getRule().getFulltextResults(stateManager.activeSearchQueryProperty().get().get().getQuery(), stateManager.getActiveDatabase().get(), entry);
        StringBuilder content = new StringBuilder("<p>" + Localization.lang("Search results") + " </p>");

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            content.append("<p>Found match in <a href=");
            content.append(searchResult.getPath());
            content.append(">");
            content.append(searchResult.getPath());
            content.append("</a></p><p>");
            content.append(searchResult.getHtml());
            content.append("</p>");
        }

        webView.getEngine().loadContent(content.toString());
    }
}
