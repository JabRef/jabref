package org.jabref.logic.bst;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jabref.logic.preview.BstPreviewLayout;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

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
        assertEquals("O.\u00a0Kopp. Thoughts on development.", preview);
    }

    @Test
    void monthMayIsCorrectlyRendered() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "Thoughts on Development")
                                       .withField(StandardField.MONTH, "#May#");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.\u00a0Kopp. Thoughts on development, May.", preview);
    }

    @Test
    void generatePreviewForSliceTheoremPaperUsingAbbr() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);
        assertEquals("T.\u00a0Diez. Slice theorem for fréchet group actions and covariant symplectic field theory. May 2014.", preview);
    }

    @Test
    void generatePreviewForSliceTheoremPaperUsingIEEE() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("IEEEtran.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);
        assertEquals("T.\u00a0Diez, \"Slice theorem for fréchet group actions and covariant symplectic field theory\" May 2014.", preview);
    }

    @Test
    void mathSymbolsInBracedMathAreConvertedToUnicode() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "{{$\\Sigma$}}{{$\\Delta$}} Modulator");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.\u00a0Kopp. \u03a3\u0394 modulator.", preview);
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
                Arguments.of("\\textsc{L{\\'o}pez}", "O.\u00a0Kopp. <span style=\"font-variant: small-caps\">López.</span>"),
                Arguments.of("{\\sc L{\\'o}pez}", "O.\u00a0Kopp. <span style=\"font-variant: small-caps\">López.</span>"),
                Arguments.of("\\textsc{Outer \\textsc{inner} text}", "O.\u00a0Kopp. <span style=\"font-variant: small-caps\">Outer inner text.</span>"),

                // Parser edge cases that should still keep the surrounding small-caps span intact
                Arguments.of("\\textsc {L{\\'o}pez}", "O.\u00a0Kopp. <span style=\"font-variant: small-caps\">López.</span>"),
                Arguments.of("\\textsc{A\\{B\\}C}", "O.\u00a0Kopp. <span style=\"font-variant: small-caps\">ABC.</span>"),
                Arguments.of("{\\scshape Lopez}", "O.\u00a0Kopp. Lopez."),

                // Malformed input should fall back to the existing preview pipeline without failing
                Arguments.of("\\textsc{L{\\'o}pez", "O.\u00a0Kopp. López."),
                Arguments.of("{\\sc L{\\'o}pez", "O.\u00a0Kopp. lópez."),

                // Existing superscript and subscript rendering should remain unchanged
                Arguments.of("Proceedings of the 9\\textsuperscript{th} symposium", "O.\u00a0Kopp. Proceedings of the 9ᵗʰ symposium."),
                Arguments.of("{H\\textsubscript{2}O}", "O.\u00a0Kopp. H₂O.")
        );
    }

    @Test
    void unresolvableBracedMathIsKept() throws URISyntaxException {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "{{$\\notacommand$}} Modulator");
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabaseContext);
        assertEquals("O.\u00a0Kopp. modulator.", preview);
    }

    @Test
    void unparsableBstShowsPreviewError(@TempDir Path tempDir) throws IOException {
        Path invalidBst = tempDir.resolve("invalid.bst");
        Files.writeString(invalidBst, "}");

        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(invalidBst);
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabaseContext);

        assertEquals("Error parsing file '" + invalidBst.toString() + "'", preview);
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
