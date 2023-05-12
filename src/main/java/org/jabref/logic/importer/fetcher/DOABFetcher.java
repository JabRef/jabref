package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

/**
 * fetches books from https://www.doabooks.org/ through
 * <a href="https://www.doabooks.org/en/resources/metadata-harvesting-and-content-dissemination">their API</a>.
 */
public class DOABFetcher implements SearchBasedParserFetcher {
    private static final String SEARCH_URL = "https://directory.doabooks.org/rest/search?";

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
        // bitstreams included in URL building to acquire ISBN's.
        builder.addParameter("expand", "metadata,bitstreams");

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
            if (response.length() == 1) {
                // the information used for bibtex entries are in an array inside the resulting jsonarray
                // see this query for reference https://directory.doabooks.org/rest/search?query="i open fire"&expand=metadata
                JSONArray metadataArray = response.getJSONObject(0).getJSONArray("metadata");
                JSONArray bitstreamArray = response.getJSONObject(0).getJSONArray("bitstreams");
                BibEntry entry = jsonToBibEntry(metadataArray, bitstreamArray);
                return Collections.singletonList(entry);
            }
            List<BibEntry> entries = new ArrayList<>(response.length());
            for (int i = 0; i < response.length(); i++) {
                JSONArray metadataArray = response.getJSONObject(i).getJSONArray("metadata");
                JSONArray bitstreamArray = response.getJSONObject(i).getJSONArray("bitstreams");
                BibEntry entry = jsonToBibEntry(metadataArray, bitstreamArray);
                entries.add(entry);
            }
            return entries;
        };
    }

    private BibEntry jsonToBibEntry(JSONArray metadataArray, JSONArray bitstreamArray) {
        BibEntry entry = new BibEntry();
        List<Author> authorsList = new ArrayList<>();
        List<Author> editorsList = new ArrayList<>();
        StringJoiner keywordJoiner = new StringJoiner(", ");
        String publisherImprint = "";

        // Get the ISBN within the BITSTREAM. See the link below:
        // https://directory.doabooks.org/rest/search?query=handle:%2220.500.12854/26303%22&expand=metadata,bitstreams
        // Note that in many cases, an ISBN cannot be obtained in the metadata, even in the BITSTREAM. See the link below:
        // https://directory.doabooks.org/rest/search?query=%22i%20open%20fire%22&expand=metadata,bitstreams
        for (int i = 0; i < bitstreamArray.length(); i++) {
            JSONObject bitstreamObject = bitstreamArray.getJSONObject(i);
            // Subcategorise each instance of the BITSTREAM by "metadata" key
            JSONArray array = bitstreamObject.getJSONArray("metadata");
            for (int k = 0; k < array.length(); k++) {
                JSONObject metadataInBitstreamObject = array.getJSONObject(k);
                if (metadataInBitstreamObject.getString("key").equals("dc.identifier.isbn")) {
                    entry.setField(StandardField.ISBN, metadataInBitstreamObject.getString("value"));
                } else if (metadataInBitstreamObject.getString("key").equals("oapen.relation.isbn")) {
                    entry.setField(StandardField.ISBN, metadataInBitstreamObject.getString("value"));
                }
            }
        }

        for (int i = 0; i < metadataArray.length(); i++) {
            JSONObject dataObject = metadataArray.getJSONObject(i);
            switch (dataObject.getString("key")) {
                case "dc.contributor.author" -> {
                    if (dataObject.getString("value").contains("(Ed.)")) {
                       editorsList.add(toAuthor(namePreprocessing(dataObject.getString("value"))));
                    } else {
                        authorsList.add(toAuthor(dataObject.getString("value")));
                    }
                }
                case "dc.type" -> entry.setType(StandardEntryType.Book);
                case "dc.date.issued" -> entry.setField(StandardField.DATE, String.valueOf(
                        dataObject.getString("value")));
                case "oapen.identifier.doi" -> entry.setField(StandardField.DOI,
                        dataObject.getString("value"));
                case "dc.title" -> entry.setField(StandardField.TITLE,
                        dataObject.getString("value"));
                case "oapen.pages" -> {
                    try {
                        entry.setField(StandardField.PAGES, String.valueOf(dataObject.getInt("value")));
                    } catch (JSONException e) {
                        entry.setField(StandardField.PAGES, dataObject.getString("value"));
                    }
                }
                case "dc.description.abstract" -> entry.setField(StandardField.ABSTRACT,
                        dataObject.getString("value"));
                case "dc.language" -> entry.setField(StandardField.LANGUAGE,
                        dataObject.getString("value"));
                case "publisher.name" -> entry.setField(StandardField.PUBLISHER,
                        dataObject.getString("value"));
                case "dc.identifier.uri" -> entry.setField(StandardField.URI,
                        dataObject.getString("value"));
                case "dc.identifier" -> {
                    if (dataObject.getString("value").contains("http")) {
                       entry.setField(StandardField.URL, dataObject.getString("value"));
                    }
                }
                case "dc.subject.other" -> keywordJoiner.add(dataObject.getString("value"));
                case "dc.contributor.editor" -> editorsList.add(toAuthor(dataObject.getString("value")));
                case "oapen.volume" -> entry.setField(StandardField.VOLUME,
                        dataObject.getString("value"));
                case "oapen.relation.isbn", "dc.identifier.isbn" -> entry.setField(StandardField.ISBN,
                        dataObject.getString("value"));
                case "dc.title.alternative" -> entry.setField(StandardField.SUBTITLE,
                        dataObject.getString("value"));
                case "oapen.imprint" -> publisherImprint = dataObject.getString("value");
            }
        }

        entry.setField(StandardField.AUTHOR, AuthorList.of(authorsList).getAsFirstLastNamesWithAnd());
        entry.setField(StandardField.EDITOR, AuthorList.of(editorsList).getAsFirstLastNamesWithAnd());
        entry.setField(StandardField.KEYWORDS, String.valueOf(keywordJoiner));

        // Special condition to check if publisher field is empty. If so, retrieve imprint (if available)
        if (entry.getField(StandardField.PUBLISHER).isEmpty()) {
            if (!StringUtil.isNullOrEmpty(publisherImprint)) {
                entry.setField(StandardField.PUBLISHER, publisherImprint);
            }
        }
        return entry;
    }

    private Author toAuthor(String author) {
        return AuthorList.parse(author).getAuthor(0);
    }

    private String namePreprocessing(String name) {
        return name.replace("(Ed.)", "");
    }
}
