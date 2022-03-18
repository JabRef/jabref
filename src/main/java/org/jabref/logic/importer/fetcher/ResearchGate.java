package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.layout.format.RTFChars;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResearchGate implements FulltextFetcher, EntryBasedFetcher, SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResearchGate.class);
    private static final String HOST = "https://www.researchgate.net/";
    private static final String GOOGLE_SEARCH = "https://www.google.com/search?q=";
    private static final String GOOGLE_SITE = "%20site:researchgate.net";
    private static final String SEARCH = "https://www.researchgate.net/search.Search.html?";
    private static final String SEARCH_FOR_BIB_ENTRY = "https://www.researchgate.net/lite.publication.PublicationDownloadCitationModal.downloadCitation.html?fileType=BibTeX&citation=citationAndAbstract&publicationUid=";
    private final ImportFormatPreferences formatPreferences;

    public ResearchGate(ImportFormatPreferences importFormatPreferences) {
        this.formatPreferences = importFormatPreferences;
    }

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     * <p>
     * Search by title first, as DOI is not searchable directly. When the title is not present, the search is made with DOI via google.com with site:researchgate.net
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws IOException      if an IO operation has failed
     * @throws FetcherException if the ResearchGate refuses to serve the page
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);

        // DOI search
        Optional<String> title = entry.getField(StandardField.TITLE);
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        // Retrieve PDF link
        String linkForSearch;
        Document html;
        try {
            if (title.isPresent()) {
                LOGGER.trace("Search by Title");
                linkForSearch = getURLByString(title.get());
                Connection connection = Jsoup.connect(linkForSearch);
                html = connection
                        .cookieStore(connection.cookieStore())
                        .userAgent(URLDownload.USER_AGENT)
                        .referrer("www.google.com")
                        .ignoreHttpErrors(true)
                        .get();
            } else if (doi.isPresent()) {
                LOGGER.trace("Search by DOI");
                // Retrieve PDF link
                Connection connection = Jsoup.connect(getURLByDoi(doi.get()));
                html = connection
                        .cookieStore(connection.cookieStore())
                        .userAgent(URLDownload.USER_AGENT)
                        .ignoreHttpErrors(true)
                        .get();
            } else {
                return Optional.empty();
            }
        } catch (FetcherException | NullPointerException e) {
            LOGGER.debug("ResearchGate server is not available");
            return Optional.empty();
        }
        Elements eLink = html.getElementsByTag("section");
        String link = eLink.select("a[href^=https]").select("a[href$=.pdf]").attr("href");
        LOGGER.debug("PDF link: {}", link);

        if (link.contains("researchgate.net")) {
            return Optional.of(new URL(link));
        }
        return Optional.empty();
    }

    String getURLByString(String query) throws IOException, FetcherException, NullPointerException {
        URIBuilder source;
        String link;
        try {
            source = new URIBuilder(SEARCH);
            source.addParameter("type", "publication");
            source.addParameter("query", query);

            URLDownload urlDownload = new URLDownload(source.toString());
            urlDownload.getCookieFromUrl();
            Document html = Jsoup.connect(source.toString())
                                 .userAgent(URLDownload.USER_AGENT)
                                 .referrer("www.google.com")
                                 .ignoreHttpErrors(true)
                                 .get();

            link = HOST + Objects.requireNonNull(html.getElementById("content"))
                                 .select("a[href^=publication/]")
                                 .attr("href");
            if (link.contains("?")) {
                link = link.substring(0, link.indexOf("?"));
            }
        } catch (URISyntaxException e) {
            return null;
        }
        LOGGER.trace("URL for page: {}", link);
        return link;
    }

    String getURLByDoi(DOI doi) throws IOException, FetcherException, NullPointerException {
        URIBuilder source;
        String link;
        try {

            source = new URIBuilder(SEARCH);
            source.addParameter("type", "publication");
            source.addParameter("query", doi.getDOI());

            source = new URIBuilder(GOOGLE_SEARCH + doi.getDOI() + GOOGLE_SITE);
            Connection connection = Jsoup.connect(source.toString());
            Document html = connection
                    .cookieStore(connection.cookieStore())
                    .userAgent(URLDownload.USER_AGENT)
                    .ignoreHttpErrors(true)
                    .get();

            link = Objects.requireNonNull(html.getElementById("search"))
                          .select("a").attr("href");
        } catch (URISyntaxException e) {
            return null;
        }
        LOGGER.trace("URL for page: {}", link);
        return link;
    }

    /**
     * Constructs a URL based on the query, size and page number.
     * <p>
     * Extract the numerical internal ID and add it to the URL to receive a link to a {@link BibEntry}
     *
     * @param luceneQuery the search query.
     * @return A URL that lets us download a .bib file
     * @throws URISyntaxException from {@link URIBuilder}'s build() method
     * @throws IOException        from {@link Connection}'s get() method
     */
    private Document getPage(QueryNode luceneQuery) throws URISyntaxException, IOException {
        String query = new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("");
        URIBuilder source = new URIBuilder(SEARCH);
        source.addParameter("type", "publication");
        source.addParameter("query", query);
        return Jsoup.connect(source.build().toString())
                    .userAgent(URLDownload.USER_AGENT)
                    .referrer("www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param luceneQuery the root node of the lucene query
     * @return a list of {@link BibEntry}, which are matched by the query (maybe empty)
     * @throws FetcherException if the ResearchGate refuses to serve the page
     */
    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        Document html;
        try {
            html = getPage(luceneQuery);
            // ResearchGate's server blocks when too many request are made
            if (!html.getElementsByClass("nova-legacy-v-publication-item__title").hasText()) {
                throw new FetcherException("ResearchGate server unavailable");
            }
        } catch (URISyntaxException | IOException e) {
            throw new FetcherException("URL is not correct", e);
        }

        Elements sol = html.getElementsByClass("nova-legacy-v-publication-item__title");
        List<String> urls = sol.select("a").eachAttr("href").stream()
                               .filter(stream -> stream.contains("publication/"))
                               .map(resultStream -> resultStream.substring(resultStream.indexOf("publication/") + 12, resultStream.indexOf("_")))
                               .map(idStream -> SEARCH_FOR_BIB_ENTRY + idStream)
                               .map(this::getInputStream)
                               .filter(Objects::nonNull)
                               .map(stream -> stream.lines().collect(Collectors.joining(OS.NEWLINE)))
                               .toList();

        List<BibEntry> list = new ArrayList<>();
        for (String bib : urls) {
            BibtexParser parser = new BibtexParser(formatPreferences, new DummyFileUpdateMonitor());
            Optional<BibEntry> entry;
            try {
                entry = parser.parseSingleEntry(bib);
                entry.ifPresent(list::add);
            } catch (ParseException e) {
                LOGGER.debug("Entry is not convertible to Bibtex", e);
            }
        }
        return list;
    }

    private BufferedReader getInputStream(String urlString) {
        try {
            URL url = new URL((urlString));
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            LOGGER.debug("Wrong URL:", e);
        }
        return null;
    }

    @Override
    public String getName() {
        return "ResearchGate";
    }

    /**
     * Looks for hits which are matched by the given {@link BibEntry}.
     *
     * @param entry entry to search bibliographic information for
     * @return a list of {@link BibEntry}, which are matched by the query (maybe empty)
     * @throws FetcherException if the ResearchGate refuses to serve the page
     */
    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getTitle();
        if (title.isEmpty()) {
            return new ArrayList<>();
        }
        return performSearch(new RTFChars().format(title.get()));
    }
}

