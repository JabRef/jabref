package net.sf.jabref.gui.importer.fetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.fetcher.AdsFetcher;
import net.sf.jabref.logic.importer.fetcher.ArXiv;
import net.sf.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import net.sf.jabref.logic.importer.fetcher.DiVA;
import net.sf.jabref.logic.importer.fetcher.DoiFetcher;
import net.sf.jabref.logic.importer.fetcher.GvkFetcher;
import net.sf.jabref.logic.importer.fetcher.IsbnFetcher;
import net.sf.jabref.logic.importer.fetcher.MathSciNet;
import net.sf.jabref.logic.importer.fetcher.zbMATH;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

public class EntryFetchers {

    private final List<EntryFetcher> entryFetchers = new LinkedList<>();

    public EntryFetchers(JournalAbbreviationLoader abbreviationLoader) {
        entryFetchers.add(new CiteSeerXFetcher());
        entryFetchers.add(new DBLPFetcher());
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
        entryFetchers.add(
                new SearchBasedEntryFetcher(new AstrophysicsDataSystem(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new MathSciNet(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new zbMATH(Globals.prefs.getImportFormatPreferences())));
    }

    public List<EntryFetcher> getEntryFetchers() {
        return Collections.unmodifiableList(this.entryFetchers);
    }

    public static ArrayList<IdBasedFetcher> getIdFetchers() {
        ArrayList<IdBasedFetcher> list = new ArrayList<>();
        list.add(new AdsFetcher(Globals.prefs.getImportFormatPreferences()));
        list.add(new IsbnFetcher(Globals.prefs.getImportFormatPreferences()));
        list.add(new DiVA(Globals.prefs.getImportFormatPreferences()));
        list.add(new DoiFetcher(Globals.prefs.getImportFormatPreferences()));
        list.sort((fetcher1, fetcher2) -> fetcher1.getName().compareTo(fetcher2.getName()));
        return list;
    }
}
