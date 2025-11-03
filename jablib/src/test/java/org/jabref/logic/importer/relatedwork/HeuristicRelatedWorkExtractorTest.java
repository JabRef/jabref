package org.jabref.logic.importer.relatedwork;

import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeuristicRelatedWorkExtractorTest {

    @Test
    void extractsSentencesWithAuthorYearCitations() {
        String text = """
            1.3 Related work: environmental and social assessments of chocolate production
            Existing environmental LCAs include Italian chocolate production (Vesce et al., 2016),
            and Italian dark chocolate production from cradle-to-grave (Bianchi et al. 2021).
            Further studies assessed Colombian supply chains (Ramirez et al. 2016).
            """;

        BibEntry vesce = new BibEntry();
        vesce.setCitationKey("Vesce2016");
        vesce.setField(StandardField.AUTHOR, "Vesce, A.");
        vesce.setField(StandardField.YEAR, "2016");

        BibEntry bianchi = new BibEntry();
        bianchi.setCitationKey("Bianchi2021");
        bianchi.setField(StandardField.AUTHOR, "Bianchi, L.");
        bianchi.setField(StandardField.YEAR, "2021");

        BibEntry ramirez = new BibEntry();
        ramirez.setCitationKey("Ramirez2016");
        ramirez.setField(StandardField.AUTHOR, "Ramirez, J.");
        ramirez.setField(StandardField.YEAR, "2016");

        List<BibEntry> bibs = List.of(vesce, bianchi, ramirez);

        HeuristicRelatedWorkExtractor extractor = new HeuristicRelatedWorkExtractor();
        Map<String, String> out = extractor.extract(text, bibs);

        assertTrue(out.containsKey("Vesce2016"));
        assertTrue(out.containsKey("Bianchi2021"));
        assertTrue(out.containsKey("Ramirez2016"));
    }
}
