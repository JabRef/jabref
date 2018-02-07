package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.GvkParser;

import org.apache.http.client.utils.URIBuilder;

public class GvkFetcher implements SearchBasedParserFetcher {

    private static final String URL_PATTERN = "http://sru.gbv.de/gvk?";

    /**
     * Searchkeys are used to specify a search request. For example "tit" stands for "title".
     * If no searchkey is used, the default searchkey "all" is used.
     */
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
        String query = "";
        boolean lastWasNoKey = false;

        for (String key : queryList) {
            if (searchKeys.contains(key)) {
                if (lastWasNoKey) {
                    query = query + "and ";
                }
                query = query + "pica." + key + "=";
            } else {
                query = query + key + " ";
                lastWasNoKey = true;
            }
        }
        return query.trim();
    }

    protected String getSearchQueryString(String query) {
        Objects.requireNonNull(query);
        LinkedList<String> queryList = new LinkedList<>(Arrays.asList(query.split("\\s")));

        if (searchKeys.contains(queryList.get(0))) {
            return getSearchQueryStringForComplexQuery(queryList);
        } else {
            // query as pica.all
            return queryList.stream().collect(Collectors.joining(" ", "pica.all=", ""));
        }
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        String gvkQuery = getSearchQueryString(query);
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
    public Parser getParser() {
        return new GvkParser();
    }

}
