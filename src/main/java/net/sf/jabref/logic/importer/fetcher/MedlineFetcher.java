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
import net.sf.jabref.model.entry.FieldName;

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

    private static final Pattern PART1_PATTERN = Pattern.compile(", ");
    private static final Pattern PART2_PATTERN = Pattern.compile(",");

    private static final Pattern ID_PATTERN = Pattern.compile("<Id>(\\d+)</Id>");
    private static final Pattern COUNT_PATTERN = Pattern.compile("<Count>(\\d+)<\\/Count>");
    private static final Pattern RET_MAX_PATTERN = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
    private static final Pattern RET_START_PATTERN = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");


    /**
     * How many entries to query in one request
     */
    private static final int PACING = 20;

    private static String toSearchTerm(String in) {
        // This can probably be simplified using simple String.replace()...
        String result = in;
        Matcher matcher;
        matcher = PART1_PATTERN.matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        matcher = PART2_PATTERN.matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        result = result.replace(" ", "+");
        return result;
    }

    /**
     * Gets the initial list of ids
     */
    private SearchResult getIds(String term, int start, int pacing) {
        boolean doCount = true;
        SearchResult result = new SearchResult();
        try {
            URIBuilder uriBuilder = new URIBuilder(API_MEDLINE_SEARCH);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("retmax", Integer.toString(pacing));
            uriBuilder.addParameter("retstart", Integer.toString(start));
            uriBuilder.addParameter("term", term);
            URL ncbi = uriBuilder.build().toURL();
            // get the ids
            BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
            String inLine;
            while ((inLine = in.readLine()) != null) {

                // get the count
                Matcher idMatcher = ID_PATTERN.matcher(inLine);
                if (idMatcher.find()) {
                    result.addID(idMatcher.group(1));
                }
                Matcher retMaxMatcher = RET_MAX_PATTERN.matcher(inLine);
                if (retMaxMatcher.find()) {
                    result.retmax = Integer.parseInt(retMaxMatcher.group(1));
                }
                Matcher retStartMatcher = RET_START_PATTERN.matcher(inLine);
                if (retStartMatcher.find()) {
                    result.retstart = Integer.parseInt(retStartMatcher.group(1));
                }
                Matcher countMatcher = COUNT_PATTERN.matcher(inLine);
                if (doCount && countMatcher.find()) {
                    result.count = Integer.parseInt(countMatcher.group(1));
                    doCount = false;
                }
            }
        } catch (URISyntaxException | MalformedURLException e) { // new URL() failed
            LOGGER.warn("Bad url", e);
        } catch (IOException e) { // openConnection() failed
            LOGGER.warn("Connection failed", e);
        }
        return result;
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
    public List<BibEntry> performSearch(String query) throws FetcherException {
        List<BibEntry> entryList = new LinkedList<>();

        if (!query.isEmpty()) {
            String searchTerm = toSearchTerm(query);

            // get the ids from entrez
            SearchResult result = getIds(searchTerm, 0, 1);

            if (result.count == 0) {
                LOGGER.warn(Localization.lang("No references found"));
                return Collections.emptyList();
            }

            int numberToFetch = result.count;

            for (int i = 0; i < numberToFetch; i += MedlineFetcher.PACING) {

                int noToFetch = Math.min(MedlineFetcher.PACING, numberToFetch - i);

                // get the ids from entrez
                result = getIds(searchTerm, i, noToFetch);

                List<BibEntry> bibs = fetchMedline(result.ids);
                for (BibEntry entry : bibs) {
                    entryList.add(entry);
                }
            }
            return entryList;
        }
        throw new FetcherException("Input Error. Please enter a comma separated list of Medline IDs (numbers) or search terms.");
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     *
     * @param id One or several ids, separated by ","
     * @return Will return an empty list on error.
     */
    private static List<BibEntry> fetchMedline(String id) {
        try {
            URIBuilder uriBuilder = new URIBuilder(API_MEDLINE_FETCH);
            uriBuilder.addParameter("db", "pubmed");
            uriBuilder.addParameter("retmode", "xml");
            uriBuilder.addParameter("rettype", "citation");
            uriBuilder.addParameter("id", id);
            URL fetchURL = uriBuilder.build().toURL();
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

    private BibEntry doPostCleanUp(BibEntry entry) {
        Optional<String> bibEntryOptional = entry.getField(FieldName.MONTH);
        //TODO: Month Checker
        return entry;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String cleanQuery = identifier.trim().replace(';', ',');

        if (cleanQuery.matches("\\d+[,\\d+]*")) {
            List<BibEntry> bibs = fetchMedline(cleanQuery);

            if (bibs.isEmpty()) {
                LOGGER.warn(Localization.lang("No references found"));
            }

            return Optional.of(bibs.get(0));
        }
        return Optional.empty();
    }

    static class SearchResult {

        public int count;

        public int retmax;

        public int retstart;

        public String ids = "";


        public void addID(String id) {
            if (ids.isEmpty()) {
                ids = id;
            } else {
                ids += "," + id;
            }
        }
    }

}