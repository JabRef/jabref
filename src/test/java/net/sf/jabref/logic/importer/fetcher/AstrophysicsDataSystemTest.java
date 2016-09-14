package net.sf.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static net.sf.jabref.logic.util.OS.NEWLINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstrophysicsDataSystemTest {

    AstrophysicsDataSystem fetcher;
    BibEntry diezSliceTheoremEntry;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new AstrophysicsDataSystem(importFormatPreferences);

        diezSliceTheoremEntry = new BibEntry();
        diezSliceTheoremEntry.setType(BibtexEntryTypes.ARTICLE);
        diezSliceTheoremEntry.setCiteKey("2014arXiv1405.2249D");
        diezSliceTheoremEntry.setField("author", "Diez, T.");
        diezSliceTheoremEntry.setField("title", "Slice theorem for Fr$\\backslash$'echet group actions and covariant symplectic field theory");
        diezSliceTheoremEntry.setField("year", "2014");
        diezSliceTheoremEntry.setField("archiveprefix", "arXiv");
        diezSliceTheoremEntry.setField("eprint", "1405.2249");
        diezSliceTheoremEntry.setField("journal", "ArXiv e-prints");
        diezSliceTheoremEntry.setField("keywords", "Mathematical Physics, Mathematics - Differential Geometry, Mathematics - Symplectic Geometry, 58B99, 58Z05, 58B25, 22E65, 58D19, 53D20, 53D42");
        diezSliceTheoremEntry.setField("month", "#may#");
        diezSliceTheoremEntry.setField("primaryclass", "math-ph");
        diezSliceTheoremEntry.setField("abstract",
                "A general slice theorem for the action of a Fr$\\backslash$'echet Lie group on a" + NEWLINE
                        + "Fr$\\backslash$'echet manifolds is established. The Nash-Moser theorem provides the" + NEWLINE
                        + "fundamental tool to generalize the result of Palais to this" + NEWLINE
                        + "infinite-dimensional setting. The presented slice theorem is illustrated" + NEWLINE
                        + "by its application to gauge theories: the action of the gauge" + NEWLINE
                        + "transformation group admits smooth slices at every point and thus the" + NEWLINE
                        + "gauge orbit space is stratified by Fr$\\backslash$'echet manifolds. Furthermore, a" + NEWLINE
                        + "covariant and symplectic formulation of classical field theory is" + NEWLINE
                        + "proposed and extensively discussed. At the root of this novel framework" + NEWLINE
                        + "is the incorporation of field degrees of freedom F and spacetime M into" + NEWLINE
                        + "the product manifold F * M. The induced bigrading of differential forms" + NEWLINE
                        + "is used in order to carry over the usual symplectic theory to this new" + NEWLINE
                        + "setting. The examples of the Klein-Gordon field and general Yang-Mills" + NEWLINE
                        + "theory illustrate that the presented approach conveniently handles the" + NEWLINE
                        + "occurring symmetries." + NEWLINE);
    }

    @Test
    public void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Diez slice theorem");
        assertEquals(Collections.singletonList(diezSliceTheoremEntry), fetchedEntries);
    }

    @Test
    public void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField("title", "slice theorem");
        searchEntry.setField("author", "Diez");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(diezSliceTheoremEntry, fetchedEntries.get(0));
    }
}
