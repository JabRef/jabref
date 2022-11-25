package org.jabref.logic.importer.util;

import org.jabref.model.entry.identifier.DOI;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@FetcherTest
class ShortDOIServiceTest {

    private final DOI doi = new DOI("10.1109/ACCESS.2013.2260813");
    private final DOI notExistingDoi = new DOI("10.1109/ACCESS.2013.226081400");

    private ShortDOIService sut;

    @BeforeEach
    void setUp() {
        sut = new ShortDOIService();
    }

    @Test
    void getShortDOI() throws ShortDOIServiceException {
        DOI shortDoi = sut.getShortDOI(doi);

        assertEquals("10/gf4gqc", shortDoi.getDOI());
    }

    @Test
    void shouldThrowExceptionWhenDOIWasNotFound() throws ShortDOIServiceException {
        assertThrows(ShortDOIServiceException.class, () -> sut.getShortDOI(notExistingDoi));
    }
}
