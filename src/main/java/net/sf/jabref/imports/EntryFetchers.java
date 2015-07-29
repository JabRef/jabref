package net.sf.jabref.imports;

import net.sf.jabref.imports.fetcher.ISBNtoBibTeXFetcher;

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
