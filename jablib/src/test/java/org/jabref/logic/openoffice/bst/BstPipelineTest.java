package org.jabref.logic.openoffice.bst;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bst.BstVM;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// End-to-end pipeline tests: BST render → pandoc HTML → OOText.
/// Tests that require pandoc are skipped automatically when pandoc is not on the PATH.
class BstPipelineTest {

    private PandocLatexConverter pandoc;
    private BibEntry articleEntry;

    @BeforeEach
    void setUp() {
        pandoc = new PandocLatexConverter("pandoc");
        articleEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Cooper2007")
                .withField(StandardField.AUTHOR, "Cooper, Karen A. and Donovan, Jennifer L. and Waterhouse, Andrew L.")
                .withField(StandardField.TITLE, "Cocoa and health: a decade of research")
                .withField(StandardField.JOURNAL, "British Journal of Nutrition")
                .withField(StandardField.VOLUME, "99")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "1--11")
                .withField(StandardField.YEAR, "2007");
    }

    private static Path ieeePath() throws URISyntaxException {
        return Path.of(BstPipelineTest.class.getResource("/org/jabref/logic/bst/IEEEtran.bst").toURI());
    }

    private static Path abbrvPath() throws URISyntaxException {
        return Path.of(BstPipelineTest.class.getResource("/org/jabref/logic/bst/abbrv.bst").toURI());
    }

    // --- pandoc availability ---

    @Test
    void pandocAvailabilityCheck() {
        // This test always passes - it just documents whether pandoc is present.
        // If false, all pandoc-dependent tests below will be skipped.
        boolean available = pandoc.isAvailable();
        assertTrue(available || !available, "pandoc.isAvailable() must return a boolean");
    }

    // --- IEEEtran full pipeline ---

    @Test
    void ieeeFullPipelineProducesCleanText() throws Exception {
        assumeTrue(pandoc.isAvailable(), "pandoc not available - skipping pipeline test");

        BstVM vm = new BstVM(ieeePath());
        BibDatabase db = new BibDatabase(List.of(articleEntry));
        BstEntryRenderer renderer = new BstEntryRenderer(vm);

        String latex = renderer.renderEntryToLatex(articleEntry, db);

        assertFalse(latex.isBlank(), "BstVM should produce output for IEEEtran");
        assertFalse(latex.contains("\\providecommand"), "Preamble should be stripped before pandoc");

        String html = pandoc.latexToHtml(latex);

        assertFalse(html.isBlank(), "pandoc should produce HTML output");
        assertTrue(html.contains("<p>"), "pandoc wraps output in <p>");

        String ooText = BstHtmlToOOText.convert(html);

        assertFalse(ooText.startsWith("<p>"), "OOText should not start with <p>");
        assertTrue(ooText.contains("Cooper"), "Author name should survive the full pipeline");
        assertTrue(ooText.contains("2007"), "Year should survive the full pipeline");
    }

    @Test
    void ieeeFullPipelineProducesItalicsForJournalTitle() throws Exception {
        assumeTrue(pandoc.isAvailable(), "pandoc not available - skipping pipeline test");

        BstVM vm = new BstVM(ieeePath());
        BibDatabase db = new BibDatabase(List.of(articleEntry));

        String latex = new BstEntryRenderer(vm).renderEntryToLatex(articleEntry, db);
        String html = pandoc.latexToHtml(latex);
        String ooText = BstHtmlToOOText.convert(html);

        // IEEEtran renders journal name in \emph{} → pandoc → <em> → BstHtmlToOOText → <i>
        assertTrue(ooText.contains("<i>") && ooText.contains("</i>"),
                "Journal title should be in italics in OOText output");
    }

    @Test
    void ieeeOutputDoesNotContainPandocParagraphWrapper() throws Exception {
        assumeTrue(pandoc.isAvailable(), "pandoc not available - skipping pipeline test");

        BstVM vm = new BstVM(ieeePath());
        BibDatabase db = new BibDatabase(List.of(articleEntry));

        String latex = new BstEntryRenderer(vm).renderEntryToLatex(articleEntry, db);
        String html = pandoc.latexToHtml(latex);
        String ooText = BstHtmlToOOText.convert(html);

        // The OOText must be a plain inline run - no leading paragraph break that would
        // orphan the "[n] " prefix written by BstCitationOOAdapter.insertBibliography.
        assertFalse(ooText.startsWith("<p>"), "No leading <p> - would orphan the citation number");
    }

    // --- abbrv full pipeline ---

    @Test
    void abbrvFullPipelineProducesCleanText() throws Exception {
        assumeTrue(pandoc.isAvailable(), "pandoc not available - skipping pipeline test");

        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("canh05")
                .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H. and Howison, J. and Masango, C.")
                .withField(StandardField.TITLE, "Effective work practices for floss development")
                .withField(StandardField.BOOKTITLE, "Hawaii International Conference On System Sciences (HICSS)")
                .withField(StandardField.YEAR, "2005");

        BstVM vm = new BstVM(abbrvPath());
        BibDatabase db = new BibDatabase(List.of(entry));

        String latex = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(latex.isBlank(), "BstVM should produce output for abbrv");
        assertFalse(latex.contains("\\bibitem"), "\\bibitem should be stripped");

        String html = pandoc.latexToHtml(latex);
        String ooText = BstHtmlToOOText.convert(html);

        assertFalse(ooText.startsWith("<p>"), "No leading <p> in OOText");
        assertTrue(ooText.contains("Crowston"), "Author should be in final output");
        assertTrue(ooText.contains("2005"), "Year should be in final output");
    }

    // --- pandoc error surfacing ---

    @Test
    void pandocLatexToHtmlDoesNotReturnBlankOnValidInput() throws Exception {
        assumeTrue(pandoc.isAvailable(), "pandoc not available - skipping pipeline test");

        // Minimal valid LaTeX inline text - pandoc must produce non-empty HTML
        String html = pandoc.latexToHtml("Smith et al., 2016.");

        assertFalse(html.isBlank(), "pandoc should produce HTML for simple text input");
        assertTrue(html.contains("Smith"), "Content should appear in pandoc output");
    }
}
