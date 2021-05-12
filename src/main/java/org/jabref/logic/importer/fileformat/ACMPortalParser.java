package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;

public class ACMPortalParser implements Parser {

    private static final String DOI_URL = "https://dl.acm.org/action/exportCiteProcCitation";
    private static final Pattern DIO_HTML_PATTERN = Pattern.compile("<input name=\"(.*?)\"");
    private static final String ITEM_HTML = "<li class=\"search__item issue-item-container\">";
    private static final int MAX_ITEM_CNT_PER_PAGE = 20;
    private static HashMap<String, StandardEntryType> ENTRY_TYPE_MAP;

    public ACMPortalParser() {
        // Gets the lowercase StandardEntryType keys and corresponding StandardEntryType
        StandardEntryType[] standardEntryTypes = StandardEntryType.values();
        ENTRY_TYPE_MAP = new HashMap<>(standardEntryTypes.length);
        for (StandardEntryType standardEntryType : standardEntryTypes) {
            ENTRY_TYPE_MAP.put(standardEntryType.getName(), standardEntryType);
        }
    }

    /**
     * Parse the DOI of the ACM Portal search result page and obtain the corresponding BibEntry
     *
     * @param stream html stream
     * @return BibEntry List
     */
    @Override
    public List<BibEntry> parseEntries(InputStream stream) throws ParseException {
        List<BibEntry> bibEntries;
        try {
            bibEntries = getBibEntriesFromDoiList(this.parseDoiSearchPage(stream));
        } catch (FetcherException e) {
            throw new ParseException(e);
        }
        return bibEntries;
    }

    /**
     * Parse all DOIs from the ACM Portal search results page
     *
     * @param stream html stream
     * @return DOI list
     */
    public List<String> parseDoiSearchPage(InputStream stream) throws ParseException {
        List<String> doiList = new ArrayList<>();
        String htmlLine;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            int cnt = 0;
            while ((htmlLine = in.readLine()) != null && cnt < MAX_ITEM_CNT_PER_PAGE) {
                if (ITEM_HTML.equals(htmlLine)) {
                    Matcher matcher = DIO_HTML_PATTERN.matcher(in.readLine());
                    if (matcher.find()) {
                        doiList.add(matcher.group(1));
                        ++cnt;
                    }
                }
            }
        } catch (IOException e) {
            throw new ParseException(e);
        }
        return doiList;
    }

    /**
     * Obtain BibEntry according to DOI
     *
     * @param doiList DOI List
     * @return BibEntry List
     */
    public List<BibEntry> getBibEntriesFromDoiList(List<String> doiList) throws FetcherException {
        List<BibEntry> bibEntries = new ArrayList<>();
        CookieHandler.setDefault(new CookieManager());
        try (InputStream stream = new URLDownload(getUrlFromDoiList(doiList)).asInputStream()) {
            String jsonString = new String((stream.readAllBytes()), Charsets.UTF_8);

            JsonElement jsonElement = JsonParser.parseString(jsonString);
            if (jsonElement.isJsonObject()) {
                JsonArray items = jsonElement.getAsJsonObject().getAsJsonArray("items");
                for (JsonElement item : items) {
                    for (Map.Entry<String, JsonElement> entry : item.getAsJsonObject().entrySet()) {
                        bibEntries.add(parseBibEntry(entry.getValue().toString()));
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new FetcherException("A network error occurred while fetching from ", e);
        }

        return bibEntries;
    }

    /**
     * Constructing the query url for the doi
     *
     * @param doiList DOI List
     * @return query URL
     */
    public URL getUrlFromDoiList(List<String> doiList) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(DOI_URL);
        uriBuilder.addParameter("targetFile", "custom-bibtex");
        uriBuilder.addParameter("format", "bibTex");
        uriBuilder.addParameter("dois", String.join(",", doiList));
        return uriBuilder.build().toURL();
    }

    private StandardEntryType typeStrToEnum(String typeStr) {
        StandardEntryType type;
        typeStr = typeStr.toLowerCase(Locale.ENGLISH).replace("_", "");
        if (ENTRY_TYPE_MAP.containsKey(typeStr)) {
            type = ENTRY_TYPE_MAP.get(typeStr);
        } else {
            // There may be other types that do not exactly match
            switch (typeStr) {
                case "PAPER_CONFERENCE":
                    type = StandardEntryType.Conference;
                    break;
                default:
                    type = StandardEntryType.Article;
            }
        }
        return type;
    }

    /**
     * Parse BibEntry from query result xml
     *
     * @param jsonStr query result in JSON format
     * @return BibEntry
     */
    public BibEntry parseBibEntry(String jsonStr) {
        JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();
        BibEntry bibEntry;

        bibEntry = new BibEntry();

        if (jsonObject.has("type")) {
            bibEntry.setType(typeStrToEnum(jsonObject.get("type").getAsString()));
        }

        if (jsonObject.has("author")) {
            JsonArray authors = jsonObject.getAsJsonArray("author");
            StringBuilder authorStrBuilder = new StringBuilder();
            int size = authors.size();
            for (JsonElement author : authors) {
                JsonObject authorJsonObject = author.getAsJsonObject();
                authorStrBuilder.append(authorJsonObject.get("given").getAsString()).append(" ")
                        .append(authorJsonObject.get("family").getAsString());
                --size;
                if (size > 0) {
                    authorStrBuilder.append(" and ");
                }
            }
            bibEntry.setField(StandardField.AUTHOR, authorStrBuilder.toString());
        }

        if (jsonObject.has("issued")) {
            JsonArray dateArray = jsonObject.get("issued").getAsJsonObject().get("date-parts").getAsJsonArray().get(0).getAsJsonArray();
            bibEntry.setField(StandardField.YEAR, dateArray.get(0).getAsString());
            bibEntry.setField(StandardField.MONTH, dateArray.get(1).getAsString());
            bibEntry.setField(StandardField.DAY, dateArray.get(2).getAsString());
        }

        if (jsonObject.has("abstract")) {
            bibEntry.setField(StandardField.ABSTRACT, jsonObject.get("abstract").getAsString());
        }

        if (jsonObject.has("collection-title")) {
            bibEntry.setField(StandardField.SERIES, jsonObject.get("collection-title").getAsString());
        }

        if (jsonObject.has("container-title")) {
            bibEntry.setField(StandardField.BOOKTITLE, jsonObject.get("container-title").getAsString());
        }

        if (jsonObject.has("DOI")) {
            bibEntry.setField(StandardField.DOI, jsonObject.get("DOI").getAsString());
        }

        if (jsonObject.has("event-place")) {
            bibEntry.setField(StandardField.LOCATION, jsonObject.get("event-place").getAsString());
        }

        if (jsonObject.has("ISBN")) {
            bibEntry.setField(StandardField.ISBN, jsonObject.get("ISBN").getAsString());
        }

        if (jsonObject.has("keyword")) {
            String[] keywords = jsonObject.get("keyword").getAsString().split(", ");
            String sortedKeywords = Arrays.stream(keywords).sorted().collect(Collectors.joining(", "));
            bibEntry.setField(StandardField.KEYWORDS, sortedKeywords);
        }

        if (jsonObject.has("number-of-pages")) {
            bibEntry.setField(StandardField.PAGETOTAL, jsonObject.get("number-of-pages").getAsString());
        }

        if (jsonObject.has("page")) {
            bibEntry.setField(StandardField.PAGES, jsonObject.get("page").getAsString());
        }

        if (jsonObject.has("publisher")) {
            bibEntry.setField(StandardField.PUBLISHER, jsonObject.get("publisher").getAsString());
        }

        if (jsonObject.has("publisher-place")) {
            bibEntry.setField(StandardField.ADDRESS, jsonObject.get("publisher-place").getAsString());
        }

        if (jsonObject.has("title")) {
            bibEntry.setField(StandardField.TITLE, jsonObject.get("title").getAsString());
        }

        if (jsonObject.has("URL")) {
            bibEntry.setField(StandardField.URL, jsonObject.get("URL").getAsString());
        }

        return bibEntry;
    }

}
