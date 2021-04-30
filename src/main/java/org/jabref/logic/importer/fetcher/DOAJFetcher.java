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
import org.jabref.logic.importer.fetcher.transformers.DefaultLuceneQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
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
        Field[] singleFields = {StandardField.YEAR, StandardField.TITLE, StandardField.ABSTRACT, StandardField.MONTH};

        // Fields that are accessible in the journal part of the BibJson object
        Field[] journalSingleFields = {StandardField.PUBLISHER, StandardField.NUMBER, StandardField.VOLUME};

        BibEntry entry = new BibEntry(StandardEntryType.Article);

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
            entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.info("No author found.");
        }

        // Direct accessible fields
        for (Field field : singleFields) {
            if (bibJsonEntry.has(field.getName())) {
                entry.setField(field, bibJsonEntry.getString(field.getName()));
            }
        }

        // Page numbers
        if (bibJsonEntry.has("start_page")) {
            if (bibJsonEntry.has("end_page")) {
                entry.setField(StandardField.PAGES,
                        bibJsonEntry.getString("start_page") + "--" + bibJsonEntry.getString("end_page"));
            } else {
                entry.setField(StandardField.PAGES, bibJsonEntry.getString("start_page"));
            }
        }

        // Journal
        if (bibJsonEntry.has("journal")) {
            JSONObject journal = bibJsonEntry.getJSONObject("journal");
            // Journal title
            if (journal.has("title")) {
                entry.setField(StandardField.JOURNAL, journal.getString("title").trim());
            } else {
                LOGGER.info("No journal title found.");
            }
            // Other journal related fields
            for (Field field : journalSingleFields) {
                if (journal.has(field.getName())) {
                    entry.setField(field, journal.getString(field.getName()));
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
                    entry.setField(StandardField.DOI, identifiers.getJSONObject(i).getString("id"));
                } else if ("pissn".equals(type)) {
                    entry.setField(StandardField.ISSN, identifiers.getJSONObject(i).getString("id"));
                } else if ("eissn".equals(type)) {
                    entry.setField(StandardField.ISSN, identifiers.getJSONObject(i).getString("id"));
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
                        entry.setField(StandardField.URL, links.getJSONObject(i).getString("url"));
                    }
                }
            }
        }

        return entry;
    }

    /**
     * @implNote slightly altered version based on https://gist.github.com/enginer/230e2dc2f1d213a825d5
     */
    public static URIBuilder addPath(URIBuilder base, String subPath) {
        if (StringUtil.isBlank(subPath) || "/".equals(subPath)) {
            return base;
        } else {
            base.setPath(appendSegmentToPath(base.getPath(), subPath));
            return base;
        }
    }

    private static String appendSegmentToPath(String path, String segment) {
        if (StringUtil.isBlank(path)) {
            path = "/";
        }

        if (path.charAt(path.length() - 1) == '/' || segment.startsWith("/")) {
            return path + segment;
        }

        return path + "/" + segment;
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
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_URL);
        DOAJFetcher.addPath(uriBuilder, new DefaultLuceneQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        // Number of results
        uriBuilder.addParameter("pageSize", "30");
        // Page (not needed so far)
        // uriBuilder.addParameter("page", "1");
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
