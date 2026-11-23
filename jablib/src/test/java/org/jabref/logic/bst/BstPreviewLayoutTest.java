package org.jabref.logic.bst;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstPreviewLayoutTest {

    private final BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();

    @Test
    void generatePreviewForSimpleEntryUsingAbbr() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "Thoughts on Development");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.&nbsp;Kopp. Thoughts on development.", preview);
    }

    @Test
    void generatePreviewForEntryWithMathFormulaInTitle() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Jane Doe")
                .withField(StandardField.TITLE, "Characterization of $\\alpha$");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("J.&nbsp;Doe. Characterization of &alpha;.", preview);
    }

    @Test
    void monthMayIsCorrectlyRendered() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "Thoughts on Development")
                                       .withField(StandardField.MONTH, "#May#");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.&nbsp;Kopp. Thoughts on development, May.", preview);
    }

    @Test
    void generatePreviewForSliceTheoremPaperUsingAbbr() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);
        assertEquals("T.&nbsp;Diez. Slice theorem for fréchet group actions and covariant symplectic field theory. May 2014.", preview);
    }

    @Test
    void generatePreviewForSliceTheoremPaperUsingIEEE() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("IEEEtran.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);
        assertEquals("T.&nbsp;Diez, \"Slice theorem for fréchet group actions and covariant symplectic field theory\" May 2014.", preview);
    }

    @Test
    void mathSymbolsInBracedMathAreConvertedToUnicode() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "{{$\\Sigma$}}{{$\\Delta$}} Modulator");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.&nbsp;Kopp. \u03a3\u0394 modulator.", preview);
    }

    @ParameterizedTest
    @MethodSource
    void generatePreviewHandlesInlineFormatting(String title, String expectedPreview) throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, title);
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals(expectedPreview, preview);
    }

    private static Stream<Arguments> generatePreviewHandlesInlineFormatting() {
        return Stream.of(

                // Small-caps forms that should be preserved in the preview output
                Arguments.of("\\textsc{L{\\'o}pez}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">L&oacute;pez.</span>"),
                Arguments.of("{\\sc L{\\'o}pez}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">L&oacute;pez.</span>"),
                Arguments.of("\\textsc{Outer \\textsc{inner} text}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">Outer <span style=\"font-variant: small-caps\">inner</span> text.</span>"),

                // Parser edge cases that should still keep the surrounding small-caps span intact
                Arguments.of("\\textsc {L{\\'o}pez}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">L&oacute;pez.</span>"),
                Arguments.of("\\textsc{A\\{B\\}C}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">ABC.</span>"),
                Arguments.of("{\\scshape Lopez}", "O.&nbsp;Kopp. <span style=\"font-variant: small-caps\">Lopez.</span>"),

                // Malformed input should fall back to the existing preview pipeline without failing
                Arguments.of("\\textsc{L{\\'o}pez", "O.&nbsp;Kopp. López."),
                Arguments.of("{\\sc L{\\'o}pez", "O.&nbsp;Kopp. lópez."),

                // Emphasis becomes HTML, not Unicode mathematical italic or bold letters
                // abbrv lowercases titles, as BibTeX does inside {\\em ...}
                Arguments.of("On {\\em Drosophila} genetics", "O.&nbsp;Kopp. On <em>drosophila</em> genetics."),
                Arguments.of("On \\emph{Drosophila} genetics", "O.&nbsp;Kopp. On <em>Drosophila</em> genetics."),
                Arguments.of("On \\textit{Drosophila} genetics", "O.&nbsp;Kopp. On <i>Drosophila</i> genetics."),
                Arguments.of("On \\textbf{Drosophila} genetics", "O.&nbsp;Kopp. On <b>Drosophila</b> genetics."),

                // Superscript and subscript become HTML
                Arguments.of("Proceedings of the 9\\textsuperscript{th} symposium", "O.&nbsp;Kopp. Proceedings of the 9<sup>th</sup> symposium."),
                Arguments.of("{H\\textsubscript{2}O}", "O.&nbsp;Kopp. H<sub>2</sub>O.")
        );
    }

    @Test
    void journalEmphasizedByBstIsRenderedItalic() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Karen A. Cooper")
                .withField(StandardField.TITLE, "Cocoa and health: a decade of research")
                .withField(StandardField.JOURNAL, "British Journal of Nutrition")
                .withField(StandardField.YEAR, "2007");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("K.&nbsp;A. Cooper. Cocoa and health: a decade of research. <em>British Journal of Nutrition</em>, 2007.", preview);
    }

    @Test
    void unresolvableBracedMathIsKept() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "{{$\\notacommand$}} Modulator");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.&nbsp;Kopp. notacommand modulator.", preview);
    }

    @Test
    void unparsableBstShowsPreviewError(@TempDir Path tempDir) throws IOException {
        Path invalidBst = tempDir.resolve("invalid.bst");
        Files.writeString(invalidBst, "}");

        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(invalidBst);
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);

        assertEquals("Error parsing file '" + invalidBst.toString() + "'", preview);
    }

    @Test
    void generatePreviewResolvesXDataFields() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.ADDRESS, "Reading, MA")
                .withField(StandardField.SERIES, "Computers and Typesetting");
        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.TITLE, "The {\\TeX} Book")
                .withField(StandardField.YEAR, "1984")
                .withField(StandardField.AUTHOR, "Knuth, Donald E.")
                .withField(StandardField.XDATA, "CAT")
                .withField(StandardField.NUMBER, "1");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(xdata, book)));

        String preview = bstPreviewLayout.generatePreview(book, context);

        assertEquals("D.&nbsp;E. Knuth. <em>The TeX Book.</em> Number&nbsp;1 in Computers and Typesetting. Addison-Wesley, Reading, MA, 1984.", preview);
    }

    @Test
    void generatePreviewIgnoresLibraryPreambleWhenResolvingXDataFields() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry xdata = new BibEntry(new UnknownEntryType("xdata"))
                .withCitationKey("CAT")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.ADDRESS, "Reading, MA")
                .withField(StandardField.SERIES, "Computers and Typesetting");
        BibEntry book = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Knuth")
                .withField(StandardField.TITLE, "The {\\TeX} Book")
                .withField(StandardField.YEAR, "1984")
                .withField(StandardField.AUTHOR, "Knuth, Donald E.")
                .withField(StandardField.XDATA, "CAT")
                .withField(StandardField.NUMBER, "1");
        BibDatabase database = new BibDatabase(List.of(xdata, book));
        database.setPreamble("Plain text preamble");
        BibDatabaseContext context = new BibDatabaseContext(database);

        String preview = bstPreviewLayout.generatePreview(book, context);

        assertEquals("D.&nbsp;E. Knuth. <em>The TeX Book.</em> Number&nbsp;1 in Computers and Typesetting. Addison-Wesley, Reading, MA, 1984.", preview);
    }

    private static BibEntry getSliceTheoremPaper() {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Tobias Diez")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.DATE, "2014-05-09")
                .withField(StandardField.ABSTRACT, "A general slice theorem for the action of a Fr\\'echet Lie group on a Fr\\'echet manifolds is established. The Nash-Moser theorem provides the fundamental tool to generalize the result of Palais to this infinite-dimensional setting. The presented slice theorem is illustrated by its application to gauge theories: the action of the gauge transformation group admits smooth slices at every point and thus the gauge orbit space is stratified by Fr\\'echet manifolds. Furthermore, a covariant and symplectic formulation of classical field theory is proposed and extensively discussed. At the root of this novel framework is the incorporation of field degrees of freedom F and spacetime M into the product manifold F * M. The induced bigrading of differential forms is used in order to carry over the usual symplectic theory to this new setting. The examples of the Klein-Gordon field and general Yang-Mills theory illustrate that the presented approach conveniently handles the occurring symmetries.")
                .withField(StandardField.EPRINT, "1405.2249v1")
                .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1405.2249v1:PDF")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.EPRINTCLASS, "math-ph")
                .withField(StandardField.KEYWORDS, "math-ph, math.DG, math.MP, math.SG, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
    }
}
