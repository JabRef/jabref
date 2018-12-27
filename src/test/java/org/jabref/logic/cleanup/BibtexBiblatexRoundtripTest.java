package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibtexBiblatexRoundtripTest {
    private BibEntry bibtex;
    private BibEntry biblatex;

    @BeforeEach
    void setUp() {
        bibtex = new BibEntry("article");
        bibtex.setField("author", "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.");
        bibtex.setField("title", "The hook graphs of the symmetric groups");
        bibtex.setField("journal", "Canadian J. Math.");
        bibtex.setField("fjournal", "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques");
        bibtex.setField("volume", "6");
        bibtex.setField("year", "1954");
        bibtex.setField("pages", "316--324");
        bibtex.setField("issn", "0008-414X");
        bibtex.setField("mrclass", "20.0X");
        bibtex.setField("mrnumber", "0062127");
        bibtex.setField("mrreviewer", "D. E. Littlewood");

        biblatex = new BibEntry("article");
        biblatex.setField("author", "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.");
        biblatex.setField("title", "The hook graphs of the symmetric groups");
        biblatex.setField("journaltitle", "Canadian J. Math.");
        biblatex.setField("fjournal", "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques");
        biblatex.setField("volume", "6");
        biblatex.setField("date", "1954");
        biblatex.setField("pages", "316--324");
        biblatex.setField("issn", "0008-414X");
        biblatex.setField("mrclass", "20.0X");
        biblatex.setField("mrnumber", "0062127");
        biblatex.setField("mrreviewer", "D. E. Littlewood");
    }

    @Test
    void roundTripBibtexToBiblatexIsIdentity() {
        BibEntry clone = (BibEntry) bibtex.clone();
        new ConvertToBiblatexCleanup().cleanup(clone);
        assertEquals(biblatex, clone);
        new ConvertToBibtexCleanup().cleanup(clone);

        assertEquals(bibtex, clone);
    }

    @Test
    void roundTripBiblatexToBibtexIsIdentity() {
        BibEntry clone = (BibEntry) biblatex.clone();
        new ConvertToBibtexCleanup().cleanup(clone);
        assertEquals(bibtex, clone);
        new ConvertToBiblatexCleanup().cleanup(clone);

        assertEquals(biblatex, clone);
    }
}
