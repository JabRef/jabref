package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

/*
 fetches books from https://www.doabooks.org/ through their API at
 https://www.doabooks.org/en/resources/metadata-harvesting-and-content-dissemination
 */

public class DOABFetcher implements SearchBasedParserFetcher {
    private static final String SEARCH_URL = "https://directory.doabooks.org/rest/search?";
    // private static final String PEER_REVIEW_URL = " https://directory.doabooks.org/rest/peerReviews?";

    @Override
    public String getName() {
        return "DOAB";
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder builder = new URIBuilder(SEARCH_URL);
        String query = new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("");
        // adding quotations for the query for more specified results
        // without the quotation the results returned are not relevant to the query
        query = ("\"".concat(query)).concat("\"");
        builder.addParameter("query", query);
        builder.addParameter("expand", "metadata");

        return builder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return InputStream -> {
            // can't use this method JsonReader.toJsonObject(inputStream) because the results are sent in an array
            // like format resulting in an error when trying to convert them into a json object
            // created a similar method suitable for this case "toJsonArray"
            JSONArray response = JsonReader.toJsonArray(InputStream);
            if (response.isEmpty()) {
                return Collections.emptyList();
            }
            // single result case
            if (response.length() == 1) {

                // the information used for bibtex entries are in an array inside the resulting jsonarray
                // see this query for reference https://directory.doabooks.org/rest/search?query="i open fire"&expand=metadata
                JSONArray metadataArray = response.getJSONObject(0).getJSONArray("metadata");
                BibEntry entry = JsonToBibEntry(metadataArray);
                return Collections.singletonList(entry);
            }
            // multiple results
            List<BibEntry> entries = new ArrayList<>(response.length());
            for (int i = 0; i < response.length(); i++) {
                JSONArray metadataArray = response.getJSONObject(i).getJSONArray("metadata");
                BibEntry entry = JsonToBibEntry(metadataArray);
                entries.add(entry);
            }
            return entries;
        };
    }

    private BibEntry JsonToBibEntry(JSONArray metadataArray) {
        BibEntry entry = new BibEntry();
        for (int i = 0; i < metadataArray.length(); i++) {
            JSONObject dataObject = metadataArray.getJSONObject(i);
            switch (dataObject.getString("key")) {
                case "dc.contributor.author" -> entry.setField(StandardField.AUTHOR,
                        dataObject.getString("value"));
                case "dc.type" -> entry.setField(StandardField.TYPE,
                        dataObject.getString("value"));
                case "dc.date.issued" -> entry.setField(StandardField.YEAR, String.valueOf(
                        dataObject.getInt("value")));
                case "oapen.identifier.doi" -> entry.setField(StandardField.DOI,
                        dataObject.getString("value"));
                case "dc.title" -> entry.setField(StandardField.TITLE,
                        dataObject.getString("value"));
                case "oapen.pages" -> entry.setField(StandardField.PAGES, String.valueOf(
                        dataObject.getInt("value")));
                case "dc.description.abstract" -> entry.setField(StandardField.ABSTRACT,
                        dataObject.getString("value"));
                case "dc.language" -> entry.setField(StandardField.LANGUAGE,
                        dataObject.getString("value"));
                case "publisher.name" -> entry.setField(StandardField.PUBLISHER,
                        dataObject.getString("value"));
                case "dc.identifier.uri" -> entry.setField(StandardField.URI,
                        dataObject.getString("value"));
                case "dc.subject.other" -> entry.setField(StandardField.KEYWORDS,
                        dataObject.getString("value"));
                case "dc.contributor.editor" -> entry.setField(StandardField.EDITOR,
                        dataObject.getString("value"));
            }
        }
        return entry;
    }

}
