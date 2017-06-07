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
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class RelatedArticlesTab extends EntryEditorTab {

    private final BibEntry entry;

    public RelatedArticlesTab(BibEntry entry) {
        this.entry = entry;
        setText(Localization.lang("Related articles"));
        setTooltip(new Tooltip(Localization.lang("Related articles")));
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
                .executeWith(Globals.taskExecutor);
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
                "<a href='http://mr-dlib.org/information-for-users/information-about-mr-dlib-for-jabref-users/#'>");
        htmlContent.append(Localization.lang("What_is_Mr._DLib?"));
        htmlContent.append("</a></div>");
        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }

    @Override
    public boolean shouldShow() {
        return Globals.prefs.getBoolean(JabRefPreferences.SHOW_RECOMMENDATIONS);
    }

    @Override
    protected void initialize() {
        setContent(getPane(entry));
    }
}
