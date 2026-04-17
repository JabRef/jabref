package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RelatedWorkReferenceResolverTest {

    @Test
    void parseReferencesReturnsExpectedReferenceCount() throws IOException, URISyntaxException {
        Path pdfPath = Path.of(RelatedWorkReferenceResolverTest.class
                .getResource("/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf")
                .toURI());
        LinkedFile linkedFile = new LinkedFile("", pdfPath, "PDF");
        RelatedWorkReferenceResolver resolver = new RelatedWorkReferenceResolver();

        Map<String, BibEntry> referencesByMarker = resolver.parseReferences(linkedFile, new BibDatabaseContext(), createFilePreferences());

        assertEquals(83, referencesByMarker.size());
    }

    @ParameterizedTest
    @MethodSource
    void parseReferences(String marker, String expectedComment) throws IOException, URISyntaxException {
        Path pdfPath = Path.of(RelatedWorkReferenceResolverTest.class
                .getResource("/org/jabref/logic/importer/fileformat/pdf/2024_SPLC_Becker.pdf")
                .toURI());
        LinkedFile linkedFile = new LinkedFile("", pdfPath, "PDF");
        RelatedWorkReferenceResolver resolver = new RelatedWorkReferenceResolver();

        Map<String, BibEntry> referencesByMarker = resolver.parseReferences(linkedFile, new BibDatabaseContext(), createFilePreferences());
        BibEntry reference = Optional.ofNullable(referencesByMarker.get(marker)).orElseThrow();

        assertEquals(expectedComment, reference.getField(StandardField.COMMENT).orElseThrow());
    }

    private static Stream<Arguments> parseReferences() {
        return Stream.of(
                Arguments.of(
                        "[1]",
                        "[1] Mathieu Acher, José Galindo Duarte, and Jean-Marc Jézéquel. 2023. On Program- ming Variability with Large Language Model-based Assistant. In Proc. of the 27th ACM International Systems and Software Product Line Conference-Volume A. ACM, 8–14."
                ),
                Arguments.of(
                        "[5]",
                        "[5] Wesley K. G. Assunção, Roberto E. Lopez-Herrejon, Lukas Linsbauer, Silvia R. Vergilio, and Alexander Egyed. 2017. Reengineering legacy applications into software product lines: a systematic mapping. Empir. Softw. Eng. 22, 6 (2017), 2972–3016."
                )
        );
    }

    private FilePreferences createFilePreferences() {
        return mock(FilePreferences.class);
    }
}
