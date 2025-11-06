package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeuristicRelatedWorkExtractorTest {

    private BibEntry entry(String key, String author, String year) {
        BibEntry b = new BibEntry();
        b.setCitationKey(key);
        b.setField(StandardField.AUTHOR, author);
        b.setField(StandardField.YEAR, year);
        return b;
    }

    @Test
    void singleCitationExtracts() {
        String text = """
                1.4 Related work
                Prior literature reports similar findings (Vesce et al., 2016). Additional details follow.
                """;

        BibEntry vesce = entry("Vesce2016Key", "Vesce, A.", "2016");
        HeuristicRelatedWorkExtractor ex = new HeuristicRelatedWorkExtractor();

        Map<String, String> out = ex.extract(text, List.of(vesce));
        assertEquals(1, out.size());
        assertTrue(out.containsKey("Vesce2016Key"));
        assertTrue(out.get("Vesce2016Key").contains("similar findings"));
    }

    @Test
    void multiCitationBlockExtractsAll() {
        String text = """
                RELATED WORK
                Approaches vary by context (Bianchi, 2021; López & Perez 2020; Doe et al. 2019a), yet converge later.
                """;

        BibEntry bianchi = entry("Bianchi2021", "Bianchi, M.", "2021");
        BibEntry lopez = entry("Lopez2020", "López and Perez", "2020");
        BibEntry doe = entry("Doe2019", "Doe and Others", "2019");

        HeuristicRelatedWorkExtractor ex = new HeuristicRelatedWorkExtractor();
        Map<String, String> out = ex.extract(text, List.of(bianchi, lopez, doe));

        assertEquals(3, out.size());
        assertTrue(out.get("Bianchi2021").endsWith("."));
        assertTrue(out.get("Lopez2020").contains("Approaches vary"));
        assertTrue(out.get("Doe2019").contains("Approaches vary"));
    }

    @Test
    void diacriticsAreNormalized() {
        String text = """
                Related work
                See also prior synthesis (Šimić, 2022).
                """;

        BibEntry simic = entry("Simic2022", "Šimić, Ana", "2022");
        HeuristicRelatedWorkExtractor ex = new HeuristicRelatedWorkExtractor();
        Map<String, String> out = ex.extract(text, List.of(simic));
        assertTrue(out.containsKey("Simic2022"));
    }
}
