package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.fetcher.BibsonomyScraper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * The current ScienceDirect fetcher implementation does no longer work
 *
 */
@Deprecated
public class ScienceDirectFetcher implements EntryFetcher {

    private static final String SCIENCE_DIRECT = "ScienceDirect";

    private static final Log LOGGER = LogFactory.getLog(ScienceDirectFetcher.class);

    private static final int MAX_PAGES_TO_LOAD = 8;
    private static final String WEBSITE_URL = "http://www.sciencedirect.com";
    private static final String SEARCH_URL = ScienceDirectFetcher.WEBSITE_URL + "/science/quicksearch?query=";

    private static final String LINK_PREFIX = "http://www.sciencedirect.com/science?_ob=ArticleURL&";
    private static final Pattern LINK_PATTERN = Pattern
            .compile("<a href=\"" + ScienceDirectFetcher.LINK_PREFIX.replaceAll("\\?", "\\\\?") + "([^\"]+)\"\"");

    private boolean stopFetching;


    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_SCIENCEDIRECT;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No Options panel
        return null;
    }

    @Override
    public String getTitle() {
        return "ScienceDirect";
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            if (citations == null) {
                return false;
            }
            if (citations.isEmpty()) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'",
                        query),
                        Localization.lang("Search %0", SCIENCE_DIRECT), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            int i = 0;
            for (String cit : citations) {
                if (stopFetching) {
                    break;
                }
                BibsonomyScraper.getEntry(cit, Globals.prefs.getImportFormatPreferences())
                        .ifPresent(dialog::addEntry);
                dialog.setProgress(++i, citations.size());
            }

            return true;

        } catch (IOException e) {
            LOGGER.warn("Communcation problems", e);
            ((ImportInspectionDialog)dialog).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param query
     *            The search term to query JStor for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private static List<String> getCitations(String query) throws IOException {
        String urlQuery;
        List<String> ids = new ArrayList<>();
        urlQuery = ScienceDirectFetcher.SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        int count = 1;
        String nextPage;
        while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                && (count < ScienceDirectFetcher.MAX_PAGES_TO_LOAD)) {
            urlQuery = nextPage;
            count++;
        }
        return ids;
    }

    private static String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        String cont = new URLDownload(urlQuery).downloadToString(Globals.prefs.getDefaultEncoding());
        Matcher m = ScienceDirectFetcher.LINK_PATTERN.matcher(cont);
        if (m.find()) {
            while (m.find()) {
                ids.add(ScienceDirectFetcher.LINK_PREFIX + m.group(1));
                cont = cont.substring(m.end());
                m = ScienceDirectFetcher.LINK_PATTERN.matcher(cont);
            }
        }

        else {
            return null;
        }
        return null;
    }

}
