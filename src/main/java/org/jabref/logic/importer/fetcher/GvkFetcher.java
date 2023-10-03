package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.GVKQueryTransformer;
import org.jabref.logic.importer.fileformat.PicaXmlParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class GvkFetcher extends AbstractIsbnFetcher implements SearchBasedParserFetcher {

    private static final String URL_PATTERN = "https://sru.k10plus.de/opac-de-627?";

    /**
     * Searchkeys are used to specify a search request. For example "tit" stands for "title".
     * If no searchkey is used, the default searchkey "all" is used.
     */
    private final Collection<String> searchKeys = Arrays.asList("all", "tit", "per", "thm", "slw", "txt", "num", "kon", "ppn", "bkl", "erj");

    public GvkFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "GVK";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_GVK);
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", new GVKQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        uriBuilder.addParameter("maximumRecords", "50");
        uriBuilder.addParameter("recordSchema", "picaxml");
        uriBuilder.addParameter("sortKeys", "Year,,1");

        LOGGER.debug("Using URL {}", uriBuilder.build());
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        this.ensureThatIsbnIsValid(identifier);
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", "pica.isb=" + identifier);
        uriBuilder.addParameter("maximumRecords", "50");
        uriBuilder.addParameter("recordSchema", "picaxml");
        uriBuilder.addParameter("sortKeys", "Year,,1");

        LOGGER.debug("Using URL {}", uriBuilder.build());
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new PicaXmlParser();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        super.doPostCleanup(entry);

        // Fetcher returns page numbers as "30 Seiten" -> remove every non-digit character in the PAGETOTAL field
        entry.getField(StandardField.PAGETOTAL).ifPresent(pages ->
                entry.setField(StandardField.PAGETOTAL, pages.replaceAll("[\\D]", "")));
        new FieldFormatterCleanup(StandardField.PAGETOTAL, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);
    }
}
