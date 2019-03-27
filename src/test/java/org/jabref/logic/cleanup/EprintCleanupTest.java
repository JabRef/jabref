package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EprintCleanupTest {

    @Test
    void cleanupCompleteEntry() {
        BibEntry input = new BibEntry()
                .withField("journaltitle", "arXiv:1502.05795 [math]")
                .withField("note", "arXiv: 1502.05795")
                .withField("url", "http://arxiv.org/abs/1502.05795")
                .withField("urldate", "2018-09-07TZ");

        BibEntry expected = new BibEntry()
                .withField("eprint", "1502.05795")
                .withField("eprintclass", "math")
                .withField("eprinttype", "arxiv");

        EprintCleanup cleanup = new EprintCleanup();
        cleanup.cleanup(input);

        assertEquals(expected, input);
    }
}
