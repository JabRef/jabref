package org.jabref.model.search.query;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchResultTest {

    private static final String CONTENT = "This sentence, comma included. THIS SENTENCE, COMMA INCLUDED.";

    @Test
    void caseInsensitiveTermHighlightsAllSpellings() {
        SearchResult result = new SearchResult("file.pdf", CONTENT, "", 1, new TermQuery(new Term("content", "comma")));
        assertEquals(List.of("This sentence, <b>comma</b> included. THIS SENTENCE, <b>COMMA</b> INCLUDED."), result.getContentResultStringsHtml());
    }

    @Test
    void caseSensitiveTermHighlightsOnlyThatSpelling() {
        SearchResult result = new SearchResult("file.pdf", CONTENT, "", 1, new TermQuery(new Term("contentCaseSensitive", "comma")));
        assertEquals(List.of("This sentence, <b>comma</b> included. THIS SENTENCE, COMMA INCLUDED."), result.getContentResultStringsHtml());
    }

    @Test
    void compoundQueryHighlightsBothKindsOfTerms() {
        BooleanQuery query = new BooleanQuery.Builder()
                .add(new TermQuery(new Term("content", "sentenc")), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term("contentCaseSensitive", "COMMA")), BooleanClause.Occur.MUST)
                .build();
        SearchResult result = new SearchResult("file.pdf", CONTENT, "", 1, query);
        assertEquals(List.of(
                "This <b>sentence</b>, comma included. THIS <b>SENTENCE</b>, COMMA INCLUDED.",
                "This sentence, comma included. THIS SENTENCE, <b>COMMA</b> INCLUDED."), result.getContentResultStringsHtml());
    }

    @Test
    void caseSensitiveAnnotationTermHighlightsOnlyThatSpelling() {
        SearchResult result = new SearchResult("file.pdf", "", "Hello HELLO", 1, new TermQuery(new Term("annotationsCaseSensitive", "Hello")));
        assertEquals(List.of("<b>Hello</b> HELLO"), result.getAnnotationsResultStringsHtml());
    }
}
