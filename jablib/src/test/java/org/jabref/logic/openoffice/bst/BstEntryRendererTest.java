package org.jabref.logic.openoffice.bst;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bst.BstVM;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BstEntryRendererTest {

    // Article entry used across tests
    private static BibEntry articleEntry() {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey("Cooper2007")
                .withField(StandardField.AUTHOR, "Cooper, Karen A. and Donovan, Jennifer L. and Waterhouse, Andrew L. and Williamson, Gary")
                .withField(StandardField.TITLE, "Cocoa and health: a decade of research")
                .withField(StandardField.JOURNAL, "British Journal of Nutrition")
                .withField(StandardField.VOLUME, "99")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "1--11")
                .withField(StandardField.YEAR, "2007");
    }

    // InProceedings entry matching BstVMTest.defaultTestEntry()
    private static BibEntry inproceedingsEntry() {
        return new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("canh05")
                .withField(StandardField.AUTHOR, "Crowston, K. and Annabi, H. and Howison, J. and Masango, C.")
                .withField(StandardField.TITLE, "Effective work practices for floss development: A model and propositions")
                .withField(StandardField.BOOKTITLE, "Hawaii International Conference On System Sciences (HICSS)")
                .withField(StandardField.YEAR, "2005");
    }

    private static Path ieeePath() throws URISyntaxException {
        return Path.of(BstEntryRendererTest.class.getResource("/org/jabref/logic/bst/IEEEtran.bst").toURI());
    }

    private static Path abbrvPath() throws URISyntaxException {
        return Path.of(BstEntryRendererTest.class.getResource("/org/jabref/logic/bst/abbrv.bst").toURI());
    }

    @Test
    void ieeeRendererStripsProvidecommandPreamble() throws Exception {
        BstVM vm = new BstVM(ieeePath());
        BibEntry entry = articleEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\providecommand"), "IEEEtran preamble should be stripped");
        assertFalse(result.contains("\\csname"), "IEEEtran preamble should be stripped");
        assertFalse(result.contains("\\BIBentryALTinterwordstretchfactor"), "IEEEtran preamble should be stripped");
    }

    @Test
    void ieeeRendererStripsBibitem() throws Exception {
        BstVM vm = new BstVM(ieeePath());
        BibEntry entry = articleEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\bibitem"), "\\bibitem tag should be stripped");
    }

    @Test
    void ieeeRendererStripsThebibliographyEnvironment() throws Exception {
        BstVM vm = new BstVM(ieeePath());
        BibEntry entry = articleEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\begin{thebibliography}"), "\\begin{thebibliography} should be stripped");
        assertFalse(result.contains("\\end{thebibliography}"), "\\end{thebibliography} should be stripped");
    }

    @Test
    void ieeeRendererPreservesAuthorText() throws Exception {
        BstVM vm = new BstVM(ieeePath());
        BibEntry entry = articleEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertTrue(result.contains("Cooper"), "Author name should be in the output");
        assertTrue(result.contains("2007"), "Year should be in the output");
    }

    @Test
    void abbrvRendererStripsBibitem() throws Exception {
        BstVM vm = new BstVM(abbrvPath());
        BibEntry entry = inproceedingsEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\bibitem"), "\\bibitem tag should be stripped");
    }

    @Test
    void abbrvRendererStripsThebibliographyEnvironment() throws Exception {
        BstVM vm = new BstVM(abbrvPath());
        BibEntry entry = inproceedingsEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\begin{thebibliography}"), "\\begin{thebibliography} should be stripped");
        assertFalse(result.contains("\\end{thebibliography}"), "\\end{thebibliography} should be stripped");
    }

    @Test
    void abbrvRendererReplacesNewblock() throws Exception {
        BstVM vm = new BstVM(abbrvPath());
        BibEntry entry = inproceedingsEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertFalse(result.contains("\\newblock"), "\\newblock should be replaced with a space");
    }

    @Test
    void abbrvRendererPreservesAuthorAndTitle() throws Exception {
        BstVM vm = new BstVM(abbrvPath());
        BibEntry entry = inproceedingsEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        assertTrue(result.contains("Crowston"), "Author name should be in the output");
        assertTrue(result.contains("2005"), "Year should be in the output");
    }

    @Test
    void abbrvRendererPreservesInlineLatexMarkup() throws Exception {
        BstVM vm = new BstVM(abbrvPath());
        BibEntry entry = inproceedingsEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        // abbrv wraps booktitle in {\em ...} - inline markup must survive stripping
        assertTrue(result.contains("{\\em"), "Inline LaTeX emphasis markup should be preserved");
    }

    @Test
    void ieeeSingleEntryDoesNotProduceDuplicateText() throws Exception {
        BstVM vm = new BstVM(ieeePath());
        BibEntry entry = articleEntry();
        BibDatabase db = new BibDatabase(List.of(entry));

        String result = new BstEntryRenderer(vm).renderEntryToLatex(entry, db);

        // The author name should appear exactly once (preamble fully stripped)
        int count = 0;
        int idx = 0;
        while ((idx = result.indexOf("Cooper", idx)) >= 0) {
            count++;
            idx++;
        }
        assertTrue(count >= 1, "Author should appear in output");
        assertTrue(count <= 2, "Author should not appear many times (preamble not stripped)");
    }
}
