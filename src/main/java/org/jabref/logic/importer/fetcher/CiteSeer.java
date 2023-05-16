package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.CiteSeerParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class CiteSeer implements SearchBasedParserFetcher {

    private static final String API_URL = "https://citeseerx.ist.psu.edu/api/search";

    public CiteSeer() {
    }

    @Override
    public String getName() {
        return "CiteSeerX";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_CITESEERX);
    }

    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        // ADR-0014
        URL urlForQuery;
        try {
            urlForQuery = getURLForQuery(luceneQuery);
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        }

        String payload = getPayloadString(new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        return getBibEntries(urlForQuery, payload);
    }

    private List<BibEntry> getBibEntries(URL urlForQuery, String payload) throws FetcherException {
        try {
            JsonNode requestResponse = Unirest.post(API_URL)
                                                     .header("authority", "citeseerx.ist.psu.edu")
                                                     .header("accept", "application/json, text/plain, */*")
                                                     .header("content-type", "application/json;charset=UTF-8")
                                                     .header("origin", "https://citeseerx.ist.psu.edu")
                                                     .body(payload)
                                                     .asJson().getBody();

            Optional<JSONArray> responses = Optional.of(requestResponse)
                                                    .map(JsonNode::getObject)
                                                    .filter(Objects::nonNull)
                                                    .map(jsonResponse -> jsonResponse.optJSONArray("response"))
                                                    .filter(Objects::nonNull);

            InputStream stream = new ByteArrayInputStream(responses.orElse(new JSONArray()).toString().getBytes(StandardCharsets.UTF_8));
            List<BibEntry> fetchedEntries = getParser().parseEntries(stream);
//            fetchedEntries.forEach(this::doPostCleanup);
            return fetchedEntries;
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries, ", ex);
        }
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        return new URIBuilder(API_URL).build().toURL();
    }

    private String getPayloadString(String queryString) {
        String payload = """
            '{'
                  \"queryString\":\"{0}\",
                  \"page\":1,
                  \"pageSize\":20,
                   \"sortBy\":\"relevance\",
                  \"must_have_pdf\":\"true\",
                  \"yearStart\":1913,
                  \"yearEnd\":2023,
                  \"author\":[],
                  \"publisher\":[]
            '}'
            """;
        return MessageFormat.format(payload, queryString);
    }

    @Override
    public Parser getParser() {
      return new CiteSeerParser();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // CiteSeer escapes some characters in a way that is not recognized by the normal html to unicode formatter
        // We, of course, also want to convert these special characters
        Formatter extendedHtmlFormatter = new HtmlToUnicodeFormatter() {
            @Override
            public String format(String fieldText) {
                String formatted = super.format(fieldText);
                formatted = formatted.replaceAll("%3A", ":");
                formatted = formatted.replaceAll("%3Cem%3", "");
                formatted = formatted.replaceAll("%3C%2Fem%3E", "");
                formatted = formatted.replaceAll("%2C\\+", " ");
                formatted = formatted.replaceAll("\\+", " ");
                return formatted;
            }
        };
        new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, extendedHtmlFormatter).cleanup(entry);

        // Many titles in the CiteSeer database have all-capital titles, for convenience we convert them to title case
        new FieldFormatterCleanup(StandardField.TITLE, new TitleCaseFormatter()).cleanup(entry);
    }
}
