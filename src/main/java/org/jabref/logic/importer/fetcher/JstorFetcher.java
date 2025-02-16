package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.JstorQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Fetcher for jstor.org
 **/
public class JstorFetcher implements SearchBasedParserFetcher, FulltextFetcher, IdBasedParserFetcher {

    private static final String HOST = "https://www.jstor.org";
    private static final String SEARCH_HOST = HOST + "/open/search";
    private static final String CITE_HOST = HOST + "/citation/text/";
    private static final String URL_QUERY_REGEX = "(?<=\\?).*";

    private final ImportFormatPreferences importFormatPreferences;

    public JstorFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_HOST);
        uriBuilder.addParameter("Query", new JstorQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws MalformedURLException {
        String start = "https://www.jstor.org/citation/text/";
        if (identifier.startsWith("http")) {
            identifier = identifier.replace("https://www.jstor.org/stable", "");
            identifier = identifier.replace("http://www.jstor.org/stable", "");
        }
        identifier = identifier.replaceAll(URL_QUERY_REGEX, "");

        if (identifier.contains("/")) {
            // if identifier links to a entry with a valid doi
            return URLUtil.create(start + identifier);
        }
        // else use default doi start.
        return URLUtil.create(start + "10.2307/" + identifier);
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            BibtexParser parser = new BibtexParser(importFormatPreferences);
            String text = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining());

            // does the input stream contain bibtex ?
            if (text.startsWith("@")) {
                return parser.parseEntries(text);
            }
            // otherwise: input stream contains html

            List<BibEntry> entries;
            try {
                Document doc = Jsoup.parse(inputStream, null, HOST);

                StringBuilder stringBuilder = new StringBuilder();
                List<Element> elements = doc.body().getElementsByClass("cite-this-item");
                for (Element element : elements) {
                    String id = element.attr("href").replace("citation/info/", "");

                    String data = new URLDownload(CITE_HOST + id).asString();
                    stringBuilder.append(data);
                }
                entries = new ArrayList<>(parser.parseEntries(stringBuilder.toString()));
            } catch (IOException | FetcherException e) {
                throw new ParseException("Could not download data from jstor.org", e);
            }
            return entries;
        };
    }

    @Override
    public String getName() {
        return "JSTOR";
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws FetcherException, IOException {
        if (entry.getField(StandardField.URL).isEmpty()) {
            return Optional.empty();
        }

        String page = new URLDownload(entry.getField(StandardField.URL).get()).asString();

        Document doc = Jsoup.parse(page);

        List<Element> elements = doc.getElementsByAttribute("data-doi");
        if (elements.size() != 1) {
            return Optional.empty();
        }

        String url = elements.getFirst().attr("href");
        return Optional.of(URLUtil.create(url));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }
}
