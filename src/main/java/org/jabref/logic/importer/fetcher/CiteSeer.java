package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.CiteSeerQueryTransformer;
import org.jabref.logic.importer.fileformat.CiteSeerParser;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONElement;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiteSeer implements SearchBasedFetcher, FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiteSeer.class);

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
            HttpResponse<JsonNode> httpResponse = Unirest.post(API_URL)
                                                         .header("authority", BASE_URL)
                                                         .header("accept", "application/json, text/plain, */*")
                                                         .header("content-type", "application/json;charset=UTF-8")
                                                         .header("origin", "https://" + BASE_URL)
                                                         .body(payload)
                                                         .asJson();
            if (!httpResponse.isSuccess()) {
                LOGGER.debug("No success");
                // TODO: body needs to be added to the exception, but we currently only have JSON available, but the error is most probably simple text (or HTML)
                SimpleHttpResponse simpleHttpResponse = new SimpleHttpResponse(httpResponse.getStatus(), httpResponse.getStatusText(), "");
                throw new FetcherException(API_URL, simpleHttpResponse);
            }

            JsonNode requestResponse = httpResponse.getBody();
            Optional<JSONArray> jsonResponse = Optional.ofNullable(requestResponse)
                                                       .map(JsonNode::getObject)
                                                       .map(response -> response.optJSONArray("response"));

            if (jsonResponse.isEmpty()) {
                LOGGER.debug("No entries found for query: {}", luceneQuery);
                return List.of();
            }

            CiteSeerParser parser = new CiteSeerParser();
            List<BibEntry> fetchedEntries = parser.parseCiteSeerResponse(jsonResponse.orElse(new JSONArray()));
            return fetchedEntries;
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries", ex);
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
            return Optional.of(URLUtil.create(source));
        }

        // if using id fails, we can try the source URL
        Optional<String> urlString = entry.getField(StandardField.URL);
        if (urlString.isPresent()) {
            return Optional.of(URLUtil.create(urlString.get()));
        }

        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }
}
