package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.util.GVKParser;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.xml.sax.SAXException;

public class GvkFetcher implements SearchBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(GvkFetcher.class);

    private final Collection<String> searchKeys = Arrays.asList("all", "tit", "per", "thm", "slw", "txt", "num", "kon", "ppn", "bkl", "erj");

    @Override
    public String getName() {
        return "GVK";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_GVK;
    }

    private String getSearchQueryStringForComplexQuery(List<String> queryList) {
        StringJoiner gvkQueryJoiner = new StringJoiner(" and ");
        if (!searchKeys.contains(queryList.get(0))) {
            throw new IllegalStateException("First element of query list has to be a searchKey");
        };

        Iterator<String> iterator = queryList.iterator();
        String query = iterator.next();
        // query is a searchKey
        while (iterator.hasNext()) {
            String searchKey = query;
            StringJoiner parameterJoiner = new StringJoiner(" ");
            while (iterator.hasNext() && (!searchKeys.contains((query = iterator.next())))) {
                parameterJoiner.add(query);
            }
            gvkQueryJoiner.add("pica.".concat(searchKey).concat("=").concat(parameterJoiner.toString()));
        }

        return gvkQueryJoiner.toString();
    }

    String getSearchQueryString(String query) {
        Objects.requireNonNull(query);
        LinkedList<String> queryList = new LinkedList(Arrays.asList(query.split("\\s")));

        if (queryList.isEmpty()) {
            return "";
        }

        String gvkQuery;
        if (searchKeys.contains(queryList.get(0))) {
            gvkQuery = getSearchQueryStringForComplexQuery(queryList);
        } else {
            // query as pica.all
            gvkQuery = queryList.stream().collect(Collectors.joining(" ", "pica.all=", ""));
        }

        return gvkQuery;
    }

    URL getQueryURL(String query) throws URISyntaxException, MalformedURLException {
        String gvkQuery = getSearchQueryString(query);
        final String URL_PATTERN = "http://sru.gbv.de/gvk?";
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", gvkQuery);
        uriBuilder.addParameter("maximumRecords", "50");
        uriBuilder.addParameter("recordSchema", "picaxml");
        uriBuilder.addParameter("sortKeys", "Year,,1");
        return uriBuilder.build().toURL();
    }


    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<BibEntry> result;

        try {
            try (InputStream is = getQueryURL(query).openStream()) {
                result = (new GVKParser()).parseEntries(is);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("URI malformed error", e);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.error("An I/O exception occurred", e);
            return Collections.emptyList();
        } catch (SAXException | ParserConfigurationException e) {
            LOGGER.error("An internal parser error occurred", e);
            return Collections.emptyList();
        }

        if (result.isEmpty()) {
            LOGGER.info("No references found");
        }

        return result;
    }

}


