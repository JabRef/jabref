package org.jabref.gui.entryeditor;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.OpenHyperlinksInExternalBrowser;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class RelatedArticlesTab extends EntryEditorTab {

    private final JabRefPreferences preferences;

    public RelatedArticlesTab(JabRefPreferences preferences) {
        setText(Localization.lang("Related articles"));
        setTooltip(new Tooltip(Localization.lang("Related articles")));
        this.preferences = preferences;
    }

    private StackPane getPane(BibEntry entry) {
        StackPane root = new StackPane();
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(100, 100);
        WebView browser = new WebView();
        root.getChildren().addAll(browser, progress);

        MrDLibFetcher fetcher = new MrDLibFetcher(Globals.prefs.get(JabRefPreferences.LANGUAGE),
                Globals.BUILD_INFO.getVersion().getFullVersion());
        BackgroundTask
                .wrap(() -> fetcher.performSearch(entry))
                .onRunning(() -> progress.setVisible(true))
                .onSuccess(relatedArticles -> {
                    progress.setVisible(false);
                    browser.getEngine().loadContent(convertToHtml(relatedArticles));
                })
                .executeWith(Globals.TASK_EXECUTOR);

        browser.getEngine().getLoadWorker().stateProperty().addListener(new OpenHyperlinksInExternalBrowser(browser));

        return root;
    }

    /**
     * Takes a List of HTML snippets stored in the field "html_representation" of a list of bibentries
     *
     * @param list of bib entries having a field html_representation
     */
    private String convertToHtml(List<BibEntry> list) {
        StringBuilder htmlContent = new StringBuilder();
        URL url = IconTheme.getIconUrl("mdlListIcon");
        htmlContent
                .append("<html><head><title></title></head><body bgcolor='#ffffff'>");
        htmlContent.append("<ul style='list-style-image:(");
        htmlContent.append(url);
        htmlContent.append(")'>");
        list.stream()
                .map(bibEntry -> bibEntry.getField("html_representation"))
                .filter(Optional::isPresent)
                .map(o -> "<li style='margin: 5px'>" + o.get() + "</li>")
                .forEach(html -> htmlContent.append(html));
        htmlContent.append("</ul>");
        htmlContent.append("<br><div style='margin-left: 5px'>");
        htmlContent.append(
                "<a href='http://mr-dlib.org/information-for-users/information-about-mr-dlib-for-jabref-users/#' target=\"_blank\">");
        htmlContent.append(Localization.lang("What is Mr. DLib?"));
        htmlContent.append("</a></div>");
        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS);
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        setContent(getPane(entry));
    }
}
