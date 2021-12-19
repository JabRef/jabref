package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Enums;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ACMPortalParser implements Parser {

    private static final String HOST = "https://dl.acm.org";
    private static final String DOI_URL = "https://dl.acm.org/action/exportCiteProcCitation";

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

        try {
            Document doc = Jsoup.parse(stream, null, HOST);
            Elements doiHrefs = doc.select("div.issue-item__content-right > h5 > span > a");

            for (Element elem : doiHrefs) {
                String fullSegement = elem.attr("href");
                String doi = fullSegement.substring(fullSegement.indexOf("10"));
                doiList.add(doi);
            }
        } catch (IOException ex) {
            throw new ParseException(ex);
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
        if ("PAPER_CONFERENCE".equals(typeStr)) {
            type = StandardEntryType.Conference;
        } else {
            String upperUnderscoreTyeStr = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, typeStr);
            type = Enums.getIfPresent(StandardEntryType.class, upperUnderscoreTyeStr).or(StandardEntryType.Article);
        }
        return type;
    }

    /**
     * Parse BibEntry from query result xml
     *
     * @param jsonStr query result in JSON format
     * @return BibEntry parsed from query result
     */
    public BibEntry parseBibEntry(String jsonStr) {
        JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();
        BibEntry bibEntry = new BibEntry();
        if (jsonObject.has("type")) {
            bibEntry.setType(typeStrToEnum(jsonObject.get("type").getAsString()));
        }

        if (jsonObject.has("author")) {
            JsonArray authors = jsonObject.getAsJsonArray("author");
            StringJoiner authorsJoiner = new StringJoiner(" and ");
            for (JsonElement author : authors) {
                JsonObject authorJsonObject = author.getAsJsonObject();
                authorsJoiner.add(
                        authorJsonObject.get("given").getAsString() + " " + authorJsonObject.get("family").getAsString()
                );
            }
            bibEntry.setField(StandardField.AUTHOR, authorsJoiner.toString());
        }

        if (jsonObject.has("issued")) {
            JsonObject issued = jsonObject.get("issued").getAsJsonObject();
            if (issued.has("date-parts")) {
                JsonArray dateArray = issued.get("date-parts").getAsJsonArray().get(0).getAsJsonArray();
                StandardField[] dateField = {StandardField.YEAR, StandardField.MONTH, StandardField.DAY};
                for (int i = 0; i < dateArray.size(); i++) {
                    bibEntry.setField(dateField[i], dateArray.get(i).getAsString());
                }
            }
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
