package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EprintCleanupTest {

    private EprintCleanup cleanup = new EprintCleanup();

    @Test
    void cleanupCompleteEntry() {
        BibEntry input = new BibEntry()
                .withField(StandardField.JOURNALTITLE, "arXiv:1502.05795 [math]")
                .withField(StandardField.NOTE, "arXiv: 1502.05795")
                .withField(StandardField.URL, "http://arxiv.org/abs/1502.05795")
                .withField(StandardField.URLDATE, "2018-09-07TZ");

        BibEntry expected = new BibEntry()
                .withField(StandardField.EPRINT, "1502.05795")
                .withField(StandardField.EPRINTCLASS, "math")
                .withField(StandardField.EPRINTTYPE, "arxiv");

        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanupEntryWithVersionAndInstitutionAndEid() {
        BibEntry input = new BibEntry()
                .withField(StandardField.NOTE, "arXiv: 1503.05173")
                .withField(StandardField.VERSION, "1")
                .withField(StandardField.INSTITUTION, "arXiv")
                .withField(StandardField.EID, "arXiv:1503.05173");

        BibEntry expected = new BibEntry()
                .withField(StandardField.EPRINT, "1503.05173v1")
                .withField(StandardField.EPRINTTYPE, "arxiv");

        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanupEntryWithOtherInstitution() {
        BibEntry input = new BibEntry()
                .withField(StandardField.NOTE, "arXiv: 1503.05173")
                .withField(StandardField.VERSION, "1")
                .withField(StandardField.INSTITUTION, "OtherInstitution")
                .withField(StandardField.EID, "arXiv:1503.05173");

        BibEntry expected = new BibEntry()
                .withField(StandardField.EPRINT, "1503.05173v1")
                .withField(StandardField.EPRINTTYPE, "arxiv")
                .withField(StandardField.INSTITUTION, "OtherInstitution");

        cleanup.cleanup(input);

        assertEquals(expected, input);
    }

    @Test
    void cleanUpFieldArxiv() {
        BibEntry input = new BibEntry()
                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.MONTH, "jun")
                .withField(FieldFactory.parseField("arxiv"), "2506.05614");

        BibEntry expected = new BibEntry()
                .withField(StandardField.AUTHOR, "E. G. Santana Jr. and G. Benjamin and M. Araujo and H. Santos")
                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.MONTH, "jun")
                .withField(StandardField.EPRINT, "2506.05614")
                .withField(StandardField.EPRINTTYPE, "arxiv");

        cleanup.cleanup(input);

        assertEquals(expected, input);
    }
}
