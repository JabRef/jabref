package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.entry.identifier.DOI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfUtilsTest {

    @Test
    void getFirstDoiFindsDoiContainedInPdf() throws Exception {
        Path file = Path.of(PdfUtilsTest.class.getResource("/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf").toURI());
        assertEquals(Optional.of(new DOI("10.1145/3646548.3672587")), PdfUtils.getFirstDoi(file));
    }

    @Test
    void getFirstDoiReturnsEmptyWhenPdfHasNoDoi() throws Exception {
        Path file = Path.of(PdfUtilsTest.class.getResource("/org/jabref/logic/importer/fileformat/empty.pdf").toURI());
        assertEquals(Optional.empty(), PdfUtils.getFirstDoi(file));
    }
}
