package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EprintCleanupTest {

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

        EprintCleanup cleanup = new EprintCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }
}
