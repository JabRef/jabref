package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.EPRINT;

public class SemanticScholar implements FulltextFetcher, PagedSearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticScholar.class);

    private static final String SOURCE_ID_SEARCH = "https://api.semanticscholar.org/v1/paper/";
    private static final String SOURCE_WEB_SEARCH = "https://api.semanticscholar.org/graph/v1/paper/search?";
    private int PAGE_SIZE = PagedSearchBasedParserFetcher.super.getPageSize();

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     * <p>
     * Currently, only uses the DOI or arXiv identifier if found.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws IOException          if one of the URL is not correct
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        Optional<ArXivIdentifier> arXiv = entry.getField(EPRINT).flatMap(ArXivIdentifier::parse);

        Document html = null;
        if (doi.isPresent()) {
            try {
                // Retrieve PDF link
                String source = SOURCE_ID_SEARCH + doi.get().getDOI();
                html = Jsoup.connect(getURLBySource(source))
                        .userAgent(URLDownload.USER_AGENT)
                        .referrer("https://www.google.com")
                        .ignoreHttpErrors(true)
                        .get();
            } catch (IOException e) {
                LOGGER.info("Error for pdf lookup with DOI");
            }
        }
        if (arXiv.isPresent() && entry.getField(EPRINT).isPresent()) {
            // Check if entry is a match
            String arXivString = entry.getField(EPRINT).get();
            if (!arXivString.startsWith("arXiv:")) {
                arXivString = "arXiv:" + arXivString;
            }
            String source = SOURCE_ID_SEARCH + arXivString;
            html = Jsoup.connect(getURLBySource(source))
                    .userAgent(URLDownload.USER_AGENT)
                    .referrer("https://www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
        }
        if (html == null) {
            return Optional.empty();
        }

        // Retrieve PDF link from button on the webpage
        // First checked is a drop-down menu, as it has the correct URL if present
        Elements metaLinks = html.getElementsByClass("flex-item alternate-sources__dropdown");
        // TODO verbessern
        String link = metaLinks.select("a[href^=https]").select("a[href$=.pdf]").attr("href");
        link = metaLinks.select("a").attr("href");
        if (link.length() < 10) {
            metaLinks = html.getElementsByClass("flex-paper-actions__button--primary");
            link = metaLinks.select("a").attr("href");
        }
        LOGGER.info("Fulltext PDF found @ SemanticScholar.");
        return Optional.of(new URL(link));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    /**
     * Finds the URL for the webpage to look for the link to download the pdf.
     *
     * @param source the API link that contains the relevant information.
     * @return the correct URL
     * @throws IOException if error by downloading the page
     */
    public String getURLBySource(String source) throws IOException {
        URLDownload download = new URLDownload(source);
        JSONObject json = new JSONObject(download.asString());
        LOGGER.debug(json.get("url").toString());
        return json.get("url").toString();
    }

    /**
     * Constructs a URL based on the query, size and page number.
     *
     * @param luceneQuery the search query
     * @param pageNumber  the number of the page indexed from 0
     */
    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SOURCE_WEB_SEARCH);
        uriBuilder.addParameter("query", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        uriBuilder.addParameter("offset", String.valueOf(pageNumber * getPageSize()));
        uriBuilder.addParameter("limit", String.valueOf(Math.min(getPageSize(), 10000 - pageNumber * getPageSize())));
        // All fields need to be specified
        uriBuilder.addParameter("fields", "paperId,externalIds,url,title,abstract,venue,year,authors");
        LOGGER.debug(uriBuilder.build().toURL().toString());
        return uriBuilder.build().toURL();
    }

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    @Override
    public Parser getParser() {
        return inputStream -> {

            JSONObject response = JsonReader.toJsonObject(inputStream);
            LOGGER.debug(response.toString());
            if (response.isEmpty()) {
                return Collections.emptyList();
            }

            int total = response.getInt("total");
            if (total == 0) {
                return Collections.emptyList();
            } else if (response.has("next")) {
                total = Math.min(total, response.getInt("next") - response.getInt("offset"));
            }

//            if (!response.has("data")) {
//                // Singleton response
//                BibEntry entry = jsonItemToBibEntry(response);
//                return Collections.singletonList(entry);
//            }

            // Response contains a list
            JSONArray items = response.getJSONArray("data");
            List<BibEntry> entries = new ArrayList<>(items.length());
            for (int i = 0; i < total; i++) {
                JSONObject item = items.getJSONObject(i);
                BibEntry entry = jsonItemToBibEntry(item);
                entries.add(entry);
            }

            return entries;
        };
    }

    /**
     * This is copy-paste from CrossRef, need to be checked.
     *
     * @param item an entry received, needs to be parsed into a BibEntry
     * @return The BibEntry that corresponds to the received object
     */
    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry(StandardEntryType.Article);
            entry.setField(StandardField.URL, item.optString("url"));
            entry.setField(StandardField.TITLE, item.optString("title"));
            entry.setField(StandardField.ABSTRACT, item.optString("abstract"));
            entry.setField(StandardField.VENUE, item.optString("venue"));
            entry.setField(StandardField.YEAR, item.optString("year"));

            entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("authors")));

            // TODO: check. I'm not sure about the EPRINT and arXiv fields
            JSONObject externalIds = item.optJSONObject("externalIds");
            entry.setField(StandardField.DOI, externalIds.optString("DOI"));
            if (externalIds.has("ArXiv")) {
                entry.setField(EPRINT, externalIds.getString("ArXiv"));
                entry.setField(StandardField.ARCHIVEPREFIX, "arXiv");
            }
            entry.setField(StandardField.PMID, externalIds.optString("PubMed"));
//            exIds.addAll(externalIds.keySet());
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("SemanticScholar API JSON format has changed", exception);
        }
    }

    private String toAuthors(JSONArray author) {
        if (author == null || author.isEmpty()) {
            return "";
        }
        StringBuilder authors = new StringBuilder();
        for (int i = 0; i < author.length(); i++) {
            JSONObject item = author.getJSONObject(i);
            authors.append(item.getString("name")).append(" and ");
        }
        authors.delete(authors.length() - 5, authors.length());
        return authors.toString();
    }

    /**
     * Returns the localized name of this fetcher.
     * The title can be used to display the fetcher in the menu and in the side pane.
     *
     * @return the localized name
     */
    @Override
    public String getName() {
        return "SemanticScholar";
    }

    public void setPageSize(int size) {
        PAGE_SIZE = size;
    }

    @Override
    public int getPageSize() {
        return PAGE_SIZE;
    }
}

