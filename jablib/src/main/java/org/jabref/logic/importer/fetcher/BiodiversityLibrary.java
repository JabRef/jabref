package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.BiodiversityLibraryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import com.google.common.annotations.VisibleForTesting;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the Biodiversity Heritage Library
 *
 * @see <a href="https://www.biodiversitylibrary.org/docs/api3.html">API documentation</a>
 */
public class BiodiversityLibrary implements SearchBasedParserFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Biodiversity Heritage";

    private static final Logger LOGGER = LoggerFactory.getLogger(BiodiversityLibrary.class);

    private static final String BASE_URL = "https://www.biodiversitylibrary.org/api3";
    private static final String RESPONSE_FORMAT = "json";

    private final ImporterPreferences importerPreferences;

    public BiodiversityLibrary(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_BIODIVERSITY_HERITAGE_LIBRARY);
    }

    private String getTestUrl(String apiKey) {
        return "https://www.biodiversitylibrary.org/api3?apikey=" + apiKey;
    }

    @Override
    public boolean isValidKey(@NonNull String apiKey) {
        try {
            URLDownload urlDownload = new URLDownload(getTestUrl(apiKey));
            int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
            return (statusCode >= 200) && (statusCode < 300);
        } catch (IOException | UnirestException e) {
            return false;
        }
    }

    public URL getBaseURL() throws URISyntaxException, MalformedURLException {
        URIBuilder baseURI = new URIBuilder(BASE_URL);
        importerPreferences.getApiKey(getName()).ifPresent(key -> baseURI.addParameter("apikey", key));
        baseURI.addParameter("format", RESPONSE_FORMAT);

        return baseURI.build().toURL();
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

    @VisibleForTesting
    URL getPartMetadataURL(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(getBaseURL().toURI());
        uriBuilder.addParameter("op", "GetPartMetadata");
        uriBuilder.addParameter("pages", "f");
        uriBuilder.addParameter("names", "f");
        uriBuilder.addParameter("id", identifier);

        return uriBuilder.build().toURL();
    }

    public JSONObject getDetails(URL url) throws FetcherException {
        URLDownload download = new URLDownload(url);
        String response = download.asString();
        LOGGER.debug("Response {}", response);
        return new JSONObject(response).getJSONArray("Result").getJSONObject(0);
    }

    public BibEntry parseBibJSONtoBibtex(JSONObject item, BibEntry entry) throws FetcherException {
        if (item.has("BHLType")) {
            if ("Part".equals(item.getString("BHLType"))) {
                URL url;
                try {
                    url = getPartMetadataURL(item.getString("PartID"));
                } catch (URISyntaxException | MalformedURLException e) {
                    throw new FetcherException("Malformed URL", e);
                }
                JSONObject itemsDetails = getDetails(url);
                entry.setField(StandardField.LANGUAGE, itemsDetails.optString("Language", ""));

                entry.setField(StandardField.DOI, itemsDetails.optString("Doi", ""));

                entry.setField(StandardField.PUBLISHER, itemsDetails.optString("PublisherName", ""));
                entry.setField(StandardField.DATE, itemsDetails.optString("Date", ""));
                entry.setField(StandardField.VOLUME, itemsDetails.optString("Volume", ""));
                entry.setField(StandardField.URL, itemsDetails.optString("PartUrl", ""));
            }

            if ("Item".equals(item.getString("BHLType"))) {
                URL url;
                try {
                    url = getItemMetadataURL(item.getString("ItemID"));
                } catch (URISyntaxException | MalformedURLException e) {
                    throw new FetcherException("Malformed URL", e);
                }
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
        entry.setField(StandardField.LOCATION, item.optString("PublisherPlace", ""));
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
                        .map(author -> new Author(
                                author.optString("Name", ""), "", "", "", ""))
                        .collect(AuthorList.collect())
                        .getAsFirstLastNamesWithAnd();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return List.of();
            }

            String errorMessage = response.getString("ErrorMessage");
            if (!errorMessage.isBlank()) {
                return List.of();
            }

            JSONArray items = response.getJSONArray("Result");
            List<BibEntry> entries = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                BibEntry entry = jsonResultToBibEntry(item);
                try {
                    entry = parseBibJSONtoBibtex(item, entry);
                } catch (JSONException | FetcherException exception) {
                    throw new ParseException("Error when parsing entry", exception);
                }
                entries.add(entry);
            }

            return entries;
        };
    }

    @Override
    public URL getURLForQuery(BaseQueryNode query) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(getBaseURL().toURI());
        BiodiversityLibraryTransformer transformer = new BiodiversityLibraryTransformer();
        uriBuilder.addParameter("op", "PublicationSearch");
        uriBuilder.addParameter("searchtype", "C");
        uriBuilder.addParameter("searchterm", transformer.transformSearchQuery(query).orElse(""));
        return uriBuilder.build().toURL();
    }
}
