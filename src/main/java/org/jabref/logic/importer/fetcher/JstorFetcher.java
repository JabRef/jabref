package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Fetcher for jstor.org
 **/
public class JstorFetcher implements SearchBasedParserFetcher, FulltextFetcher {

    private static final String HOST = "https://www.jstor.org";
    private static final String SEARCH_HOST = HOST + "/open/search";
    private static final String CITE_HOST = HOST + "/citation/text";

    private final ImportFormatPreferences importFormatPreferences;

    public JstorFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_HOST);
        uriBuilder.addParameter("Query", query);
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getComplexQueryURL(ComplexSearchQuery complexSearchQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(SEARCH_HOST);
        StringBuilder stringBuilder = new StringBuilder();
        if (!complexSearchQuery.getDefaultFieldPhrases().isEmpty()) {
            stringBuilder.append(complexSearchQuery.getDefaultFieldPhrases());
        }
        if (!complexSearchQuery.getAuthors().isEmpty()) {
            for (String author : complexSearchQuery.getAuthors()) {
                stringBuilder.append("au:").append(author);
            }
        }
        if (!complexSearchQuery.getTitlePhrases().isEmpty()) {
            for (String title : complexSearchQuery.getTitlePhrases()) {
                stringBuilder.append("ti:").append(title);
            }
        }
        if (complexSearchQuery.getJournal().isPresent()) {
            stringBuilder.append("pt:").append(complexSearchQuery.getJournal().get());
        }
        if (complexSearchQuery.getSingleYear().isPresent()) {
            uriBuilder.addParameter("sd", String.valueOf(complexSearchQuery.getSingleYear().get()));
            uriBuilder.addParameter("ed", String.valueOf(complexSearchQuery.getSingleYear().get()));
        }
        if (complexSearchQuery.getFromYear().isPresent()) {
            uriBuilder.addParameter("sd", String.valueOf(complexSearchQuery.getFromYear().get()));
        }
        if (complexSearchQuery.getToYear().isPresent()) {
            uriBuilder.addParameter("ed", String.valueOf(complexSearchQuery.getToYear().get()));
        }

        uriBuilder.addParameter("Query", stringBuilder.toString());
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            List<BibEntry> entries;
            try {
                Document doc = Jsoup.parse(inputStream, null, HOST);
                List<Element> elements = doc.body().getElementsByClass("cite-this-item");
                StringBuilder stringBuilder = new StringBuilder();
                for (Element element : elements) {
                    String id = element.attr("href").replace("citation/info/", "");

                    String data = new URLDownload(CITE_HOST + id).asString();
                    stringBuilder.append(data);
                }
                BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
                entries = new ArrayList<>(parser.parseEntries(stringBuilder.toString()));
            } catch (IOException e) {
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
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        if (entry.getField(StandardField.URL).isEmpty()) {
            return Optional.empty();
        }

        String page = new URLDownload(entry.getField(StandardField.URL).get()).asString();

        Document doc = Jsoup.parse(page);

        List<Element> elements = doc.getElementsByAttribute("data-doi");
        if (elements.size() != 1) {
            return Optional.empty();
        }

        String url = elements.get(0).attr("href");
        return Optional.of(new URL(url));
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }
}
