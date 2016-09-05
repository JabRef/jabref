/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstrophysicsDataSystemTest {

    AstrophysicsDataSystem fetcher;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new AstrophysicsDataSystem(importFormatPreferences);
    }

    @Test
    public void performSearchFindsEntry() throws Exception {
        BibEntry diezSliceTheoremEntry = new BibEntry();
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
                        + "Fr$\\backslash$'echet manifolds is established. The Nash-Moser theorem provides the"
                        + NEWLINE + "fundamental tool to generalize the result of Palais to this" + NEWLINE
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

        List<BibEntry> fetchedEntries = fetcher.performSearch("Diez slice theorem");
        assertEquals(Collections.singletonList(diezSliceTheoremEntry), fetchedEntries);
    }
}
