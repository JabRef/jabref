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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetch or search from Pubmed http://www.ncbi.nlm.nih.gov/sites/entrez/
 */
public class MedlineFetcher implements IdBasedFetcher, SearchBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(MedlineFetcher.class);

    private static final String API_MEDLINE_FETCH = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String API_MEDLINE_SEARCH = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    private static final Pattern ID_PATTERN = Pattern.compile("<Id>(\\d+)</Id>");
    private static final Pattern COUNT_PATTERN = Pattern.compile("<Count>(\\d+)<\\/Count>");
    private static final Pattern RET_MAX_PATTERN = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
    private static final Pattern RET_START_PATTERN = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");

    private int count;
    private int retmax;
    private int retstart;
    private String ids = "";

    private void addID(String id) {
        if (ids.isEmpty()) {
            ids = id;
        } else {
            ids += "," + id;
        }
    }

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
     * Gets the initial list of ids
     */
    private void getIds(String term, int start, int pacing) {
        boolean doCount = true;
        try {
            URL ncbi = createSearchUrl(term, start, pacing);
            // get the ids
            BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
            String inLine;
            while ((inLine = in.readLine()) != null) {

                // get the count
                Matcher idMatcher = ID_PATTERN.matcher(inLine);
                if (idMatcher.find()) {
                    addID(idMatcher.group(1));
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
        } catch (URISyntaxException e) {
            LOGGER.warn("Bad url", e);
        } catch (IOException e) {
            LOGGER.warn("Connection failed", e);
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
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String cleanQuery = identifier.trim().replace(';', ',');

        List<BibEntry> entry = fetchMedline(cleanQuery);

        if (entry.isEmpty()) {
            LOGGER.warn(Localization.lang("No references found"));
        }
        return Optional.of(entry.get(0));
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        final int NUMBER_TO_FETCH = 50;
        List<BibEntry> entryList = new LinkedList<>();

        if (!query.isEmpty()) {
            String searchTerm = replaceCommaWithAND(query);

            getIds(searchTerm, 0, 1);

            if (count == 0) {
                LOGGER.warn(Localization.lang("No references found"));
                return Collections.emptyList();
            }

            getIds(searchTerm, 0, NUMBER_TO_FETCH - 1);

            List<BibEntry> bibs = fetchMedline(ids);
            entryList.addAll(bibs);

            return entryList;
        }
        throw new FetcherException(Localization.lang("Input error") + ". " + Localization.lang("Please enter a comma separated list of Medline IDs (numbers) or search terms."));
    }

    static URL createSearchUrl(String term, int start, int pacing) throws URISyntaxException, MalformedURLException {
        term = replaceCommaWithAND(term);
        URIBuilder uriBuilder = new URIBuilder(API_MEDLINE_SEARCH);
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("retmax", Integer.toString(pacing));
        uriBuilder.addParameter("retstart", Integer.toString(start));
        uriBuilder.addParameter("sort", "relevance");
        uriBuilder.addParameter("term", term);
        return uriBuilder.build().toURL();
    }

    /**
     * @see <a href="https://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/?report=objectonly">www.ncbi.nlm.nih.gov</a>
     */
    static URL createFetchUrl(String id) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(API_MEDLINE_FETCH);
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("retmode", "xml");
        uriBuilder.addParameter("id", id);
        return uriBuilder.build().toURL();
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     *
     * @param id One or several ids, separated by ","
     * @return Will return an empty list on error.
     */
    private static List<BibEntry> fetchMedline(String id) {
        try {
            URL fetchURL = createFetchUrl(id);
            URLConnection data = fetchURL.openConnection();
            ParserResult result = new MedlineImporter().importDatabase(
                    new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8)));
            if (result.hasWarnings()) {
                LOGGER.warn(result.getErrorMessage());
            }
            return result.getDatabase().getEntries();
        } catch (URISyntaxException | IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            return new ArrayList<>();
        }
    }
}
