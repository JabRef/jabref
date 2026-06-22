package org.jabref.logic.cleanup;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArXivDoiCleanupTest {

    @ParameterizedTest
    @MethodSource
    void cleanup(BibEntry expected, BibEntry input) {
        new ArXivDoiCleanup().cleanup(input);
        assertEquals(expected, input);
    }

    private static Stream<Arguments> cleanup() {
        return Stream.of(
                // Rule 1: eprint present, no DOI -> create the DOI from the eprint, drop the now-redundant eprint
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arxiv.2106.12345"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv")),

                // Rule 2: arXiv DOI and matching eprint -> keep DOI only, drop redundant eprint fields
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.2106.12345"),
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.2106.12345")
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                                .withField(StandardField.EPRINTCLASS, "cs.DL")),

                // Rule 2: versioned eprint still matches the (unversioned) arXiv DOI
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.2106.12345"),
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.2106.12345")
                                .withField(StandardField.EPRINT, "2106.12345v2")
                                .withField(StandardField.EPRINTTYPE, "arxiv")),

                // Rule 3: a real (non-arXiv) DOI alongside an arXiv eprint -> keep both
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.DOI, "10.1145/2594455")
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv"),
                        new BibEntry()
                                .withField(StandardField.DOI, "10.1145/2594455")
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv")),

                // Rule 3: arXiv DOI and arXiv eprint with different IDs -> keep both
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.1111.22222")
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv"),
                        new BibEntry()
                                .withField(StandardField.DOI, "10.48550/arXiv.1111.22222")
                                .withField(StandardField.EPRINT, "2106.12345")
                                .withField(StandardField.EPRINTTYPE, "arxiv")),

                // No arXiv data at all -> unchanged
                Arguments.of(
                        new BibEntry().withField(StandardField.DOI, "10.1145/2594455"),
                        new BibEntry().withField(StandardField.DOI, "10.1145/2594455")));
    }
}
