package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CiteSeerXFetcher implements EntryFetcher {

    private static final int MAX_PAGES_TO_LOAD = 8;
    private static final String QUERY_MARKER = "___QUERY___";
    private static final String URL_START = "http://citeseer.ist.psu.edu";
    private static final String SEARCH_URL = CiteSeerXFetcher.URL_START + "/search?q=" + CiteSeerXFetcher.QUERY_MARKER
            + "&submit=Search&sort=rlv&t=doc";
    private static final Pattern CITE_LINK_PATTERN = Pattern.compile("<a class=\"remove doc_details\" href=\"(.*)\">");

    private boolean stopFetching;

    private static final String BASE_PATTERN = "<meta name=\"" + CiteSeerXFetcher.QUERY_MARKER
            + "\" content=\"(.*)\" />";
    private static final Pattern TITLE_PATTERN = Pattern
            .compile(CiteSeerXFetcher.BASE_PATTERN.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_title"));
    private static final Pattern AUTHOR_PATTERN = Pattern
            .compile(CiteSeerXFetcher.BASE_PATTERN.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_authors"));
    private static final Pattern YEAR_PATTERN = Pattern
            .compile(CiteSeerXFetcher.BASE_PATTERN.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_year"));
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("<h3>Abstract</h3>\\s*<p>(.*)</p>");

    private static final Log LOGGER = LogFactory.getLog(CiteSeerXFetcher.class);

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            for (String citation : citations) {
                if (stopFetching) {
                    break;
                }
                BibEntry entry = getSingleCitation(citation);
                if (entry != null) {
                    inspector.addEntry(entry);
                }
            }

            return true;
        } catch (IOException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)inspector).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "CiteSeerX";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_CITESEERX;
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }

    /**
     *
     * @param query
     *            The search term to query JStor for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private List<String> getCitations(String query) throws IOException {
        String urlQuery;
        List<String> ids = new ArrayList<>();
        urlQuery = CiteSeerXFetcher.SEARCH_URL.replace(CiteSeerXFetcher.QUERY_MARKER,
                URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
        int count = 1;
        String nextPage;
        while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                && (count < CiteSeerXFetcher.MAX_PAGES_TO_LOAD)) {
            urlQuery = nextPage;
            count++;
            if (stopFetching) {
                break;
            }
        }
        return ids;
    }

    private static String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        String cont = new URLDownload(urlQuery).downloadToString(Globals.prefs.getDefaultEncoding());
        Matcher m = CiteSeerXFetcher.CITE_LINK_PATTERN.matcher(cont);
        while (m.find()) {
            ids.add(CiteSeerXFetcher.URL_START + m.group(1));
        }

        return null;
    }



    private static BibEntry getSingleCitation(String urlString) throws IOException {
        String cont = new URLDownload(urlString).downloadToString(StandardCharsets.UTF_8);

        // Find title, and create entry if we do. Otherwise assume we did not get an entry:
        Matcher m = CiteSeerXFetcher.TITLE_PATTERN.matcher(cont);
        if (m.find()) {
            BibEntry entry = new BibEntry(IdGenerator.next());
            entry.setField(FieldName.TITLE, m.group(1));

            // Find authors:
            m = CiteSeerXFetcher.AUTHOR_PATTERN.matcher(cont);
            if (m.find()) {
                String authors = m.group(1);
                entry.setField(FieldName.AUTHOR, new NormalizeNamesFormatter().format(authors));
            }

            // Find year:
            m = CiteSeerXFetcher.YEAR_PATTERN.matcher(cont);
            if (m.find()) {
                entry.setField(FieldName.YEAR, m.group(1));
            }

            // Find abstract:
            m = CiteSeerXFetcher.ABSTRACT_PATTERN.matcher(cont);
            if (m.find()) {
                entry.setField(FieldName.ABSTRACT, m.group(1));
            }

            return entry;
        } else {
            return null;
        }

    }

}
