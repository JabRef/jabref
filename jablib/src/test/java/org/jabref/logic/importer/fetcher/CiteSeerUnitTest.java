package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CiteSeerUnitTest {

    private final CiteSeer fetcher = new CiteSeer();

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "  \t  "})
    void performRawSearchQueryWithBlankQueryReturnsEmpty(String blank) throws FetcherException {
        assertEquals(List.of(), fetcher.performRawSearchQuery(blank));
    }

    @Test
    void performSearchWithBlankQueryReturnsEmpty() throws FetcherException {
        assertEquals(List.of(), fetcher.performSearch(""));
    }
}

