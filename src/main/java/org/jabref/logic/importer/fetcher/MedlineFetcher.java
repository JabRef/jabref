package org.jabref.logic.importer.fetcher;

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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch or search from PubMed <a href="http://www.ncbi.nlm.nih.gov/sites/entrez/">www.ncbi.nlm.nih.gov</a>
 * The MedlineFetcher fetches the entries from the PubMed database.
 * See <a href="http://help.jabref.org/en/MedlineRIS">help.jabref.org</a> for a detailed documentation of the available fields.
 */
public class MedlineFetcher implements IdBasedParserFetcher, SearchBasedFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MedlineFetcher.class);

    private static final int NUMBER_TO_FETCH = 50;
    private static final String ID_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String SEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    private int numberOfResultsFound;


    /**
     * Replaces all commas in a given string with " AND "
     *
     * @param query input to remove commas
     * @return input without commas
     */
    private static String replaceCommaWithAND(String query) {
        return query.replaceAll(", ", " AND ").replaceAll(",", " AND ");
    }

    /**
     * When using 'esearch.fcgi?db=<database>&term=<query>' we will get a list of IDs matching the query.
     * Input: Any text query (&term)
     * Output: List of UIDs matching the query
     *
     * @see <a href="https://www.ncbi.nlm.nih.gov/books/NBK25500/">www.ncbi.nlm.nih.gov/books/NBK25500/</a>
     */
    private List<String> getPubMedIdsFromQuery(String query) throws FetcherException {
        boolean fetchIDs = false;
        boolean firstOccurrenceOfCount = false;
        List<String> idList = new ArrayList<>();
        try {
            URL ncbi = createSearchUrl(query);

            XMLInputFactory inputFactory = XMLInputFactory.newFactory();
            XMLStreamReader streamReader = inputFactory.createXMLStreamReader(ncbi.openStream());

            fetchLoop: while (streamReader.hasNext()) {
                int event = streamReader.getEventType();

                switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    if (streamReader.getName().toString().equals("Count")) {
                        firstOccurrenceOfCount = true;
                    }

                    if (streamReader.getName().toString().equals("IdList")) {
                        fetchIDs = true;
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (firstOccurrenceOfCount) {
                        numberOfResultsFound = Integer.parseInt(streamReader.getText());
                        firstOccurrenceOfCount = false;
                    }

                    if (fetchIDs) {
                        idList.add(streamReader.getText());
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    //Everything relevant is listed before the IdList. So we break the loop right after the IdList tag closes.
                    if (streamReader.getName().toString().equals("IdList")) {
                        break fetchLoop;
                    }
                }
                streamReader.next();
            }
            streamReader.close();
            return idList;
        } catch (IOException | URISyntaxException e) {
            throw new FetcherException("Unable to get PubMed IDs", Localization.lang("Unable to get PubMed IDs"), e);
        } catch (XMLStreamException e) {
            throw new FetcherException("Error while parsing ID list", Localization.lang("Error while parsing ID list"),
                    e);
        }
    }

    @Override
    public String getName() {
        return "Medline/PubMed";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_MEDLINE;
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(ID_URL);
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
        new FieldFormatterCleanup("journal-abbreviation", new ClearFormatter()).cleanup(entry);
        new FieldFormatterCleanup("status", new ClearFormatter()).cleanup(entry);
        new FieldFormatterCleanup("copyright", new ClearFormatter()).cleanup(entry);

        new FieldFormatterCleanup(FieldName.MONTH, new NormalizeMonthFormatter()).cleanup(entry);
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        List<BibEntry> entryList = new LinkedList<>();

        if (query.isEmpty()) {
            return Collections.emptyList();
        } else {
            String searchTerm = replaceCommaWithAND(query);

            //searching for pubmed ids matching the query
            List<String> idList = getPubMedIdsFromQuery(searchTerm);

            if (idList.isEmpty()) {
                LOGGER.info("No results found.");
                return Collections.emptyList();
            }
            if (numberOfResultsFound > NUMBER_TO_FETCH) {
                LOGGER.info(
                        numberOfResultsFound + " results found. Only 50 relevant results will be fetched by default.");
            }

            //pass the list of ids to fetchMedline to download them. like a id fetcher for mutliple ids
            entryList = fetchMedline(idList);

            return entryList;
        }
    }

    private URL createSearchUrl(String term) throws URISyntaxException, MalformedURLException {
        term = replaceCommaWithAND(term);
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("sort", "relevance");
        uriBuilder.addParameter("retmax", String.valueOf(NUMBER_TO_FETCH));
        uriBuilder.addParameter("term", term);
        return uriBuilder.build().toURL();
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     * The E-utilities generate a huge XML file containing all entries for the ids
     *
     * @param ids A list of IDs to search for.
     * @return Will return an empty list on error.
     */
    private List<BibEntry> fetchMedline(List<String> ids) throws FetcherException {
        try {
            //Separate the IDs with a comma to search multiple entries
            URL fetchURL = getURLForID(String.join(",", ids));
            URLConnection data = fetchURL.openConnection();
            ParserResult result = new MedlineImporter().importDatabase(
                    new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8)));
            if (result.hasWarnings()) {
                LOGGER.warn(result.getErrorMessage());
            }
            List<BibEntry> resultList = result.getDatabase().getEntries();
            resultList.forEach(this::doPostCleanup);
            return resultList;
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Error while generating fetch URL",
                    Localization.lang("Error while generating fetch URL"), e);
        } catch (IOException e) {
            throw new FetcherException("Error while fetching from Medline",
                    Localization.lang("Error while fetching from %0", "Medline"), e);
        }
    }

}
