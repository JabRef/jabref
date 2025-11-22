package org.jabref.logic.importer.relatedwork;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RelatedWorkMetricsTest {

    private static BibEntry be(String author, String year) {
        BibEntry e = new BibEntry();
        e.setField(StandardField.AUTHOR, author);
        e.setField(StandardField.YEAR, year);
        // Add a citation key so the adapter can match it
        e.setCitationKey(author.split(",")[0].replaceAll("\\s+", "") + year);
        return e;
    }

    @Test
    public void evaluateFixtureInline() {
        // 1. Inline Related Work text (shortened example)
        String relatedWorkText = """
                Existing environmental LCAs include Italian chocolate production (Vesce et al., 2016),
                a comparison of milk and white chocolate (Bianchi et al., 2021),
                chocolate production and consumption in the UK (Konstantas et al., 2018),
                and dark chocolate cradle-to-grave (Recanati et al., 2018).
                """;

        // 2. Inline expected matches
        List<RelatedWorkFixture.Expectation> expectations = List.of(
                new RelatedWorkFixture.Expectation("Vesce", "2016", "Italian chocolate production"),
                new RelatedWorkFixture.Expectation("Bianchi", "2021", "milk and white chocolate"),
                new RelatedWorkFixture.Expectation("Konstantas", "2018", "production and consumption in the UK"),
                new RelatedWorkFixture.Expectation("Recanati", "2018", "dark chocolate cradle-to-grave")
        );

        // 3. Build fixture object directly
        RelatedWorkFixture fx = new RelatedWorkFixture("inline-fixture", relatedWorkText, expectations);

        // 4. Candidate BibEntries (with citation keys)
        List<BibEntry> candidates = List.of(
                be("Vesce, E.; Olivieri, G.; Pairotti, M. B.", "2016"),
                be("Bianchi, F. R.; Moreschi, L.; Gallo, M.", "2021"),
                be("Recanati, F.; Marveggio, D.; Dotelli, G.", "2018"),
                be("Konstantas, A.; Jeswani, H. K.; Stamford, L.; Azapagic, A.", "2018")
        );

        // 5. Run extractor via adapter + evaluation runner
        HeuristicRelatedWorkExtractor extractor = new HeuristicRelatedWorkExtractor();
        RelatedWorkEvaluationRunner runner = new RelatedWorkEvaluationRunner(new HeuristicExtractorAdapter(extractor));
        RelatedWorkMetrics metrics = runner.run(fx, candidates);

        System.out.println(metrics.pretty());

        // Loose sanity check
        assertTrue(metrics.recall >= 0.5, "recall should be >= 0.5 on the inline snippet set");
    }
}
