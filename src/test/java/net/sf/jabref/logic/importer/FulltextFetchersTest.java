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

package net.sf.jabref.logic.importer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FulltextFetchersTest {
    private BibEntry entry;

    @Before
    public void setUp() {
        entry = new BibEntry();
    }

    @After
    public void tearDown() {
        entry = null;
    }

    @Ignore
    @Test
    public void acceptPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("http://docs.oasis-open.org/wsbpel/2.0/OS/wsbpel-v2.0-OS.pdf");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finder));

        assertEquals(Optional.of(pdfUrl), fetcher.findFullTextPDF(entry));
    }

    @Ignore // Fails on travis
    @Test
    public void rejectNonPdfUrls() throws MalformedURLException {
        URL pdfUrl = new URL("https://github.com/JabRef/jabref/blob/master/README.md");
        FulltextFetcher finder = (e) -> Optional.of(pdfUrl);
        FulltextFetchers fetcher = new FulltextFetchers(Arrays.asList(finder));

        assertEquals(Optional.empty(), fetcher.findFullTextPDF(entry));
    }
}
