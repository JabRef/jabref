package net.sf.jabref.gui.importer.fetcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.DiVA;
import net.sf.jabref.logic.importer.fetcher.GvkFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnFetcher;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

public class EntryFetchers {

    private final List<EntryFetcher> entryFetchers = new LinkedList<>();

    public EntryFetchers(JournalAbbreviationLoader abbreviationLoader) {
        entryFetchers.add(new ADSFetcher());
        entryFetchers.add(new CiteSeerXFetcher());
        entryFetchers.add(new DBLPFetcher());
        entryFetchers.add(new DOItoBibTeXFetcher());
        entryFetchers.add(new IEEEXploreFetcher(abbreviationLoader));
        entryFetchers.add(new INSPIREFetcher());
        entryFetchers.add(new MedlineFetcher());
        // entryFetchers.add(new OAI2Fetcher()); - new arXiv fetcher in place, see below
        // entryFetchers.add(new ScienceDirectFetcher()); currently not working - removed see #409
        entryFetchers.add(new ACMPortalFetcher());
        entryFetchers.add(new GoogleScholarFetcher());
        entryFetchers.add(new DOAJFetcher());
        entryFetchers.add(new SpringerFetcher());

        entryFetchers.add(new SearchBasedEntryFetcher(new ArXiv()));
        entryFetchers.add(new SearchBasedEntryFetcher(new GvkFetcher()));
    }

    public List<EntryFetcher> getEntryFetchers() {
        return Collections.unmodifiableList(this.entryFetchers);
    }

    public static List<IdBasedFetcher> getIdFetchers() {
        List<IdBasedFetcher> list = new LinkedList<>();
        list.add(new IsbnFetcher(Globals.prefs.getImportFormatPreferences()));
        list.add(new DiVA(Globals.prefs.getImportFormatPreferences()));
        list.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        return list;
    }
}
