/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fetcher;

import net.sf.jabref.gui.importer.fetcher.ACMPortalFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.ADSFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.DBLPFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.DOAJFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.DOItoBibTeXFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.DiVAtoBibTeXFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.EntryFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.GVKFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.GoogleScholarFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.IEEEXploreFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.INSPIREFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.ISBNtoBibTeXFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.MedlineFetcherGUI;
import net.sf.jabref.gui.importer.fetcher.OAI2FetcherGUI;
import net.sf.jabref.gui.importer.fetcher.SpringerFetcherGUI;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class EntryFetchers {

    private final List<EntryFetcherGUI> entryFetchers = new LinkedList<>();


    public EntryFetchers(JournalAbbreviationLoader abbreviationLoader) {
        entryFetchers.add(new ADSFetcherGUI());
        entryFetchers.add(new CiteSeerXFetcher());
        entryFetchers.add(new DBLPFetcherGUI());
        entryFetchers.add(new DiVAtoBibTeXFetcherGUI());
        entryFetchers.add(new DOItoBibTeXFetcherGUI());
        entryFetchers.add(new GVKFetcherGUI());
        entryFetchers.add(new IEEEXploreFetcherGUI(abbreviationLoader));
        entryFetchers.add(new INSPIREFetcherGUI());
        entryFetchers.add(new ISBNtoBibTeXFetcherGUI());
        entryFetchers.add(new MedlineFetcherGUI());
        entryFetchers.add(new OAI2FetcherGUI());
        // entryFetchers.add(new ScienceDirectFetcher()); currently not working - removed see #409
        entryFetchers.add(new ACMPortalFetcherGUI());
        entryFetchers.add(new GoogleScholarFetcherGUI());
        entryFetchers.add(new DOAJFetcherGUI());
        entryFetchers.add(new SpringerFetcherGUI());
    }

    public List<EntryFetcherGUI> getEntryFetchers() {
        return Collections.unmodifiableList(this.entryFetchers);
    }
}
