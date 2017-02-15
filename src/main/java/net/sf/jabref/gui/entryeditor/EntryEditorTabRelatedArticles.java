/**
 *
 */
package net.sf.jabref.gui.entryeditor;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JEditorPane;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.importer.fetcher.MrDLibFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This Component is displaying the recommendations received from Mr. DLib.
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane {

    //The entry the user selects in the table
    private final BibEntry selectedEntry;

    private static final Log LOGGER = LogFactory.getLog(EntryEditorTabRelatedArticles.class);

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
     * @param The entry selected by the user
     */
    public EntryEditorTabRelatedArticles(BibEntry selectedEntry) {
        this.selectedEntry = selectedEntry;
        this.setContentType("text/html");
        this.setEditable(false);
        registerHyperlinkListener();
        setDefaultContent();
    }


    /**
     * Takes a List of HTML snippets and sets it in the JEditorPane
     * @param list of HTML Strings
     */
    public void setHtmlText(List<BibEntry> list) {
        StringBuffer htmlContent = new StringBuffer();
        URL url = IconTheme.getIconUrl("mdlListIcon");
        htmlContent
                .append("<html><head><title></title></head><body bgcolor='#ffffff'>");
        htmlContent.append("<ul style='list-style-image:(");
        htmlContent.append(url);
        htmlContent.append(")'>");
        for (BibEntry bibEntry : list) {
            if (bibEntry != null) {
                htmlContent.append("<li style='margin: 5px'>");
                bibEntry.getField("html_representation").ifPresent(htmlContent::append);
                htmlContent.append("</li>");
            }
        }
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
        StringBuffer htmlContent = new StringBuffer();
        URL url = IconTheme.getIconUrl("mdlloading");
        htmlContent
                .append("<html><head><title></title></head><body bgcolor='#ffffff'><div align='center' style='font-size:20px'>");
        htmlContent.append(Localization.lang("Loading_Recommendations_for"));
        htmlContent.append(": ");
        htmlContent.append("<b>");
        htmlContent.append(selectedEntry.getLatexFreeField(FieldName.TITLE).get());
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
                        new JabRefDesktop().openBrowser(e.getURL().toString());
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
