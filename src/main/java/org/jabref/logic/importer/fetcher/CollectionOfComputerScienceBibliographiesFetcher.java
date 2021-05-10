package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveDigitsFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveRedundantSpacesFormatter;
import org.jabref.logic.formatter.bibtexfields.ReplaceTabsBySpaceFormater;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.CollectionOfComputerScienceBibliographiesQueryTransformer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class CollectionOfComputerScienceBibliographiesFetcher implements SearchBasedParserFetcher {

    private static final String BASIC_SEARCH_URL = "http://liinwww.ira.uka.de/bibliography/rss?";

    private final CollectionOfComputerScienceBibliographiesParser parser;

    public CollectionOfComputerScienceBibliographiesFetcher(ImportFormatPreferences importFormatPreferences) {
        this.parser = new CollectionOfComputerScienceBibliographiesParser(importFormatPreferences);
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        return new URIBuilder(BASIC_SEARCH_URL)
                .addParameter("query", new CollectionOfComputerScienceBibliographiesQueryTransformer().transformLuceneQuery(luceneQuery).orElse(""))
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
        // identifier fields is a key-value field
        // example: "urn:isbn:978-1-4503-5217-8; doi:10.1145/3129790.3129810; ISI:000505046100032; Scopus 2-s2.0-85037741580"
        // thus, key can contain multiple ":"; sometimes value separated by " " instead of ":"
        UnknownField identifierField = new UnknownField("identifier");
        entry.getField(identifierField)
             .stream()
             .flatMap(value -> Arrays.stream(value.split("; ")))
             .forEach(identifierKeyValue -> {
                 // check for pattern "Scopus 2-..."
                 String[] identifierKeyValueSplit = identifierKeyValue.split(" ");
                 if (identifierKeyValueSplit.length == 1) {
                     // check for pattern "doi:..."
                     identifierKeyValueSplit = identifierKeyValue.split(":");
                 }
                 int length = identifierKeyValueSplit.length;
                 if (length < 2) {
                     return;
                 }
                 // in the case "urn:isbn:", just "isbn" is used
                 String key = identifierKeyValueSplit[length - 2];
                 String value = identifierKeyValueSplit[length - 1];
                 Field field = FieldFactory.parseField(key);
                 if (!entry.hasField(field)) {
                     entry.setField(field, value);
                 }
             });
        entry.clearField(identifierField);
    }
}
