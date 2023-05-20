package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.CiteSeerParser;
import org.jabref.model.entry.BibEntry;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONElement;
import kong.unirest.json.JSONObject;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class CiteSeer implements SearchBasedFetcher {

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
        try {
            JSONElement payload = getPayloadJSON(luceneQuery);
            JsonNode requestResponse = Unirest.post(API_URL)
                                              .header("authority", "citeseerx.ist.psu.edu")
                                              .header("accept", "application/json, text/plain, */*")
                                              .header("content-type", "application/json;charset=UTF-8")
                                              .header("origin", "https://citeseerx.ist.psu.edu")
                                              .body(payload)
                                              .asJson().getBody();

            Optional<JSONArray> jsonResponse = Optional.of(requestResponse)
                                                    .map(JsonNode::getObject)
                                                    .filter(Objects::nonNull)
                                                    .map(response -> response.optJSONArray("response"))
                                                    .filter(Objects::nonNull);

            CiteSeerParser parser = new CiteSeerParser();
            List<BibEntry> fetchedEntries = parser.parseCiteSeerResponse(jsonResponse.orElse(new JSONArray()));
            return fetchedEntries;
        } catch (URISyntaxException | MalformedURLException | FetcherException e) {
            throw new FetcherException("Search URI crafted from complex search query is malformed", e);
        } catch (ParseException ex) {
            throw new FetcherException("An internal parser error occurred while parsing CiteSeer entries, " ex);
        }
    }

    private JSONElement getPayloadJSON(QueryNode luceneQuery) {
        // use CiteSeerQueryTransformer
        return null;
    }

//    private JSONObject getPayloadString(String queryString) {
//        JSONObject payload = new JSONObject();
//        payload.put("queryString", "value");
//        payload.put("key", "value");
//        payload.put("key", "value");
//        payload.put("key", "value");
//        payload.put("key", "value");

//        String payload = """
//            '{'
//                  \"queryString\":\"{0}\",
//                  \"page\":1,
//                  \"pageSize\":20,
//                   \"sortBy\":\"relevance\",
//                  \"must_have_pdf\":\"false\",
//                  \"yearStart\":1913,
//                  \"yearEnd\":2023,
//                  \"author\":[],
//                  \"publisher\":[]
//            '}'
//            """;
//        return MessageFormat.format(payload, queryString);
//        return payload;
//    }
}
