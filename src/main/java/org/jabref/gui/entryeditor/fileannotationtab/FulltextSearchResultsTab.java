package org.jabref.gui.entryeditor.fileannotationtab;

import java.nio.file.Path;

import javafx.scene.web.WebView;

import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.util.OpenHyperlinksInExternalBrowser;
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

    private final WebView webView;

    public FulltextSearchResultsTab(StateManager stateManager, Theme theme, FilePreferences filePreferences) {
        this.stateManager = stateManager;
        this.filePreferences = filePreferences;
        webView = new WebView();
        setTheme(theme);
        webView.getEngine().loadContent(wrapHTML("<p>" + Localization.lang("Search results") + "</p>"));
        setContent(webView);
        webView.getEngine().getLoadWorker().stateProperty().addListener(new OpenHyperlinksInExternalBrowser(webView));
        setText(Localization.lang("Search results"));
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
        if (!shouldShow(entry)) {
            return;
        }
        PdfSearchResults searchResults = stateManager.activeSearchQueryProperty().get().get().getRule().getFulltextResults(stateManager.activeSearchQueryProperty().get().get().getQuery(), entry);
        StringBuilder content = new StringBuilder();

        content.append("<p>");
        if (searchResults.numSearchResults() == 0) {
            content.append(Localization.lang("No search matches."));
        } else {
            content.append(Localization.lang("Search results"));
        }
        content.append("</p>");

        for (SearchResult searchResult : searchResults.getSearchResults()) {
            content.append("<p>");
            LinkedFile linkedFile = new LinkedFile("just for link", Path.of(searchResult.getPath()), "pdf");
            Path resolvedPath = linkedFile.findIn(stateManager.getActiveDatabase().get(), filePreferences).orElse(Path.of(searchResult.getPath()));
            String link = "<a href=" + resolvedPath.toAbsolutePath().toString() + ">" + searchResult.getPath() + "</a>";
            content.append(Localization.lang("Found match in %0", link));
            content.append("</p><p>");
            content.append(searchResult.getHtml());
            content.append("</p>");
        }

        webView.getEngine().loadContent(wrapHTML(content.toString()));
    }

    private String wrapHTML(String content) {
        return "<html><body id=\"previewBody\"><div id=\"content\">" + content + "</div></body></html>";
    }

    public void setTheme(Theme theme) {
        theme.getAdditionalStylesheet().ifPresent(location -> webView.getEngine().setUserStyleSheetLocation(location));
    }
}
