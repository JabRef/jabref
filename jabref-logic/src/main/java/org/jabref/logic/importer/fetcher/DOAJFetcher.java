package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.net.URLUtil;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the Directory of Open Access Journals (DOAJ)
 *
 * @implNote <a href="https://doaj.org/api/v1/docs">API documentation</a>
 */
public class DOAJFetcher implements SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DOAJFetcher.class);

    private static final String SEARCH_URL = "https://doaj.org/api/v1/search/articles/";
    private final ImportFormatPreferences preferences;

    public DOAJFetcher(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    /**
     * Convert a JSONObject containing a bibJSON entry to a BibEntry
     *
     * @param bibJsonEntry The JSONObject to convert
     * @return the converted BibEntry
     */
    public static BibEntry parseBibJSONtoBibtex(JSONObject bibJsonEntry, Character keywordSeparator) {
        // Fields that are directly accessible at the top level BibJson object
        String[] singleFieldStrings = {FieldName.YEAR, FieldName.TITLE, FieldName.ABSTRACT, FieldName.MONTH};

        // Fields that are accessible in the journal part of the BibJson object
        String[] journalSingleFieldStrings = {FieldName.PUBLISHER, FieldName.NUMBER, FieldName.VOLUME};

        BibEntry entry = new BibEntry();
        entry.setType("article");

        // Authors
        if (bibJsonEntry.has("author")) {
            JSONArray authors = bibJsonEntry.getJSONArray("author");
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("name")) {
                    authorList.add(authors.getJSONObject(i).getString("name"));
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField(FieldName.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.info("No author found.");
        }

        // Direct accessible fields
        for (String field : singleFieldStrings) {
            if (bibJsonEntry.has(field)) {
                entry.setField(field, bibJsonEntry.getString(field));
            }
        }

        // Page numbers
        if (bibJsonEntry.has("start_page")) {
            if (bibJsonEntry.has("end_page")) {
                entry.setField(FieldName.PAGES,
                        bibJsonEntry.getString("start_page") + "--" + bibJsonEntry.getString("end_page"));
            } else {
                entry.setField(FieldName.PAGES, bibJsonEntry.getString("start_page"));
            }
        }

        // Journal
        if (bibJsonEntry.has("journal")) {
            JSONObject journal = bibJsonEntry.getJSONObject("journal");
            // Journal title
            if (journal.has("title")) {
                entry.setField(FieldName.JOURNAL, journal.getString("title").trim());
            } else {
                LOGGER.info("No journal title found.");
            }
            // Other journal related fields
            for (String field : journalSingleFieldStrings) {
                if (journal.has(field)) {
                    entry.setField(field, journal.getString(field));
                }
            }
        } else {
            LOGGER.info("No journal information found.");
        }

        // Keywords
        if (bibJsonEntry.has("keywords")) {
            JSONArray keywords = bibJsonEntry.getJSONArray("keywords");
            for (int i = 0; i < keywords.length(); i++) {
                if (!keywords.isNull(i)) {
                    entry.addKeyword(keywords.getString(i).trim(), keywordSeparator);
                }
            }
        }

        // Identifiers
        if (bibJsonEntry.has("identifier")) {
            JSONArray identifiers = bibJsonEntry.getJSONArray("identifier");
            for (int i = 0; i < identifiers.length(); i++) {
                String type = identifiers.getJSONObject(i).getString("type");
                if ("doi".equals(type)) {
                    entry.setField(FieldName.DOI, identifiers.getJSONObject(i).getString("id"));
                } else if ("pissn".equals(type)) {
                    entry.setField(FieldName.ISSN, identifiers.getJSONObject(i).getString("id"));
                } else if ("eissn".equals(type)) {
                    entry.setField(FieldName.ISSN, identifiers.getJSONObject(i).getString("id"));
                }
            }
        }

        // Links
        if (bibJsonEntry.has("link")) {
            JSONArray links = bibJsonEntry.getJSONArray("link");
            for (int i = 0; i < links.length(); i++) {
                if (links.getJSONObject(i).has("type")) {
                    String type = links.getJSONObject(i).getString("type");
                    if ("fulltext".equals(type) && links.getJSONObject(i).has("url")) {
                        entry.setField(FieldName.URL, links.getJSONObject(i).getString("url"));
                    }
                }
            }
        }

        return entry;
    }

    @Override
    public String getName() {
        return "DOAJ";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_DOAJ);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        URLUtil.addPath(uriBuilder, query);
        uriBuilder.addParameter("pageSize", "30"); // Number of results
        //uriBuilder.addParameter("page", "1"); // Page (not needed so far)
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("results")) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject bibJsonEntry = results.getJSONObject(i).getJSONObject("bibjson");
                    BibEntry entry = parseBibJSONtoBibtex(bibJsonEntry, preferences.getKeywordSeparator());
                    entries.add(entry);
                }
            }
            return entries;
        };
    }
}
