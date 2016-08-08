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

package net.sf.jabref.logic.groups;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;

public class AbstractGroupTest {

    private AbstractGroup group;
    private List<BibEntry> entries = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        group = mock(AbstractGroup.class, Mockito.CALLS_REAL_METHODS);

        entries.add(new BibEntry().withField("author", "author1 and author2"));
        entries.add(new BibEntry().withField("author", "author1"));
    }
}
