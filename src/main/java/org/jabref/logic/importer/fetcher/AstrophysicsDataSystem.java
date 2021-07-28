package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.paging.Page;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

/**
 * Fetches data from the SAO/NASA Astrophysics Data System (https://ui.adsabs.harvard.edu/)
 */
public class AstrophysicsDataSystem implements IdBasedParserFetcher, PagedSearchBasedParserFetcher, EntryBasedParserFetcher {

    private static final String API_SEARCH_URL = "https://api.adsabs.harvard.edu/v1/search/query";
    private static final String API_EXPORT_URL = "https://api.adsabs.harvard.edu/v1/export/bibtexabs";

    private static final String API_KEY = new BuildInfo().astrophysicsDataSystemAPIKey;
    private final ImportFormatPreferences preferences;

    public AstrophysicsDataSystem(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * @param bibcodes collection of bibcodes for which a JSON object should be created
     */
    private static String buildPostData(Collection<String> bibcodes) {
        JSONObject obj = new JSONObject();
        obj.put("bibcode", bibcodes);
        return obj.toString();
    }

    /**
     * @return export URL endpoint
     */
    private static URL getURLforExport() throws URISyntaxException, MalformedURLException {
        return new URIBuilder(API_EXPORT_URL).build().toURL();
    }

    @Override
    public String getName() {
        return "SAO/NASA ADS";
    }

    /**
     * @param luceneQuery query string, matching the apache solr format
     * @return URL which points to a search request for given query
     */
    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder builder = new URIBuilder(API_SEARCH_URL);
        builder.addParameter("q", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        builder.addParameter("fl", "bibcode");
        builder.addParameter("rows", String.valueOf(getPageSize()));
        builder.addParameter("start", String.valueOf(getPageSize() * pageNumber));
        return builder.build().toURL();
    }

    /**
     * @param entry BibEntry for which a search URL is created
     * @return URL which points to a search request for given entry
     */
    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException {
        StringBuilder stringBuilder = new StringBuilder();

        Optional<String> title = entry.getFieldOrAlias(StandardField.TITLE).map(t -> "title:\"" + t + "\"");
        Optional<String> author = entry.getFieldOrAlias(StandardField.AUTHOR).map(a -> "author:\"" + a + "\"");

        if (title.isPresent()) {
            stringBuilder.append(title.get())
                         .append(author.map(s -> " AND " + s)
                                       .orElse(""));
        } else {
            stringBuilder.append(author.orElse(""));
        }
        String query = stringBuilder.toString().trim();

        URIBuilder builder = new URIBuilder(API_SEARCH_URL);
        builder.addParameter("q", query);
        builder.addParameter("fl", "bibcode");
        builder.addParameter("rows", "20");
        return builder.build().toURL();
    }

    /**
     * @param identifier bibcode or doi for which a search URL is created
     * @return URL which points to a search URL for given identifier
     */
    @Override
    public URL getUrlForIdentifier(String identifier) throws FetcherException, URISyntaxException, MalformedURLException {
        String query = "doi:\"" + identifier + "\" OR " + "bibcode:\"" + identifier + "\"";
        URIBuilder builder = new URIBuilder(API_SEARCH_URL);
        builder.addParameter("q", query);
        builder.addParameter("fl", "bibcode");
        return builder.build().toURL();
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
        new FieldFormatterCleanup(StandardField.ABSTRACT, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.ABSTRACT, new RemoveNewlinesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()).cleanup(entry);

        // Remove ADS note
        new FieldFormatterCleanup(new UnknownField("adsnote"), new ClearFormatter()).cleanup(entry);
        // Move adsurl to url field
        new MoveFieldCleanup(new UnknownField("adsurl"), StandardField.URL).cleanup(entry);
        entry.getField(StandardField.ABSTRACT)
             .filter(abstractText -> abstractText.equals("Not Available <P />"))
             .ifPresent(abstractText -> entry.clearField(StandardField.ABSTRACT));

        entry.getField(StandardField.ABSTRACT)
             .map(abstractText -> abstractText.replace("<P />", ""))
             .map(abstractText -> abstractText.replace("\\textbackslash", ""))
             .map(abstractText -> abstractText.trim())
             .ifPresent(abstractText -> entry.setField(StandardField.ABSTRACT, abstractText));
        // The fetcher adds some garbage (number of found entries etc before)
        entry.setCommentsBeforeEntry("");
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        if (entry.getFieldOrAlias(StandardField.TITLE).isEmpty() && entry.getFieldOrAlias(StandardField.AUTHOR).isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<String> bibcodes = fetchBibcodes(getURLForEntry(entry));
            return performSearchByIds(bibcodes);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        }
    }

    /**
     * @param url search ul for which bibcode will be returned
     * @return list of bibcodes matching the search request. May be empty
     */
    private List<String> fetchBibcodes(URL url) throws FetcherException {
        try {
            URLDownload download = getUrlDownload(url);
            String content = download.asString();
            JSONObject obj = new JSONObject(content);
            JSONArray codes = obj.getJSONObject("response").getJSONArray("docs");
            List<String> bibcodes = new ArrayList<>();
            for (int i = 0; i < codes.length(); i++) {
                bibcodes.add(codes.getJSONObject(i).getString("bibcode"));
            }
            return bibcodes;
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        try {
            List<String> bibcodes = fetchBibcodes(getUrlForIdentifier(identifier));
            List<BibEntry> fetchedEntries = performSearchByIds(bibcodes);

            if (fetchedEntries.isEmpty()) {
                return Optional.empty();
            }
            if (fetchedEntries.size() > 1) {
                LOGGER.info("Fetcher " + getName() + "found more than one result for identifier " + identifier
                        + ". We will use the first entry.");
            }
            BibEntry entry = fetchedEntries.get(0);
            return Optional.of(entry);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        }
    }

    /**
     * @param identifiers bibcodes for which bibentries ahould be fetched
     * @return list of bibentries matching the bibcodes. Can be empty and differ in size to the size of requested bibcodes
     */
    private List<BibEntry> performSearchByIds(Collection<String> identifiers) throws FetcherException {
        List<String> ids = identifiers.stream().filter(identifier -> !StringUtil.isBlank(identifier)).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String postData = buildPostData(ids);
            URLDownload download = new URLDownload(getURLforExport());
            download.addHeader("Authorization", "Bearer " + API_KEY);
            download.addHeader("ContentType", "application/json");
            download.setPostData(postData);
            String content = download.asString();
            JSONObject obj = new JSONObject(content);

            try {
                List<BibEntry> fetchedEntries = getParser().parseEntries(obj.optString("export"));
                if (fetchedEntries.isEmpty()) {
                    return Collections.emptyList();
                }
                // Post-cleanup
                fetchedEntries.forEach(this::doPostCleanup);

                return fetchedEntries;
            } catch (JSONException e) {
                return Collections.emptyList();
            }
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
    }

    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(luceneQuery);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        }
        List<String> bibCodes = fetchBibcodes(urlForQuery);
        List<BibEntry> results = performSearchByIds(bibCodes);
        return results;
    }

    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(luceneQuery, pageNumber);
        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("A network error occurred", e);
        }
        // This is currently just interpreting the complex query as a default string query
        List<String> bibCodes = fetchBibcodes(urlForQuery);
        Collection<BibEntry> results = performSearchByIds(bibCodes);
        return new Page<>(luceneQuery.toString(), pageNumber, results);
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        URLDownload urlDownload = new URLDownload(url);
        urlDownload.addHeader("Authorization", "Bearer " + API_KEY);
        return urlDownload;
    }

}
