package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for finding PDF URLs for entries on IEEE
 * Will first look for URLs of the type https://ieeexplore.ieee.org/stamp/stamp.jsp?[tp=&]arnumber=...
 * If not found, will resolve the DOI, if it starts with 10.1109, and try to find a similar link on the HTML page
 *
 * @implNote <a href="https://developer.ieee.org/docs">API documentation</a>
 */
public class IEEE implements FulltextFetcher, SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IEEE.class);
    private static final String STAMP_BASE_STRING_DOCUMENT = "/stamp/stamp.jsp?tp=&arnumber=";
    private static final Pattern STAMP_PATTERN = Pattern.compile("(/stamp/stamp.jsp\\?t?p?=?&?arnumber=[0-9]+)");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("document/([0-9]+)/");

    private static final Pattern PDF_PATTERN = Pattern.compile("\"(https://ieeexplore.ieee.org/ielx[0-9/]+\\.pdf[^\"]+)\"");
    private static final String IEEE_DOI = "10.1109";
    private static final String BASE_URL = "https://ieeexplore.ieee.org";
    private static final String API_KEY = new BuildInfo().ieeeAPIKey;

    private final ImportFormatPreferences preferences;

    public IEEE(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * @implNote <a href="https://developer.ieee.org/docs/read/Metadata_API_responses">documentation</a>
     */
    private static BibEntry parseJsonRespone(JSONObject jsonEntry, Character keywordSeparator) {
        BibEntry entry = new BibEntry();

        switch (jsonEntry.optString("content_type")) {
            case "Books" -> entry.setType(StandardEntryType.Book);
            case "Conferences" -> entry.setType(StandardEntryType.InProceedings);
            case "Courses" -> entry.setType(StandardEntryType.Misc);
            default -> entry.setType(StandardEntryType.Article);
        }

        entry.setField(StandardField.ABSTRACT, jsonEntry.optString("abstract"));
        // entry.setField(StandardField.IEEE_ID, jsonEntry.optString("article_number"));

        final List<String> authors = new ArrayList<>();
        JSONObject authorsContainer = jsonEntry.optJSONObject("authors");
        authorsContainer.getJSONArray("authors").forEach(authorPure -> {
            JSONObject author = (JSONObject) authorPure;
            authors.add(author.optString("full_name"));
        });
        entry.setField(StandardField.AUTHOR, String.join(" and ", authors));
        entry.setField(StandardField.LOCATION, jsonEntry.optString("conference_location"));
        entry.setField(StandardField.DOI, jsonEntry.optString("doi"));
        entry.setField(StandardField.YEAR, jsonEntry.optString("publication_year"));
        entry.setField(StandardField.PAGES, jsonEntry.optString("start_page") + "--" + jsonEntry.optString("end_page"));

        JSONObject keywordsContainer = jsonEntry.optJSONObject("index_terms");
        if (keywordsContainer != null) {
            if (keywordsContainer.has("ieee_terms")) {
                keywordsContainer.getJSONObject("ieee_terms").getJSONArray("terms").forEach(data -> {
                    String keyword = (String) data;
                    entry.addKeyword(keyword, keywordSeparator);
                });
            }
            if (keywordsContainer.has("author_terms")) {
                keywordsContainer.getJSONObject("author_terms").getJSONArray("terms").forEach(data -> {
                    String keyword = (String) data;
                    entry.addKeyword(keyword, keywordSeparator);
                });
            }
        }

        entry.setField(StandardField.ISBN, jsonEntry.optString("isbn"));
        entry.setField(StandardField.ISSN, jsonEntry.optString("issn"));
        entry.setField(StandardField.ISSUE, jsonEntry.optString("issue"));
        try {
            entry.addFile(new LinkedFile(new URL(jsonEntry.optString("pdf_url")), "PDF"));
        } catch (MalformedURLException e) {
            LOGGER.error("Fetched PDF URL String is malformed.");
        }
        entry.setField(StandardField.JOURNALTITLE, jsonEntry.optString("publication_title"));
        entry.setField(StandardField.DATE, jsonEntry.optString("publication_date"));
        entry.setField(StandardField.EVENTTITLEADDON, jsonEntry.optString("conference_location"));
        entry.setField(StandardField.EVENTDATE, jsonEntry.optString("conference_dates"));
        entry.setField(StandardField.PUBLISHER, jsonEntry.optString("publisher"));
        entry.setField(StandardField.TITLE, jsonEntry.optString("title"));
        entry.setField(StandardField.VOLUME, jsonEntry.optString("volume"));

        return entry;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        String stampString = "";

        // Try URL first -- will primarily work for entries from the old IEEE search
        Optional<String> urlString = entry.getField(StandardField.URL);
        if (urlString.isPresent()) {

            Matcher documentUrlMatcher = DOCUMENT_PATTERN.matcher(urlString.get());
            if (documentUrlMatcher.find()) {
                String docId = documentUrlMatcher.group(1);
                stampString = STAMP_BASE_STRING_DOCUMENT + docId;
            }

            // You get this url if you export bibtex from IEEE
            Matcher stampMatcher = STAMP_PATTERN.matcher(urlString.get());
            if (stampMatcher.find()) {
                // Found it
                stampString = stampMatcher.group(1);
            }
        }

        // If not, try DOI
        if (stampString.isEmpty()) {
            Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
            if (doi.isPresent() && doi.get().getDOI().startsWith(IEEE_DOI) && doi.get().getExternalURI().isPresent()) {
                // Download the HTML page from IEEE
                URLDownload urlDownload = new URLDownload(doi.get().getExternalURI().get().toURL());
                // We don't need to modify the cookies, but we need support for them
                urlDownload.getCookieFromUrl();

                String resolvedDOIPage = urlDownload.asString();
                // Try to find the link
                Matcher matcher = STAMP_PATTERN.matcher(resolvedDOIPage);
                if (matcher.find()) {
                    // Found it
                    stampString = matcher.group(1);
                }
            }
        }

        // Any success?
        if (stampString.isEmpty()) {
            return Optional.empty();
        }

        // Download the HTML page containing a frame with the PDF
        URLDownload urlDownload = new URLDownload(BASE_URL + stampString);
        // We don't need to modify the cookies, but we need support for them
        urlDownload.getCookieFromUrl();

        String framePage = urlDownload.asString();
        // Try to find the direct PDF link
        Matcher matcher = PDF_PATTERN.matcher(framePage);
        if (matcher.find()) {
            // The PDF was found
            LOGGER.debug("Full text document found on IEEE Xplore");
            return Optional.of(new URL(matcher.group(1)));
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder("https://ieeexploreapi.ieee.org/api/v1/search/articles");
        uriBuilder.addParameter("apikey", API_KEY);
        uriBuilder.addParameter("querytext", query);

        URLDownload.bypassSSLVerification();

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("articles")) {
                JSONArray results = jsonObject.getJSONArray("articles");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jsonEntry = results.getJSONObject(i);
                    BibEntry entry = parseJsonRespone(jsonEntry, preferences.getKeywordSeparator());
                    entries.add(entry);
                }
            }

            return entries;
        };
    }

    @Override
    public String getName() {
        return "IEEEXplore";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_IEEEXPLORE);
    }

    @Override
    public URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder("https://ieeexploreapi.ieee.org/api/v1/search/articles");
        uriBuilder.addParameter("apikey", API_KEY);
        if (!complexSearchQuery.getDefaultFieldPhrases().isEmpty()) {
            uriBuilder.addParameter("querytext", String.join(" AND ", complexSearchQuery.getDefaultFieldPhrases()));
        }
        if (!complexSearchQuery.getAuthors().isEmpty()) {
            uriBuilder.addParameter("author", String.join(" AND ", complexSearchQuery.getAuthors()));
        }
        if (!complexSearchQuery.getAbstractPhrases().isEmpty()) {
            uriBuilder.addParameter("abstract", String.join(" AND ", complexSearchQuery.getAbstractPhrases()));
        }
        if (!complexSearchQuery.getTitlePhrases().isEmpty()) {
            uriBuilder.addParameter("article_title", String.join(" AND ", complexSearchQuery.getTitlePhrases()));
        }
        complexSearchQuery.getJournal().ifPresent(journalTitle -> uriBuilder.addParameter("publication_title", journalTitle));
        complexSearchQuery.getFromYear().map(String::valueOf).ifPresent(year -> uriBuilder.addParameter("start_year", year));
        complexSearchQuery.getToYear().map(String::valueOf).ifPresent(year -> uriBuilder.addParameter("end_year", year));
        complexSearchQuery.getSingleYear().map(String::valueOf).ifPresent(year -> {
            uriBuilder.addParameter("start_year", year);
            uriBuilder.addParameter("end_year", year);
        });

        URLDownload.bypassSSLVerification();
        return uriBuilder.build().toURL();
    }
}
