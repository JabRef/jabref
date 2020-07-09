package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.formatter.bibtexfields.RemoveDigitsFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveRedundantSpacesFormatter;
import org.jabref.logic.formatter.bibtexfields.ReplaceTabsBySpaceFormater;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.http.client.utils.URIBuilder;

public class CollectionOfComputerScienceBibliographiesFetcher implements SearchBasedParserFetcher {

    private static final String BASIC_SEARCH_URL = "http://liinwww.ira.uka.de/bibliography/rss?";

    private final CollectionOfComputerScienceBibliographiesParser parser;

    public CollectionOfComputerScienceBibliographiesFetcher(ImportFormatPreferences importFormatPreferences) {
        this.parser = new CollectionOfComputerScienceBibliographiesParser(importFormatPreferences);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        return new URIBuilder(BASIC_SEARCH_URL)
            .addParameter("query", query)
            .addParameter("sort", "score")
            .build()
            .toURL();
    }

    @Override
    public Parser getParser() {
        return parser;
    }

    @Override
    public String getName() {
        return "Collection of Computer Science Bibliographies";
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(StandardField.ABSTRACT, new RemoveNewlinesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.ABSTRACT, new ReplaceTabsBySpaceFormater()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.ABSTRACT, new RemoveRedundantSpacesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.EDITOR, new RemoveDigitsFormatter()).cleanup(entry);
    }
}
