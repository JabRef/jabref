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
        bibtex = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.")
                .withField(StandardField.TITLE, "The hook graphs of the symmetric groups")
                .withField(StandardField.JOURNAL, "Canadian J. Math.")
                .withField(new UnknownField("fjournal"), "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques")
                .withField(StandardField.VOLUME, "6")
                .withField(StandardField.YEAR, "1954")
                .withField(StandardField.PAGES, "316--324")
                .withField(StandardField.ISSN, "0008-414X")
                .withField(new UnknownField("mrclass"), "20.0X")
                .withField(StandardField.MR_NUMBER, "0062127")
                .withField(new UnknownField("mrreviewer"), "D. E. Littlewood");

        biblatex = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Frame, J. S. and Robinson, G. de B. and Thrall, R. M.")
                .withField(StandardField.TITLE, "The hook graphs of the symmetric groups")
                .withField(StandardField.JOURNALTITLE, "Canadian J. Math.")
                .withField(new UnknownField("fjournal"), "Canadian Journal of Mathematics. Journal Canadien de Math\\'ematiques")
                .withField(StandardField.VOLUME, "6")
                .withField(StandardField.DATE, "1954")
                .withField(StandardField.PAGES, "316--324")
                .withField(StandardField.ISSN, "0008-414X")
                .withField(new UnknownField("mrclass"), "20.0X")
                .withField(StandardField.MR_NUMBER, "0062127")
                .withField(new UnknownField("mrreviewer"), "D. E. Littlewood");
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
