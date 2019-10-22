package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

//TODO Replace Old ADS after the new one is mature

/**
 * Fetches data from the SAO/NASA Astrophysics Data System (http://www.adsabs.harvard.edu/)
 * <p>
 * Search query-based: http://adsabs.harvard.edu/basic_search.html Entry -based: http://adsabs.harvard.edu/abstract_service.html
 * <p>
 * There is also a new API (https://github.com/adsabs/adsabs-dev-api) but it returns JSON (or at least needs multiple
 * calls to get BibTeX, status: September 2016)
 */
public class NewAstrophysicsDataSystem implements IdBasedParserFetcher, SearchBasedParserFetcher, EntryBasedParserFetcher {

    private static String API_SEARCH_URL = "https://api.adsabs.harvard.edu/v1/search/query";
    private static String API_EXPORT_URL = "https://api.adsabs.harvard.edu/v1/export/bibtexabs";

    private static String API_KEY = ""; //TODO Add API Token

    private final ImportFormatPreferences preferences;

    public NewAstrophysicsDataSystem(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    private String buildPostData(String... bibcodes) {
        JSONObject obj = new JSONObject();
        obj.put("bibcode", bibcodes);
        return obj.toString();
    }

    @Override
    public String getName() {
        return "New SAO/NASA Astrophysics Data System";
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder builder = new URIBuilder(API_SEARCH_URL);
        builder.addParameter("q", query);
        builder.addParameter("fl", "bibcode");
        return builder.build().toURL();
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        StringBuilder stringBuilder = new StringBuilder();

        Optional<String> title = entry.getFieldOrAlias(StandardField.TITLE).map(t -> "title:" + t);
        Optional<String> author = entry.getFieldOrAlias(StandardField.TITLE).map(a -> "author" + a);

        if (title.isPresent()) {
            stringBuilder.append(title.get())
                         .append(author.map(s -> " OR " + s)
                                       .orElse(""));
        } else {
            stringBuilder.append(author.orElse(""));
        }
        String query = stringBuilder.toString().trim();

        URIBuilder builder = new URIBuilder(API_SEARCH_URL);
        builder.addParameter("q", query);
        builder.addParameter("fl", "bibcode");
        return builder.build().toURL();
    }

    @Override
    public URL getURLForID(String identifier) throws FetcherException, URISyntaxException, MalformedURLException {
        return new URIBuilder(API_EXPORT_URL).build().toURL();
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ADS);
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences, new DummyFileUpdateMonitor());
    }

    @Override
    public void doPostCleanup(BibEntry entry) {

    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        return Collections.emptyList();
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {

        Optional<List<BibEntry>> results = performSearchByIds(identifier);
        if (results.isEmpty()) {
            return Optional.empty();
        }

        List<BibEntry> fetchedEntries = results.get();

        if (fetchedEntries.size() > 1) {
            LOGGER.info("Fetcher " + getName() + "found more than one result for identifier " + identifier
                    + ". We will use the first entry.");
        }

        BibEntry entry = fetchedEntries.get(0);
        return Optional.of(entry);
    }

    private Optional<List<BibEntry>> performSearchByIds(String... identifiers) throws FetcherException {

        long idCount = Arrays.stream(identifiers).filter(identifier -> !StringUtil.isBlank(identifier)).count();
        if (idCount == 0) {
            return Optional.empty();
        }

        try {

            String postData = buildPostData(identifiers);
            URLDownload download = new URLDownload(getURLForID(""));
            download.addHeader("Authorization", "Bearer " + API_KEY);
            download.addHeader("ContentType", "application/json");
            download.setPostData(postData);
            download.asString();
            String content = download.asString();
            JSONObject obj = new JSONObject(content);

            List<BibEntry> fetchedEntries = getParser().parseEntries(obj.getString("export"));

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }
            // Post-cleanup
            fetchedEntries.forEach(this::doPostCleanup);

            return Optional.of(fetchedEntries);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {

        return Collections.emptyList();
    }
}
