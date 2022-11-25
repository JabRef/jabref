package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultLuceneQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

/**
 * Fetches data from the INSPIRE database.
 */
public class INSPIREFetcher implements SearchBasedParserFetcher {

    private static final String INSPIRE_HOST = "https://inspirehep.net/api/literature/";

    private final ImportFormatPreferences importFormatPreferences;

    public INSPIREFetcher(ImportFormatPreferences preferences) {
        this.importFormatPreferences = preferences;
    }

    @Override
    public String getName() {
        return "INSPIRE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_INSPIRE);
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(INSPIRE_HOST);
        uriBuilder.addParameter("q", new DefaultLuceneQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""));
        return uriBuilder.build().toURL();
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        URLDownload download = new URLDownload(url);
        download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);
        return download;
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Remove strange "SLACcitation" field
        new FieldFormatterCleanup(new UnknownField("SLACcitation"), new ClearFormatter()).cleanup(entry);

        // Remove braces around content of "title" field
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);

        new FieldFormatterCleanup(StandardField.TITLE, new LatexToUnicodeFormatter()).cleanup(entry);
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }
}
