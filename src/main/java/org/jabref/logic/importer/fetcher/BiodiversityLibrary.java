package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.BiodiversityLibraryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
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

    public BibEntry getMostDetails(JSONObject item, BibEntry entry) throws IOException, URISyntaxException { // FixMe ???? Method name ist unfug
        if (item.has("BHLType")) {
            if (item.getString("BHLType").equals("Part")) {
                URL url = getPartMetadataURL(item.getString("PartID"));
                JSONObject itemsDetails = getDetails(url);
                if (itemsDetails.has("Language")) {
                    entry.setField(StandardField.LANGUAGE, itemsDetails.getString("Language"));
                }
                if (itemsDetails.has("Doi")) {
                    entry.setField(StandardField.DOI, itemsDetails.getString("Doi"));
                }
                if (itemsDetails.has("PublisherName")) {
                    entry.setField(StandardField.PUBLISHER, itemsDetails.getString("PublisherName"));
                }
                if (itemsDetails.has("Volume") && !entry.hasField(StandardField.VOLUME)) {
                    entry.setField(StandardField.VOLUME, itemsDetails.getString("Volume"));
                }
                if (itemsDetails.has("Date") && !entry.hasField(StandardField.DATE) && !entry.hasField(StandardField.YEAR)) {
                    entry.setField(StandardField.DATE, itemsDetails.getString("Date"));
                }
                if (itemsDetails.has("PartUrl")) {
                    entry.setField(StandardField.URL, itemsDetails.getString("PartUrl"));
                }
            }

            if (item.getString("BHLType").equals("Item")) {
                URL url = getItemMetadataURL(item.getString("ItemID"));
                JSONObject itemsDetails = getDetails(url);
                if (itemsDetails.has("Sponsor")) {
                    entry.setField(StandardField.EDITOR, itemsDetails.getString("Sponsor"));
                }
                if (itemsDetails.has("HoldingInstitution")) {
                    entry.setField(StandardField.PUBLISHER, itemsDetails.getString("HoldingInstitution"));
                }
                if (itemsDetails.has("Language")) {
                    entry.setField(StandardField.LANGUAGE, itemsDetails.getString("Language"));
                }
                if (itemsDetails.has("ItemUrl")) {
                    entry.setField(StandardField.URL, itemsDetails.getString("ItemUrl"));
                }
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

        if (item.has("Title")) {
            entry.setField(StandardField.TITLE, item.optString("Title"));
        }

        if (item.has("Authors")) {
            JSONArray authors = item.getJSONArray("Authors");
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("Name")) {
                    authorList.add(authors.getJSONObject(i).getString("Name"));
                } else {
                    LOGGER.debug("Empty author name.");
                }
            }
            entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.debug("Empty author name");
        }

        if (item.has("PageRange")) {
            entry.setField(StandardField.PAGES, item.getString("PageRange"));
        } else {
            LOGGER.debug("Empty pages number");
        }

        if (item.has("PublisherPlace")) {
            entry.setField(StandardField.PUBSTATE, item.getString("PublisherPlace"));
        } else {
            LOGGER.debug("Empty Publisher Place");
        }

        if (item.has("PublisherName")) {
            entry.setField(StandardField.PUBLISHER, item.getString("PublisherName"));
        } else {
            LOGGER.debug("Empty Publisher Name");
        }

        if (item.has("Date")) {
            entry.setField(StandardField.DATE, item.getString("Date"));
        } else if (item.has("PublicationDate")) {
            entry.setField(StandardField.YEAR, item.getString("PublicationDate"));
        } else {
            LOGGER.debug("Empty date");
        }

        if (item.has("ContainerTitle")) {
            entry.setField(StandardField.JOURNALTITLE, item.getString("ContainerTitle"));
        } else {
            LOGGER.debug("Empty journal name");
        }

        if (item.has("Volume")) {
            entry.setField(StandardField.VOLUME, item.getString("Volume"));
        } else {
            LOGGER.debug("Empty volume number");
        }

        return entry;
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
                    entry = getMostDetails(item, entry);
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
        transformer.transformLuceneQuery(luceneQuery).orElse(""); // FixMe: ????? result ignored
        uriBuilder.addParameter("op", "PublicationSearchAdvanced");

        if (transformer.getAuthor().isPresent()) {
            uriBuilder.addParameter("authorname", transformer.getAuthor().get());
        }

        if (transformer.getTitle().isPresent()) {
            uriBuilder.addParameter("title", transformer.getTitle().get());
            uriBuilder.addParameter("titleop", "all");
        }

        if (transformer.getTitle().isEmpty() && transformer.getAuthor().isEmpty()) {
                throw new FetcherException("Must add author or title");
        }

        return uriBuilder.build().toURL();
    }
}
