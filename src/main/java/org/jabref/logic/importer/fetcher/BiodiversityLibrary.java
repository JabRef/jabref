package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.BiodiversityLibraryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the Biodiversity Heritage Library
 *
 * @implNote <a href="https://www.biodiversitylibrary.org/docs/api3.html">API documentation</a>
 */
public class BiodiversityLibrary implements SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BiodiversityLibrary.class);
    private static final String API_KEY = new BuildInfo().biodiversityHeritageApiKey;
    private static final String BASE_URL = "https://www.biodiversitylibrary.org/api3";
    private static final String RESPONSE_FORMAT = "json";

    public URL getBaseURL() throws URISyntaxException, MalformedURLException {
        URIBuilder baseURI = new URIBuilder(BASE_URL);
        baseURI.addParameter("apikey", API_KEY);
        baseURI.addParameter("format", RESPONSE_FORMAT);

        return baseURI.build().toURL();
    }

    @Override
    public String getName() {
        return "Biodiversity Heritage";
    }

    public URL getItemMetadataURL(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(getBaseURL().toURI());
        uriBuilder.addParameter("op", "GetItemMetadata");
        uriBuilder.addParameter("pages", "f");
        uriBuilder.addParameter("ocr", "f");
        uriBuilder.addParameter("ocr", "f");
        uriBuilder.addParameter("id", identifier);

        return uriBuilder.build().toURL();
    }

    public URL getPartMetadataURL(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(getBaseURL().toURI());
        uriBuilder.addParameter("op", "GetPartMetadata");
        uriBuilder.addParameter("pages", "f");
        uriBuilder.addParameter("names", "f");
        uriBuilder.addParameter("id", identifier);

        return uriBuilder.build().toURL();
    }

    public JSONObject getDetails(URL url) throws IOException {
        URLDownload download = new URLDownload(url);
        String response = download.asString();
        return new JSONObject(response).getJSONArray("Result").getJSONObject(0);
    }

    public BibEntry mountDetails(JSONObject item, BibEntry entry) throws IOException, URISyntaxException { // FixMe ???? Method name ist unfug
        if (item.has("BHLType")) {
            if (item.getString("BHLType").equals("Part")) {
                URL url = getPartMetadataURL(item.getString("PartID"));
                JSONObject itemsDetails = getDetails(url);
                entry.setField(StandardField.LANGUAGE, itemsDetails.optString("Language", ""));

                entry.setField(StandardField.DOI, itemsDetails.optString("Doi", ""));

                entry.setField(StandardField.PUBLISHER, itemsDetails.optString("PublisherName", ""));
                entry.setField(StandardField.DATE, itemsDetails.optString("Date", ""));
                entry.setField(StandardField.VOLUME, itemsDetails.optString("Volume", ""));
                entry.setField(StandardField.URL, itemsDetails.optString("PartUrl", ""));
            }

            if (item.getString("BHLType").equals("Item")) {
                URL url = getItemMetadataURL(item.getString("ItemID"));
                JSONObject itemsDetails = getDetails(url);
                entry.setField(StandardField.EDITOR, itemsDetails.optString("Sponsor", ""));
                entry.setField(StandardField.PUBLISHER, itemsDetails.optString("HoldingInstitution", ""));
                entry.setField(StandardField.LANGUAGE, itemsDetails.optString("Language", ""));
                entry.setField(StandardField.URL, itemsDetails.optString("ItemUrl", ""));
                if (itemsDetails.has("Date") && !entry.hasField(StandardField.DATE) && !entry.hasField(StandardField.YEAR)) {
                    entry.setField(StandardField.DATE, itemsDetails.getString("Date"));
                }
            }
        }

        return entry;
    }

    public BibEntry jsonResultToBibEntry(JSONObject item) {
        BibEntry entry = new BibEntry();

        if ("Book".equals(item.optString("Genre"))) {
            entry.setType(StandardEntryType.Book);
        } else {
            entry.setType(StandardEntryType.Article);
        }
        entry.setField(StandardField.TITLE, item.optString("Title", ""));

        entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("Authors")));

        entry.setField(StandardField.PAGES, item.optString("PageRange", ""));
        entry.setField(StandardField.PUBSTATE, item.optString("PublisherPlace", ""));
        entry.setField(StandardField.PUBLISHER, item.optString("PublisherName", ""));

        entry.setField(StandardField.DATE, item.optString("Date", ""));
        entry.setField(StandardField.YEAR, item.optString("PublicationDate", ""));
        entry.setField(StandardField.JOURNALTITLE, item.optString("ContainerTitle", ""));
        entry.setField(StandardField.VOLUME, item.optString("Volume", ""));

        return entry;
    }

    private String toAuthors(JSONArray authors) {
        if (authors == null) {
            return "";
        }

        // input: list of { "Name": "Author name,"}
        return IntStream.range(0, authors.length())
                        .mapToObj(authors::getJSONObject)
                        .map((author) -> new Author(
                                author.optString("Name", ""), "", "", "", ""))
                        .collect(AuthorList.collect())
                        .getAsFirstLastNamesWithAnd();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return Collections.emptyList();
            }

            String errorMessage = response.getString("ErrorMessage");
            if (!errorMessage.isBlank()) {
                return Collections.emptyList();
            }

            JSONArray items = response.getJSONArray("Result");
            List<BibEntry> entries = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                BibEntry entry = jsonResultToBibEntry(item);
                try {
                    entry = mountDetails(item, entry);
                } catch (JSONException | IOException | URISyntaxException exception) {
                    throw new ParseException("Error when parsing entry", exception);
                }
                entries.add(entry);
            }

            return entries;
        };
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(getBaseURL().toURI());
        BiodiversityLibraryTransformer transformer = new BiodiversityLibraryTransformer();
        uriBuilder.addParameter("op", "PublicationSearchAdvanced");
        uriBuilder.addParameter("authorname", transformer.transformLuceneQuery(luceneQuery).orElse(""));
        return uriBuilder.build().toURL();
    }
}
