package org.jabref.logic.cleanup;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibtexBiblatexRoundtripTest {
    private BibEntry bibtex;
    private BibEntry biblatex;

    @BeforeEach
    void setUp() {
        bibtex = new BibEntry(StandardEntryType.Article);
        bibtex.setField(StandardField.AUTHOR, "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.");
        bibtex.setField(StandardField.TITLE, "The hook graphs of the symmetric groups");
        bibtex.setField(StandardField.JOURNAL, "Canadian J. Math.");
        bibtex.setField(new UnknownField("fjournal"), "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques");
        bibtex.setField(StandardField.VOLUME, "6");
        bibtex.setField(StandardField.YEAR, "1954");
        bibtex.setField(StandardField.PAGES, "316--324");
        bibtex.setField(StandardField.ISSN, "0008-414X");
        bibtex.setField(new UnknownField("mrclass"), "20.0X");
        bibtex.setField(StandardField.MR_NUMBER, "0062127");
        bibtex.setField(new UnknownField("mrreviewer"), "D. E. Littlewood");

        biblatex = new BibEntry(StandardEntryType.Article);
        biblatex.setField(StandardField.AUTHOR, "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.");
        biblatex.setField(StandardField.TITLE, "The hook graphs of the symmetric groups");
        biblatex.setField(StandardField.JOURNALTITLE, "Canadian J. Math.");
        biblatex.setField(new UnknownField("fjournal"), "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques");
        biblatex.setField(StandardField.VOLUME, "6");
        biblatex.setField(StandardField.DATE, "1954");
        biblatex.setField(StandardField.PAGES, "316--324");
        biblatex.setField(StandardField.ISSN, "0008-414X");
        biblatex.setField(new UnknownField("mrclass"), "20.0X");
        biblatex.setField(StandardField.MR_NUMBER, "0062127");
        biblatex.setField(new UnknownField("mrreviewer"), "D. E. Littlewood");
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
