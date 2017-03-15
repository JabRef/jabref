package org.jabref.gui.entryeditor;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.JEditorPane;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;

import org.jabref.Globals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.logic.importer.fetcher.MrDLibFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This Component is displaying the recommendations received from Mr. DLib.
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane {

    private static final Log LOGGER = LogFactory.getLog(EntryEditorTabRelatedArticles.class);

    //The entry the user selects in the table
    private final BibEntry selectedEntry;

    private List<BibEntry> relatedArticles;

    private Boolean onFocus = true;


    /**
     * Access related acticles delivered by Mr. DLib.
     * @return the list of BibEntries, representing the related articles deliverd by MR. DLib
     */
    public List<BibEntry> getRelatedArticles() {
        return relatedArticles;
    }

    /**
     * Takes the selected entry, runs a request to Mr. DLib and returns the recommendations as a JEditorPane
     * @param selectedEntry The entry selected by the user
     */
    public EntryEditorTabRelatedArticles(BibEntry selectedEntry) {
        this.selectedEntry = selectedEntry;
        this.setContentType("text/html");
        this.setEditable(false);
        registerHyperlinkListener();
        setDefaultContent();
    }


    /**
     * Takes a List of HTML snippets stored in the field "html_representation" of a list of bibentries and sets it in the JEditorPane
     *
     * @param list of bib entries having a field html_representation
     */
    public void setHtmlText(List<BibEntry> list) {
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
        this.setText(htmlContent.toString());
    }

    /**
     * While the fetcher gets the Recommendations from MR. DLib this content indicates
     * that the search is in Progress
     */
    private void setDefaultContent() {
        StringBuilder htmlContent = new StringBuilder();
        URL url = IconTheme.getIconUrl("mdlloading");
        htmlContent
                .append("<html><head><title></title></head><body bgcolor='#ffffff'><div align='center' style='font-size:20px'>");
        htmlContent.append(Localization.lang("Loading_Recommendations_for"));
        htmlContent.append(": ");
        htmlContent.append("<b>");
        htmlContent.append(selectedEntry.getLatexFreeField(FieldName.TITLE).orElseGet(() -> ""));
        htmlContent.append("<div>");
        htmlContent.append(
                "<a href='http://mr-dlib.org/information-for-users/information-about-mr-dlib-for-jabref-users/#'>");
        htmlContent.append(Localization.lang("What_is_Mr._DLib?"));
        htmlContent.append("</a></div>");
        htmlContent.append("</b><br><img width=\"100\" height=\"100\" src=\"" + url + "\"></img></div>");
        htmlContent.append("</body></html>");
        this.setText(htmlContent.toString());
    }

    /**
     * Makes the Hyperlinks clickable. Opens the link destination in a Browsertab
     */
    private void registerHyperlinkListener() {
        this.addHyperlinkListener(e -> {
            try {
                if ((e.getURL() != null) && (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)) {
                    JabRefDesktop.openBrowser(e.getURL().toString());
                    }
                } catch (IOException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }

        });
    }

    public void focus() {
        if (onFocus) {
            requestRecommendations();
            onFocus = false;
        }
    }

    /**
     * Starts a Fetcher getting the recommendations form Mr. DLib
     */
    public void requestRecommendations() {
        //The Fetcher delivers the recommendations
        MrDLibFetcherWorker mdlFetcher;
        try {
            mdlFetcher = new MrDLibFetcherWorker(selectedEntry);
            mdlFetcher.addPropertyChangeListener(evt -> {
                if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
                    try {
                        relatedArticles = mdlFetcher.get();
                        setHtmlText(relatedArticles);
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            });
            mdlFetcher.execute();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Helper Class to initiate SwingWorker
     */
    class MrDLibFetcherWorker extends SwingWorker<List<BibEntry>, Void> {
        private final MrDLibFetcher fetcher;
        private final JabRefPreferences prefs = JabRefPreferences.getInstance();
        private final BibEntry selectedEntry;

        public MrDLibFetcherWorker(BibEntry selectedEntry) throws Exception {
            this.selectedEntry = selectedEntry;
            fetcher = new MrDLibFetcher(prefs.get(JabRefPreferences.LANGUAGE),
                    Globals.BUILD_INFO.getVersion().getFullVersion());
        }

        @Override
        protected List<BibEntry> doInBackground() throws Exception {
            return fetcher.performSearch(selectedEntry);
        }


    }


}
