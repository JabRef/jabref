package org.jabref.logic.importer.util;

import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlIdentifierParserTest {

    @Test
    void parseDOIFromPlainDOI() {
        String input = "10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIFromDoiOrgURL() {
        String input = "https://doi.org/10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIFromDxDoiOrgURL() {
        String input = "https://dx.doi.org/10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIFromHTTPURL() {
        String input = "http://doi.org/10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIFromACMDigitalLibrary() {
        String input = "https://dl.acm.org/doi/10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIFromACMAbsURL() {
        String input = "https://dl.acm.org/doi/abs/10.1145/3544548.3580995";
        assertTrue(UrlIdentifierParser.parseDOI(input).isPresent());
    }

    @Test
    void parseDOIReturnsEmptyForNull() {
        assertFalse(UrlIdentifierParser.parseDOI(null).isPresent());
    }

    @Test
    void parseDOIReturnsEmptyForEmptyString() {
        assertFalse(UrlIdentifierParser.parseDOI("").isPresent());
    }

    @Test
    void parseDOIReturnsEmptyForInvalidURL() {
        assertFalse(UrlIdentifierParser.parseDOI("https://example.com").isPresent());
    }

    @Test
    void parseArXivFromPlainID() {
        String input = "2203.02155";
        assertTrue(UrlIdentifierParser.parseArXiv(input).isPresent());
    }

    @Test
    void parseArXivFromAbsURL() {
        String input = "https://arxiv.org/abs/2203.02155";
        assertTrue(UrlIdentifierParser.parseArXiv(input).isPresent());
    }

    @Test
    void parseArXivFromPDFURL() {
        String input = "https://arxiv.org/pdf/2203.02155.pdf";
        assertTrue(UrlIdentifierParser.parseArXiv(input).isPresent());
    }

    @Test
    void parseArXivFromHTTPURL() {
        String input = "http://arxiv.org/abs/2203.02155";
        assertTrue(UrlIdentifierParser.parseArXiv(input).isPresent());
    }

    @Test
    void parseArXivReturnsEmptyForNull() {
        assertFalse(UrlIdentifierParser.parseArXiv(null).isPresent());
    }

    @Test
    void parseArXivReturnsEmptyForInvalidURL() {
        assertFalse(UrlIdentifierParser.parseArXiv("https://example.com").isPresent());
    }

    @Test
    void parseArXivHandlesOldIDFormat() {
        String input = "https://arxiv.org/abs/math.GT/0309136";
        assertTrue(UrlIdentifierParser.parseArXiv(input).isPresent());
    }
}
