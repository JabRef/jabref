package org.jabref.logic.bst;

import java.nio.file.Path;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BstPreviewLayoutTest {

    private final BibDatabase bibDatabase = mock(BibDatabase.class);

    @Test
    public void generatePreviewForSimpleEntryUsingAbbr() throws Exception {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "Thoughts on Development");
        BibDatabase bibDatabase = mock(BibDatabase.class);
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabase);
        assertEquals("O. Kopp. Thoughts on development.", preview);
    }

    @Test
    public void monthMayIsCorrectlyRendered() throws Exception {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp")
                                       .withField(StandardField.TITLE, "Thoughts on Development")
                                       .withField(StandardField.MONTH, "#May#");
        BibDatabase bibDatabase = mock(BibDatabase.class);
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabase);
        assertEquals("O. Kopp. Thoughts on development, May.", preview);
    }

    @Test
    public void generatePreviewForSliceTheoremPaperUsingAbbr() throws Exception {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabase);
        assertEquals("T. Diez. Slice theorem for fréchet group actions and covariant symplectic field theory. May 2014.", preview);
    }

    @Test
    public void generatePreviewForSliceTheoremPaperUsingIEEE() throws Exception {
        BstPreviewLayout bstPreviewLayout = new BstPreviewLayout(Path.of(ClassLoader.getSystemResource("bst/IEEEtran.bst").toURI()));
        String preview = bstPreviewLayout.generatePreview(getSliceTheoremPaper(), bibDatabase);
        assertEquals("T. Diez, \"Slice theorem for fréchet group actions and covariant symplectic field theory\" May 2014.", preview);
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
