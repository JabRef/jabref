package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.CiteSeerQueryTransformer;
import org.jabref.logic.importer.fileformat.CiteSeerParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONElement;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class CiteSeer implements SearchBasedFetcher, FulltextFetcher {

    private static final String BASE_URL = "citeseerx.ist.psu.edu";

    private static final String API_URL = "https://citeseerx.ist.psu.edu/api/search";

    private static final String PDF_URL = "https://" + BASE_URL + "/document?repid=rep1&type=pdf&doi=%s";

    private CiteSeerQueryTransformer transformer;

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
        try {
            JSONElement payload = getPayloadJSON(luceneQuery);
            JsonNode requestResponse = Unirest.post(API_URL)
                                              .header("authority", BASE_URL)
                                              .header("accept", "application/json, text/plain, */*")
                                              .header("content-type", "application/json;charset=UTF-8")
                                              .header("origin", "https://" + BASE_URL)
                                              .body(payload)
                                              .asJson().getBody();

            Optional<JSONArray> jsonResponse = Optional.of(requestResponse)
                                                    .map(JsonNode::getObject)
                                                    .filter(Objects::nonNull)
                                                    .map(response -> response.optJSONArray("response"))
                                                    .filter(Objects::nonNull);

            if (!jsonResponse.isPresent()) {
                return List.of();
            }

            CiteSeerParser parser = new CiteSeerParser();
            List<BibEntry> fetchedEntries = parser.parseCiteSeerResponse(jsonResponse.orElse(new JSONArray()));
            return fetchedEntries;
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries, ", ex);
        }
    }

    private JSONElement getPayloadJSON(QueryNode luceneQuery) {
        transformer = new CiteSeerQueryTransformer();
        String transformedQuery = transformer.transformLuceneQuery(luceneQuery).orElse("");
        return transformer.getJSONPayload();
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);

        // does not use a valid DOI, but Cite Seer's id / hash available for each entry
        Optional<String> id = entry.getField(StandardField.DOI);
        if (id.isPresent()) {
            String source = PDF_URL.formatted(id.get());
            return Optional.of(new URL(source));
        }

        // if using id fails, we can try the source URL
        Optional<String> urlString = entry.getField(StandardField.URL);
        if (urlString.isPresent()) {
            return Optional.of(new URL(urlString.get()));
        }

        return Optional.empty();
    }
}
