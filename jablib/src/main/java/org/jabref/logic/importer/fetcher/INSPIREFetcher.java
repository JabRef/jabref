package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;

/**
 * Fetches data from the INSPIRE database.
 */
public class INSPIREFetcher implements SearchBasedParserFetcher, EntryBasedFetcher {

    private static final String INSPIRE_HOST = "https://inspirehep.net/api/literature/";
    private static final String INSPIRE_DOI_HOST = "https://inspirehep.net/api/doi/";
    private static final String INSPIRE_ARXIV_HOST = "https://inspirehep.net/api/arxiv/";

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
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(INSPIRE_HOST);
        uriBuilder.addParameter("q", new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse(""));
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
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveEnclosingBracesFormatter()).cleanup(entry);

        new FieldFormatterCleanup(StandardField.TITLE, new LatexToUnicodeFormatter()).cleanup(entry);
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        Optional<String> archiveprefix = entry.getFieldOrAlias(StandardField.ARCHIVEPREFIX);
        Optional<String> eprint = entry.getField(StandardField.EPRINT);

        String urlString;
        if (archiveprefix.filter("arxiv"::equals).isPresent() && eprint.isPresent()) {
            urlString = INSPIRE_ARXIV_HOST + eprint.get();
        } else if (doi.isPresent()) {
            urlString = INSPIRE_DOI_HOST + doi.get();
        } else {
            return List.of();
        }

        URL url;
        try {
            url = new URI(urlString).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new FetcherException("Invalid URL", e);
        }

        try {
            URLDownload download = getUrlDownload(url);
            List<BibEntry> results = getParser().parseEntries(download.asInputStream());
            results.forEach(this::doPostCleanup);
            return results;
        } catch (ParseException e) {
            throw new FetcherException(url, e);
        }
    }
}
