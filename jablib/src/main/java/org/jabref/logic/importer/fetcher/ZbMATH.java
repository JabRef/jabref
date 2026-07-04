package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.ZbMathQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;

/// Fetches data from the Zentralblatt Math (https://www.zbmath.org/)
@NullMarked
public class ZbMATH implements SearchBasedParserFetcher, IdBasedParserFetcher, EntryBasedParserFetcher {

    private static final String CITATION_MATCHING_URL = "https://zbmath.org/citationmatching/match";
    private static final String BIBTEX_OUTPUT_URL = "https://zbmath.org/bibtexoutput/";

    private final ImportFormatPreferences preferences;
    private final String citationMatchingUrl;
    private final String bibtexOutputUrl;

    public ZbMATH(ImportFormatPreferences preferences) {
        this(preferences, CITATION_MATCHING_URL, BIBTEX_OUTPUT_URL);
    }

    ZbMATH(ImportFormatPreferences preferences, String citationMatchingUrl, String bibtexOutputUrl) {
        this.preferences = preferences;
        this.citationMatchingUrl = citationMatchingUrl;
        this.bibtexOutputUrl = bibtexOutputUrl;
    }

    @Override
    public String getName() {
        return "zbMATH";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ZBMATH);
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        Optional<String> zblidInEntry = entry.getField(StandardField.ZBL_NUMBER);
        if (zblidInEntry.isPresent()) {
            // zbmath id is already present
            return getUrlForIdentifier(zblidInEntry.get());
        }

        URIBuilder uriBuilder = new URIBuilder(citationMatchingUrl);
        uriBuilder.addParameter("n", "1"); // return only the best matching entry
        uriBuilder.addParameter("m", "5"); // return only entries with a score of at least 5

        entry.getFieldOrAlias(StandardField.TITLE).ifPresent(title -> uriBuilder.addParameter("t", title));
        entry.getFieldOrAlias(StandardField.JOURNAL).ifPresent(journal -> uriBuilder.addParameter("j", journal));
        entry.getFieldOrAlias(StandardField.YEAR).ifPresent(year -> uriBuilder.addParameter("y", year));
        entry.getFieldOrAlias(StandardField.PAGINATION)
             .ifPresent(pagination -> uriBuilder.addParameter("p", pagination));
        entry.getFieldOrAlias(StandardField.VOLUME).ifPresent(volume -> uriBuilder.addParameter("v", volume));
        entry.getFieldOrAlias(StandardField.ISSUE).ifPresent(issue -> uriBuilder.addParameter("i", issue));

        if (entry.getFieldOrAlias(StandardField.AUTHOR).isPresent()) {
            // replace "and" by ";" as citation matching API uses ";" for separation
            AuthorList authors = AuthorList.parse(entry.getFieldOrAlias(StandardField.AUTHOR).get());
            String authorsWithSemicolon = authors.getAuthors().stream()
                                                 .map(author -> author.getFamilyGiven(false))
                                                 .collect(Collectors.joining(";"));
            uriBuilder.addParameter("a", authorsWithSemicolon);
        }

        /*
        zbmath citation matching API does only return json, thus we use the
        citation matching API to extract the zbl_id and then use getUrlForIdentifier
        to get the bibtex data.
         */
        JSONObject root = getCitationMatchingResponse(uriBuilder.build().toURL());
        JSONArray results = root.optJSONArray("results");
        if (results == null) {
            throw new FetcherException("Missing 'results' field in zbMATH response");
        }

        if (results.isEmpty()) {
            // citation matching API found no matching entry
            throw new ZbMathNoUrlException("No matching entry found in zbMATH");
        }

        JSONObject bestResult = results.optJSONObject(0);
        if (bestResult == null) {
            throw new FetcherException("Invalid result in zbMATH response");
        }

        String zblid = bestResult.optString("zbl_id");
        if (StringUtil.isBlank(zblid)) {
            throw new FetcherException("Missing 'zbl_id' field in zbMATH response");
        }

        return getUrlForIdentifier(zblid);
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(bibtexOutputUrl);
        uriBuilder.addParameter("q", new ZbMathQueryTransformer().transformSearchQuery(queryNode).orElse("")); // search all fields
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "200"); // should return up to 200 items (instead of default 100)
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(bibtexOutputUrl);
        String query = "an:".concat(identifier); // use an: to search for a zbMATH identifier
        uriBuilder.addParameter("q", query);
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "1"); // return exactly one item
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup(new UnknownField("msc2010"), StandardField.KEYWORDS).cleanup(entry);
        new MoveFieldCleanup(AMSField.FJOURNAL, StandardField.JOURNAL).cleanup(entry);
        new FieldFormatterCleanup(StandardField.JOURNAL, new RemoveEnclosingBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveEnclosingBracesFormatter()).cleanup(entry);
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        try {
            return EntryBasedParserFetcher.super.performSearch(entry);
        } catch (ZbMathNoUrlException e) {
            return List.of();
        }
    }

    public static class ZbMathNoUrlException extends FetcherException {
        public ZbMathNoUrlException(String errorMessage) {
            super(errorMessage);
        }
    }

    private JSONObject getCitationMatchingResponse(URL url) throws FetcherException {
        try (InputStream stream = getUrlDownload(url).asInputStream()) {
            return JsonReader.toJsonObject(stream);
        } catch (ParseException e) {
            throw new FetcherException("Invalid JSON response from zbMATH", e);
        } catch (FetcherException e) {
            throw new FetcherException("Error response from zbMATH", e);
        } catch (java.io.IOException e) {
            throw new FetcherException("Could not read response from zbMATH", e);
        }
    }
}
