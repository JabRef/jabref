package org.jabref.logic.cleanup;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EprintCleanupTest {

    private EprintCleanup cleanup = new EprintCleanup();

    @ParameterizedTest
    @MethodSource
    void cleanupTransformsEntries(BibEntry input, BibEntry expected) {
        cleanup.cleanup(input);
        assertEquals(expected, input);
    }

    static Stream<Arguments> cleanupTransformsEntries() {
        return Stream.of(
                // cleanupCompleteEntry
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.JOURNALTITLE, "arXiv:1502.05795 [math]")
                                .withField(StandardField.NOTE, "arXiv: 1502.05795")
                                .withField(StandardField.URL, "http://arxiv.org/abs/1502.05795")
                                .withField(StandardField.URLDATE, "2018-09-07TZ"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "1502.05795")
                                .withField(StandardField.EPRINTCLASS, "math")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // cleanupEntryWithVersionAndInstitutionAndEid
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.NOTE, "arXiv: 1503.05173")
                                .withField(StandardField.VERSION, "1")
                                .withField(StandardField.INSTITUTION, "arXiv")
                                .withField(StandardField.EID, "arXiv:1503.05173"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "1503.05173v1")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // cleanupEntryWithOtherInstitution
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.NOTE, "arXiv: 1503.05173")
                                .withField(StandardField.VERSION, "1")
                                .withField(StandardField.INSTITUTION, "OtherInstitution")
                                .withField(StandardField.EID, "arXiv:1503.05173"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "1503.05173v1")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                                .withField(StandardField.INSTITUTION, "OtherInstitution")
                ),

                // arXiv:1503.05173 in EPRINT field should be split-up
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.EPRINT, "arXiv:1503.05173"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "1503.05173")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // prefix exists but the value is empty
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.EPRINT, "arXiv:"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // prefix exists but the value is invalid data
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.EPRINT, "arXiv:junkData"),
                        new BibEntry()
                                .withField(StandardField.EPRINT, "junkData")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // Fields with other prefixes (non-arXiv) should remain unchanged
                Arguments.of(
                        new BibEntry().withField(StandardField.EPRINT, "other:12345"),
                        new BibEntry().withField(StandardField.EPRINT, "other:12345")
                ),

                // LLM-generated BibEntry with "arxiv" field
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                                .withField(StandardField.YEAR, "2025")
                                .withField(StandardField.MONTH, "jun")
                                .withField(FieldFactory.parseField("arxiv"), "2506.05614"),
                        new BibEntry()
                                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                                .withField(StandardField.YEAR, "2025")
                                .withField(StandardField.MONTH, "jun")
                                .withField(StandardField.EPRINT, "2506.05614")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                ),

                // LLM-generated BibEntry with journal "arxiv" and volume with arXiv ID
                Arguments.of(
                        new BibEntry()
                                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                                .withField(StandardField.YEAR, "2025")
                                .withField(StandardField.MONTH, "jun")
                                .withField(StandardField.JOURNAL, "arXiv")
                                .withField(StandardField.VOLUME, "2506.05614"),
                        new BibEntry()
                                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                                .withField(StandardField.YEAR, "2025")
                                .withField(StandardField.MONTH, "jun")
                                .withField(StandardField.EPRINT, "2506.05614")
                                .withField(StandardField.EPRINTTYPE, "arxiv")
                )
        );
    }
}
