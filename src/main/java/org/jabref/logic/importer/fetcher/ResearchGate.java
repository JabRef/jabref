package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
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
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws IOException          if an IO operation has failed
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // DOI search
        Optional<String> title = entry.getField(StandardField.TITLE);
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        // Retrieve PDF link
        String linkForSearch;
        Document html;
        if (title.isPresent()) {
            LOGGER.debug("Search by Title");
            linkForSearch = getURLByString(title.get());
            Connection connection = Jsoup.connect(linkForSearch);
            html = connection
                    .cookieStore(connection.cookieStore())
                    .userAgent(URLDownload.USER_AGENT)
                    .referrer("www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
        } else if (doi.isPresent()) {
            LOGGER.debug("Search by DOI");
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
        Elements eLink = html.getElementsByTag("section");
        String link = eLink.select("a[href^=https]").select("a[href$=.pdf]").attr("href");
        LOGGER.trace("PDF link: {}", link);

        if (link.contains("researchgate.net")) {
            return Optional.of(new URL(link));
        }
        return Optional.empty();
    }

    String getURLByString(String query) throws IOException {
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

            // TODO
            BufferedWriter bw = new BufferedWriter(new FileWriter("/home/pelirrojito/Documents/JabRef/content.html"));
            bw.write(html.html());
            bw.close();

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

    public String getURLByDoi(DOI doi) throws IOException {
        URIBuilder source;
        String link;
        try {

            source = new URIBuilder(SEARCH);
            source.addParameter("type", "publication");
            source.addParameter("query", doi.getDOI());
            // TODO: choose one

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
     */
    public Document getPage(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        String query = new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("");
        URIBuilder source = new URIBuilder(SEARCH);
        source.addParameter("type", "publication");
        source.addParameter("query", query);
        try {
            return Jsoup.connect(source.build().toString())
                        .userAgent(URLDownload.USER_AGENT)
                        .referrer("www.google.com")
                        .ignoreHttpErrors(true)
                        .get();
        } catch (IOException e) {
            throw new MalformedURLException();
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param luceneQuery the root node of the lucene query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        Document html = null;
        try {
            html = getPage(luceneQuery);
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }

        assert html != null;
        Elements sol = html.getElementsByClass("nova-legacy-v-publication-item__title");

        List<BibEntry> list = new ArrayList<>();
        List<String> urls = sol.select("a").eachAttr("href").stream()
                               .filter(stream -> stream.contains("publication/"))
                               .map(resultStream -> resultStream.substring(resultStream.indexOf("publication/") + 12, resultStream.indexOf("_")))
                               .map(idStream -> SEARCH_FOR_BIB_ENTRY + idStream)
                               .map(this::getInputStream)
                               .filter(Objects::nonNull)
                               .map(stream -> stream.lines().collect(Collectors.joining(OS.NEWLINE)))
                               .toList();

        for (String bib : urls) {
            BibtexParser parser = new BibtexParser(formatPreferences, new DummyFileUpdateMonitor());
            Optional<BibEntry> entry;
            try {
                entry = parser.parseSingleEntry(bib);
                entry.ifPresent(list::add);
            } catch (ParseException e) {
                LOGGER.trace("Entry is not convertible to Bibtex", e);
            }
        }
        return list;
    }

    private BufferedReader getInputStream(String urlString) {
        try {
            URL url = new URL((urlString));
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            LOGGER.debug("False URL:", e);
        }
        return null;
    }

    /**
     * Returns the localized name of this fetcher. The title can be used to display the fetcher in the menu and in the side pane.
     *
     * @return the localized name
     */
    @Override
    public String getName() {
        return "ResearchGate";
    }

    /**
     * Looks for hits which are matched by the given {@link BibEntry}.
     *
     * @param entry entry to search bibliographic information for
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getTitle();
        if (title.isEmpty()) {
            return new ArrayList<>();
        }
        return performSearch(title.get());
    }
}

/*
  https://www.researchgate.net/publication/4207355_Paranoid_a_global_secure_file_access_control_system/citation/download
  https://www.researchgate.net/publication/262225579_Slice_theorem_for_Frechet_group_actions_and_covariant_symplectic_field_theory/citation/download
  https://www.researchgate.net/publication/262225579_Slice_theorem_for_Frechet_group_actions_and_covariant_symplectic_field_theory

  https://www.researchgate.net/literature.AjaxLiterature.downloadCitation.html?publicationUid=262225579&fileType=BibTeX&citation=citationAndAbstract
  https://www.researchgate.net/lite.publication.PublicationDownloadCitationModal.downloadCitation.html?fileType=BibTeX&citation=citationAndAbstract&publicationUid=262225579

  */
