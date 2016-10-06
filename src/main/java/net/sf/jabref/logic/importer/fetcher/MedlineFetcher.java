package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetch or search from Pubmed http://www.ncbi.nlm.nih.gov/sites/entrez/
 */
public class MedlineFetcher implements IdBasedParserFetcher, SearchBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(MedlineFetcher.class);

    private static final Pattern ID_PATTERN = Pattern.compile("<Id>(\\d+)</Id>");
    private static final Pattern COUNT_PATTERN = Pattern.compile("<Count>(\\d+)<\\/Count>");
    private static final Pattern RET_MAX_PATTERN = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
    private static final Pattern RET_START_PATTERN = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");

    private int count;
    private int retmax;
    private int retstart;

    /**
     * Removes all comma's in a given String
     *
     * @param query input to remove comma's
     * @return input without comma's
     */
    private static String replaceCommaWithAND(String query) {
        return query.replaceAll(", ", " AND ").replaceAll(",", " AND ");
    }

    /**
     * When using 'esearch.fcgi?db=<database>&term=<query>' we will get a list of ID's matching the query.
     * Input: Any text query (&term)
     * Output: List of UIDs matching the query
     *
     * @see <a href="https://www.ncbi.nlm.nih.gov/books/NBK25500/">www.ncbi.nlm.nih.gov/books/NBK25500/</a>
     */
    private List<String> getPubMedIdsFromQuery(String query, int start, int pacing) throws FetcherException {
        boolean doCount = true;
        List<String> idList = new ArrayList<>();
        try {
            URL ncbi = createSearchUrl(query, start, pacing);
            BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
            String inLine;
            while ((inLine = in.readLine()) != null) {

                Matcher idMatcher = ID_PATTERN.matcher(inLine);
                if (idMatcher.find()) {
                    idList.add(idMatcher.group(1));
                }
                Matcher retMaxMatcher = RET_MAX_PATTERN.matcher(inLine);
                if (retMaxMatcher.find()) {
                    retmax = Integer.parseInt(retMaxMatcher.group(1));
                }
                Matcher retStartMatcher = RET_START_PATTERN.matcher(inLine);
                if (retStartMatcher.find()) {
                    retstart = Integer.parseInt(retStartMatcher.group(1));
                }
                Matcher countMatcher = COUNT_PATTERN.matcher(inLine);
                if (doCount && countMatcher.find()) {
                    count = Integer.parseInt(countMatcher.group(1));
                    doCount = false;
                }
            }
            return idList;
        } catch (IOException | URISyntaxException e) {
            throw new FetcherException("Unable to get PubMed ID's.", e);
        }
    }

    @Override
    public String getName() {
        return "Medline";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_MEDLINE;
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi");
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("retmode", "xml");
        uriBuilder.addParameter("id", identifier);
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new MedlineImporter();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        entry.clearField("journal-abbreviation");
        entry.clearField("status");
        entry.clearField("copyright");
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        final int NUMBER_TO_FETCH = 50;
        List<BibEntry> entryList = new LinkedList<>();

        if (query.isEmpty()) {
            return Collections.emptyList();
        } else {
            String searchTerm = replaceCommaWithAND(query);

            //searching for pubmed id's matching the query
            List<String> idList = getPubMedIdsFromQuery(searchTerm, 0, NUMBER_TO_FETCH);

            if (count == 0) {
                LOGGER.info("No references found");
            }

            //pass the id list to fetchMedline to download them. like a id fetcher for mutliple id's
            List<BibEntry> bibs = fetchMedline(idList);

            entryList.addAll(bibs);

            return entryList;
        }
    }

    private URL createSearchUrl(String term, int start, int pacing) throws URISyntaxException, MalformedURLException {
        term = replaceCommaWithAND(term);
        URIBuilder uriBuilder = new URIBuilder("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi");
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("retmax", Integer.toString(pacing));
        uriBuilder.addParameter("retstart", Integer.toString(start));
        uriBuilder.addParameter("sort", "relevance");
        uriBuilder.addParameter("term", term);
        return uriBuilder.build().toURL();
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     * The E-utilities generate a huge XML file containing all entries for the id's
     *
     * @param ids A list of ID's to search for.
     * @return Will return an empty list on error.
     */
    private List<BibEntry> fetchMedline(List<String> ids) throws FetcherException {
        try {
            //Separate the ID's with a comma to search multiple entries
            URL fetchURL = getURLForID(String.join(",", ids));
            URLConnection data = fetchURL.openConnection();
            ParserResult result = new MedlineImporter().importDatabase(
                    new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8)));
            if (result.hasWarnings()) {
                LOGGER.warn(result.getErrorMessage());
            }
            List<BibEntry> resultList = result.getDatabase().getEntries();
            resultList.forEach(entry -> doPostCleanup(entry));
            return resultList;
        } catch (URISyntaxException | IOException e) {
            throw new FetcherException(e.getLocalizedMessage(), e);
        }
    }

}
