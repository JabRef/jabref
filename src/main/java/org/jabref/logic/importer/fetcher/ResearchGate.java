package org.jabref.logic.importer.fetcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jabref.logic.importer.*;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ResearchGate implements FulltextFetcher, EntryBasedFetcher, SearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResearchGate.class);
    private static final String HOST = "https://www.researchgate.net/";
    private static final String GOOGLE_SEARCH = "https://www.google.com/search?q=";
    private static final String GOOGLE_SITE = "%20site:researchgate.net";
    private static final String SEARCH = "https://www.researchgate.net/search.Search.html?type=publication&query=";
//    private static final String SEARCH = "https://www.researchgate.net/search/publication?";

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
            Connection connection = Jsoup.connect(urlDownload.getSource().toString());
            Document html = connection
                    .userAgent(URLDownload.USER_AGENT)
                    .referrer("www.google.com")
                    .cookie("__cf_bm","N5oXjF8mMgVTJjSuKCzhOcaQujSqocqzpcDdXO1maCw-1647365228-0-AbE15gZUfv0Z7yieaG5ln35AFp+yLU8fEV+ruRMzD3tjR0CiZIwZR60iA6QWKPiQy46+uxVbPJeEnLFSkKG9nW8=")
//                    .cookie("cf_clearance", "_QvF8v6xVPvJqDRtl97MnBJxvWWo4jRAbtKLdD3CTuw-1646656607-0-250")
//                    .cookie("cili", "_2_ZjFmZWExMDA0NzViNGI3OGRiYWJhNjI0ODc5ZDE4ZTRmMWRlMjYyYmQyYTE1ODYwM2NmMDA5NmEzNDU5NjQ5M18zNjAyMTYwMDsw")
//                    .cookie("cirgu", "_1_rmlbZbgB3cmzxJPJNo9Ye//0bitKZxE8nES/e2ltjj0D6YRE1mR2V0uvxpuMxtCZV2sNgsT9")
//                    .cookie("classification", "institution")
                    .cookie("did", "0Z2LgmYoOAcOljjHyYTef0NDz11SyOxHB01JyB5hnuktaKtn8Sv13J4FZG1iiJdk; ptc=RG1.2339240692281222610.1645453991")
                    .cookie("isResearcher", "yes")
//                    .cookie("pl", "gzp4Is0JfoDeKqoEMmnt12Mfxac8VRqBltqtD9iIMeNPMEs3KpaTH7P2unhBQx8fWTag9UUXBwEcY3LoPnIXa5TMBOCCXOT41d4UvMTdpuPDl6PWgwLqabPY1aEc64GK")
//                    .cookie("ptc", "RG1.2339240692281222610.1645453991")
//                    .cookie("sid", "bHPY07xfFkDZ0OjZKhfHkdBLoMkvto8lbIqJ0gkhX1FUfr4RUPDW1U17BPZkTKUtj6gFnrv57IpvIClRZUffsrH3cKeWBNhJneUtpgM7VRqu1NtSYdxf23UE1qfsBTb6")
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
     *
     * @param luceneQuery the search query
     */
    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        String query = new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("");
        URIBuilder source;
        String result = "";
        source = new URIBuilder(SEARCH);
        source.addParameter("type", "publication");
        source.addParameter("query", query);
        try {
            result = getURLByString(query);
        } catch (IOException e) {
            throw new MalformedURLException();
        }
        LOGGER.trace("URL for query: {}", result);
        return new URL(result);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    /**
     * Returns the parser used to convert the response to a list of {@link BibEntry}.
     */
    @Override
    public Parser getParser() {
        // TODO: This is probably wrong, I just copied CrossRef. Is there a better, easier way to do it?
        return inputStream -> {

            LOGGER.debug("In parser");
            try {
                byte[] input = inputStream.readAllBytes();
//                LOGGER.debug("Answer: {}", new String(input, 0, input.length));
                BufferedWriter bw = new BufferedWriter(new FileWriter("/home/pelirrojito/Documents/JabRef/parser.html"));
                bw.write(new String(input, 0, input.length));
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONObject response = JsonReader.toJsonObject(inputStream);
            LOGGER.debug("Answer: {}", response);
            if (response.isEmpty()) {
                return Collections.emptyList();
            }

            response = response.getJSONObject("message");
            if (response.isEmpty()) {
                return Collections.emptyList();
            }

            if (!response.has("items")) {
                // Singleton response
                BibEntry entry = jsonItemToBibEntry(response);
                return Collections.singletonList(entry);
            }

            // Response contains a list
            JSONArray items = response.getJSONArray("items");
            List<BibEntry> entries = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++) {
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
            BibEntry entry = new BibEntry();
            LOGGER.debug("Item: {}", item.toString());
            // TODO: set type
            entry.setField(StandardField.TITLE,
                    Optional.ofNullable(item.optJSONArray("title"))
                            .map(array -> array.optString(0)).orElse(""));
            entry.setField(StandardField.SUBTITLE,
                    Optional.ofNullable(item.optJSONArray("subtitle"))
                            .map(array -> array.optString(0)).orElse(""));
            // TODO: add Author
//            entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("author")));
            entry.setField(StandardField.YEAR,
                    Optional.ofNullable(item.optJSONObject("published-print"))
                            .map(array -> array.optJSONArray("date-parts"))
                            .map(array -> array.optJSONArray(0))
                            .map(array -> array.optInt(0))
                            .map(year -> Integer.toString(year)).orElse("")
            );
            entry.setField(StandardField.DOI, item.getString("DOI"));
            entry.setField(StandardField.PAGES, item.optString("page"));
            entry.setField(StandardField.VOLUME, item.optString("volume"));
            entry.setField(StandardField.ISSN, Optional.ofNullable(item.optJSONArray("ISSN")).map(array -> array.getString(0)).orElse(""));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("ResearchGate API JSON format has changed", exception);
        }
    }

    /**
     * Returns the localized name of this fetcher.
     * The title can be used to display the fetcher in the menu and in the side pane.
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
 */
