package org.jabref.logic.bst;

import java.nio.file.Paths;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BstPreviewLayoutTest {

    private BstPreviewLayout bstPreviewLayout;

    @BeforeEach
    public void initialize() throws Exception {
        bstPreviewLayout = new BstPreviewLayout(Paths.get(BstPreviewLayoutTest.class.getResource("abbrv.bst").toURI()));
    }

    @Test
    public void generatePreviewForSimpleEntry() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Oliver Kopp").withField(StandardField.TITLE, "Thoughts on Development");
        BibDatabase bibDatabase = mock(BibDatabase.class);
        String preview = bstPreviewLayout.generatePreview(entry, bibDatabase);
        assertEquals("O. Kopp. Thoughts on development.", preview);
    }

    @Test
    public void generatePreviewForSliceTheoremPaper() {
        BibEntry sliceTheoremPaper = new BibEntry();
        sliceTheoremPaper.setType(StandardEntryType.Article);
        sliceTheoremPaper.setField(StandardField.AUTHOR, "Tobias Diez");
        sliceTheoremPaper.setField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory");
        sliceTheoremPaper.setField(StandardField.DATE, "2014-05-09");
        sliceTheoremPaper.setField(StandardField.ABSTRACT, "A general slice theorem for the action of a Fr\\'echet Lie group on a Fr\\'echet manifolds is established. The Nash-Moser theorem provides the fundamental tool to generalize the result of Palais to this infinite-dimensional setting. The presented slice theorem is illustrated by its application to gauge theories: the action of the gauge transformation group admits smooth slices at every point and thus the gauge orbit space is stratified by Fr\\'echet manifolds. Furthermore, a covariant and symplectic formulation of classical field theory is proposed and extensively discussed. At the root of this novel framework is the incorporation of field degrees of freedom F and spacetime M into the product manifold F * M. The induced bigrading of differential forms is used in order to carry over the usual symplectic theory to this new setting. The examples of the Klein-Gordon field and general Yang-Mills theory illustrate that the presented approach conveniently handles the occurring symmetries.");
        sliceTheoremPaper.setField(StandardField.EPRINT, "1405.2249v1");
        sliceTheoremPaper.setField(StandardField.FILE, ":http\\://arxiv.org/pdf/1405.2249v1:PDF");
        sliceTheoremPaper.setField(StandardField.EPRINTTYPE, "arXiv");
        sliceTheoremPaper.setField(StandardField.EPRINTCLASS, "math-ph");
        sliceTheoremPaper.setField(StandardField.KEYWORDS, "math-ph, math.DG, math.MP, math.SG, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
        BibDatabase bibDatabase = mock(BibDatabase.class);
        String preview = bstPreviewLayout.generatePreview(sliceTheoremPaper, bibDatabase);
        assertEquals("T. Diez. Slice theorem for fréchet group actions and covariant symplectic field theory. #may# 2014.", preview);
    }
}
