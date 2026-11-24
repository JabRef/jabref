package org.jabref.logic.layout.format;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateBibORDFAuthorsTest {
    private static final Pattern AUTHOR_ID = Pattern.compile("author[0-9a-f-]{36}");

    @Test
    void formatsSingleAuthorAsSequence() {
        assertEquals("""
                <dcterms:creator><foaf:Person rdf:nodeID="author" foaf:name="Jane Doe"/></dcterms:creator>
                <bibo:authorList><rdf:Seq>
                <rdf:li rdf:nodeID="author"/>
                </rdf:Seq></bibo:authorList>
                """, normalizeIds(new CreateBibORDFAuthors().format("Doe, Jane")));
    }

    @Test
    void preservesOrderAndProtectedNames() {
        assertEquals("""
                <dcterms:creator><foaf:Person rdf:nodeID="author" foaf:name="Jane Doe"/></dcterms:creator>
                <dcterms:creator><foaf:Person rdf:nodeID="author" foaf:name="Research and Development"/></dcterms:creator>
                <bibo:authorList><rdf:Seq>
                <rdf:li rdf:nodeID="author"/>
                <rdf:li rdf:nodeID="author"/>
                </rdf:Seq></bibo:authorList>
                """, normalizeIds(new CreateBibORDFAuthors().format("Doe, Jane and {Research and Development}")));
    }

    @Test
    void escapesNamesForXmlAttributes() {
        assertEquals("""
                <dcterms:creator><foaf:Person rdf:nodeID="author" foaf:name="Smith &#x0026; Jones"/></dcterms:creator>
                <bibo:authorList><rdf:Seq>
                <rdf:li rdf:nodeID="author"/>
                </rdf:Seq></bibo:authorList>
                """, normalizeIds(new CreateBibORDFAuthors().format("{Smith & Jones}")));
    }

    private String normalizeIds(String result) {
        return AUTHOR_ID.matcher(result).replaceAll("author");
    }
}
