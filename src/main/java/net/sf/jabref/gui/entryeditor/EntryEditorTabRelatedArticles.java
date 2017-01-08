/**
 *
 */
package net.sf.jabref.gui.entryeditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JEditorPane;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.fetcher.MrDLibFetcher;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This Component is displaying the recommendations received from Mr. DLib.
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane {

    //The entry the user selects in the table
    private final BibEntry selectedEntry;

    //The Fetcher delivers the recommendatinons
    private MrDLibFetcherWorker mdlFetcher;

    private static final Log LOGGER = LogFactory.getLog(EntryEditorTabRelatedArticles.class);

    private List<BibEntry> relatedArticles;


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
                .append("<html><head><title></title></head><body bgcolor='#ffffff'><font size=8>");
        htmlContent.append("<ul style='list-style-image:(");
        htmlContent.append(url);
        htmlContent.append(")'>");
        for (BibEntry bibEntry : list) {
            if (bibEntry != null) {
                htmlContent.append("<li>");
                htmlContent.append(bibEntry.getLatexFreeField("html_representation").get());
                htmlContent.append("</li>");
            }
        }
        htmlContent.append("</ul></font></body></html>");
        this.setText(htmlContent.toString());

    }

    /**
     * While the fetcher gets the Recommendations from MR. DLib this content indicates
     * that the search is in Progress
     */
    private void setDefaultContent() {
        StringBuffer htmlContent = new StringBuffer();
        URL url = IconTheme.getIconUrl("mdlloading");
        htmlContent.append("<html><head><title></title></head><body bgcolor='#ffffff'><font size=5>");
        htmlContent.append("Loading Recommendations for ");
        htmlContent.append(selectedEntry.getLatexFreeField("title").get());
        htmlContent.append("<br><img width=\"100\" height=\"100\" src=\"" + url + "\"></img>");
        htmlContent.append("</font></body></html>");
        this.setText(htmlContent.toString());
    }

    /**
     * Makes the Hyperlinks clickable. Opens the link destination in a Browsertab
     */
    private void registerHyperlinkListener() {
        this.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    if (e.getURL() != null) {
                        new JabRefDesktop().openBrowser(e.getURL().toString());
                    }
                } catch (IOException e1) {
                    LOGGER.error(e1.getMessage(), e1);
                }
            }
        });
    }

    /**
     * Starts a Fetcher getting the recommendations form Mr. DLib
     */
    public void requestRecommendations() {
        try {
            mdlFetcher = new MrDLibFetcherWorker(selectedEntry);
            mdlFetcher.execute();
            mdlFetcher.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
                        try {
                            relatedArticles = mdlFetcher.get();
                            setHtmlText(relatedArticles);
                        } catch (InterruptedException | ExecutionException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }

                }
            });
        } catch (FetcherException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Helper Class to initiate SwingWorker
     */
    class MrDLibFetcherWorker extends SwingWorker<List<BibEntry>, Void> {


        private final MrDLibFetcher fetcher;
        BibEntry selectedEntry;

        public MrDLibFetcherWorker(BibEntry selectedEntry) throws Exception {
            this.selectedEntry = selectedEntry;
            fetcher = new MrDLibFetcher();
        }

        @Override
        protected List<BibEntry> doInBackground() throws Exception {
            return fetcher.performSearch(selectedEntry);
        }


    }


}
