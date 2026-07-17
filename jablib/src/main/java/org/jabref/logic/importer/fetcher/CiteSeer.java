package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

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
import org.jabref.model.http.SimpleHttpResponse;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONElement;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CiteSeer implements SearchBasedFetcher, FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiteSeer.class);

    private static final String BASE_URL = "citeseerx.ist.psu.edu";

    private static final String API_URL = "https://citeseerx.ist.psu.edu/api/search";

    private static final String PDF_URL = "https://" + BASE_URL + "/document?repid=rep1&type=pdf&doi=%s";

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
    public List<BibEntry> performSearch(BaseQueryNode queryNode) throws FetcherException {
        // ADR-0014
        try {
            return sendSearchRequest(getPayloadJSON(queryNode));
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries", ex);
        }
    }

    @Override
    public List<BibEntry> performRawSearchQuery(String rawQuery) throws FetcherException {
        if (rawQuery.isBlank()) {
            return List.of();
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("page", CiteSeerQueryTransformer.DEFAULT_PAGE);
            payload.put("pageSize", CiteSeerQueryTransformer.DEFAULT_PAGE_SIZE);
            payload.put("must_have_pdf", CiteSeerQueryTransformer.DEFAULT_MUST_HAVE_PDF);
            payload.put("sortBy", CiteSeerQueryTransformer.DEFAULT_SORT_BY);
            payload.put("queryString", rawQuery);
            return sendSearchRequest(payload);
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries", ex);
        }
    }

    private List<BibEntry> sendSearchRequest(JSONElement payload) throws FetcherException, ParseException {
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
            LOGGER.debug("No entries found for payload: {}", payload);
            return List.of();
        }

        CiteSeerParser parser = new CiteSeerParser();
        return parser.parseCiteSeerResponse(jsonResponse.get());
    }

    private JSONElement getPayloadJSON(BaseQueryNode searchQueryList) throws ParseException {
        CiteSeerQueryTransformer transformer = new CiteSeerQueryTransformer();
        String transformedQuery = transformer.transformSearchQuery(searchQueryList).orElse("");
        return transformer.getJSONPayload();
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
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
