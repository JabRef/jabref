package org.jabref.gui.importer.fetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.jabref.Globals;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.DiVA;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.GoogleScholar;
import org.jabref.logic.importer.fetcher.GvkFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.logic.importer.fetcher.MathSciNet;
import org.jabref.logic.importer.fetcher.MedlineFetcher;
import org.jabref.logic.importer.fetcher.TitleFetcher;
import org.jabref.logic.importer.fetcher.zbMATH;
import org.jabref.logic.journals.JournalAbbreviationLoader;

public class EntryFetchers {

    private final List<EntryFetcher> entryFetchers = new LinkedList<>();

    public EntryFetchers(JournalAbbreviationLoader abbreviationLoader) {
        entryFetchers.add(new CiteSeerXFetcher());
        entryFetchers.add(new SearchBasedEntryFetcher(new DBLPFetcher(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new IEEEXploreFetcher(abbreviationLoader));
        entryFetchers.add(new INSPIREFetcher());
        // entryFetchers.add(new OAI2Fetcher()); - new arXiv fetcher in place, see below
        entryFetchers.add(new ACMPortalFetcher());
        entryFetchers.add(new DOAJFetcher());
        entryFetchers.add(new SpringerFetcher());

        entryFetchers.add(new SearchBasedEntryFetcher(new ArXiv(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new GvkFetcher()));
        entryFetchers.add(new SearchBasedEntryFetcher(new MedlineFetcher()));
        entryFetchers.add(
                new SearchBasedEntryFetcher(new AstrophysicsDataSystem(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new MathSciNet(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new zbMATH(Globals.prefs.getImportFormatPreferences())));
        entryFetchers.add(new SearchBasedEntryFetcher(new GoogleScholar(Globals.prefs.getImportFormatPreferences())));
    }

    public static List<IdBasedFetcher> getIdFetchers(ImportFormatPreferences importFormatPreferences) {
        ArrayList<IdBasedFetcher> list = new ArrayList<>();
        list.add(new ArXiv(importFormatPreferences));
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new IsbnFetcher(importFormatPreferences));
        list.add(new DiVA(importFormatPreferences));
        list.add(new DoiFetcher(importFormatPreferences));
        list.add(new MedlineFetcher());
        list.add(new TitleFetcher(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.sort(Comparator.comparing(WebFetcher::getName));
        return list;
    }

    public static List<EntryBasedFetcher> getEntryBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        ArrayList<EntryBasedFetcher> list = new ArrayList<>();
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new DoiFetcher(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.sort(Comparator.comparing(WebFetcher::getName));
        return list;
    }

    public List<EntryFetcher> getEntryFetchers() {
        return Collections.unmodifiableList(this.entryFetchers);
    }
}
