/**
 *
 */
package net.sf.jabref.gui.entryeditor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.MrDLibFetcher;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Tihs Component is displaying the recommendations received from Mr. DLib.
 *
 */
public class EntryEditorTabRelatedArticles extends JEditorPane {

    //The entry the user selects in the table
    private final BibEntry selectedEntry;

    //The Fetcher delivers the recommendatinos
    private MrDLibFetcher mdlFetcher;

    private static final String RECOMMENDATION_SEPERATOR = "<br>";


    /**
     * Takes the selected entry, runs a request to Mr. DLib and returns the recommendations as a JEditorPane
     * @param selectedEntry
     */
    public EntryEditorTabRelatedArticles(BibEntry selectedEntry) {
        this.selectedEntry = selectedEntry;
        this.setContentType("text/html");
        this.setEditable(false);
        registerHyperlinkListener();
        setDefaultContent();
        requestRecommendations(selectedEntry);
    }

    /**
     * Takes a List of html snippets and sets it in the JEditorPane
     * @param list
     */
    public void setHtmlText(List<String> list) {
        StringBuffer htmlContent = new StringBuffer();
        htmlContent.append("<html><head><title></title></head><body bgcolor='#ffffff'><font size=8>");
        for (String snippet : list) {
            if (snippet != null) {
                htmlContent.append(snippet);
                htmlContent.append(RECOMMENDATION_SEPERATOR);
            }
        }
        // loading animation
        //        htmlContent.append("<img width=\"100\" height=\"100\" src=\"" + url + "\"></img>");
        htmlContent.append("</font></body></html>");
        this.setText(htmlContent.toString());
    }

    /**
     * While the fetcher gets the Recommendations from MR. DLib this content indicates
     * that the search is in Progress
     */
    private void setDefaultContent() {
        StringBuffer htmlContent = new StringBuffer();

        //todo
        //What is the best way to include that gif?
        //AuxCommandLineTest.class.getResource("paper.aux");
        //File f = Paths.get(url.toUri()).toFile();
        //concrete example
        //File auxFile = Paths.get(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toFile();
        URL url = getClass().getResource("loading_animation.gif");
        htmlContent.append("<html><head><title></title></head><body bgcolor='#ffffff'><font size=8>");
        htmlContent.append("Loading Recommendations for ");
        htmlContent.append(formatTitleFromBibEntry(selectedEntry));
        //htmlContent.append("<img width=\"100\" height=\"100\" src=\"" + url + "\"></img>");
        htmlContent.append("</font></body></html>");
        this.setText(htmlContent.toString());
    }

    /**
     * Makes the Hyperlinks clickable. Opens the link destination in a Browsertab
     */
    private void registerHyperlinkListener() {
        this.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        new JabRefDesktop().openBrowser(e.getURL().toString());
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Extracts the title of the Bibentry. Isn't there a Method available?
     * @param selectedEntry
     * @return
     */
    private String formatTitleFromBibEntry(BibEntry selectedEntry) {
        return selectedEntry.getField("title").toString().replaceAll("\\{|\\[|\\]|\\}|(Optional)", "");
    }

    /**
     * Starts a Fetcher getting the recommendations form Mr. DLib
     * @param selectedEntry
     */
    private void requestRecommendations(BibEntry selectedEntry) {
        try {
            mdlFetcher = new MrDLibFetcher(selectedEntry);
            mdlFetcher.execute();
            mdlFetcher.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {

                        setHtmlText(mdlFetcher.getRecommendationsAsHTML());

                    }

                }
            });
        } catch (FetcherException e) {
            //some logging?
        } catch (Exception e) {
            //some logging?
        }
    }


}
