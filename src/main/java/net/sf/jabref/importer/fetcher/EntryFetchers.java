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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class EntryFetchers {

    public static final EntryFetchers INSTANCE = new EntryFetchers();

    private final List<EntryFetcher> entryFetchers = new LinkedList<>();

    public EntryFetchers() {
        entryFetchers.add(new ADSFetcher());
        entryFetchers.add(new CiteSeerXFetcher());
        entryFetchers.add(new DBLPFetcher());
        entryFetchers.add(new DiVAtoBibTeXFetcher());
        entryFetchers.add(new DOItoBibTeXFetcher());
        entryFetchers.add(new IEEEXploreFetcher());
        entryFetchers.add(new INSPIREFetcher());
        entryFetchers.add(new ISBNtoBibTeXFetcher());
        entryFetchers.add(new JSTORFetcher());
        entryFetchers.add(new JSTORFetcher2());
        entryFetchers.add(new MedlineFetcher());
        entryFetchers.add(new OAI2Fetcher());
        entryFetchers.add(new ScienceDirectFetcher());
        entryFetchers.add(new SPIRESFetcher());
        entryFetchers.add(new ACMPortalFetcher());
        entryFetchers.add(new GoogleScholarFetcher());
    }

    public List<EntryFetcher> getEntryFetchers() {
        return Collections.unmodifiableList(this.entryFetchers);
    }
}
