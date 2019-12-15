package org.jabref.logic.importer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.regex.Pattern;
import java.util.HashMap;
import  java.util.function.Predicate;

import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.ACS;
import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.CiteSeer;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.DOAJFetcher;
import org.jabref.logic.importer.fetcher.DiVA;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.DoiResolution;
import org.jabref.logic.importer.fetcher.GoogleScholar;
import org.jabref.logic.importer.fetcher.GvkFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.INSPIREFetcher;
import org.jabref.logic.importer.fetcher.IacrEprintFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.logic.importer.fetcher.LibraryOfCongress;
import org.jabref.logic.importer.fetcher.MathSciNet;
import org.jabref.logic.importer.fetcher.MedlineFetcher;
import org.jabref.logic.importer.fetcher.OpenAccessDoi;
import org.jabref.logic.importer.fetcher.RfcFetcher;
import org.jabref.logic.importer.fetcher.ScienceDirect;
import org.jabref.logic.importer.fetcher.SpringerFetcher;
import org.jabref.logic.importer.fetcher.SpringerLink;
import org.jabref.logic.importer.fetcher.TitleFetcher;
import org.jabref.logic.importer.fetcher.ZbMATH;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Identifier;

import static org.jabref.model.entry.field.StandardField.EPRINT;
import static org.jabref.model.entry.field.StandardField.ISBN;

public class WebFetchers {

    private WebFetchers() {
    }

    public static Optional<IdBasedFetcher> getIdBasedFetcherForField(Field field, ImportFormatPreferences preferences) {
        IdBasedFetcher fetcher;

        if (field == StandardField.DOI) {
            fetcher = new DoiFetcher(preferences);
        } else if (field == ISBN) {
            fetcher = new IsbnFetcher(preferences);
        } else if (field == EPRINT) {
            fetcher = new ArXiv(preferences);
        } else {
            return Optional.empty();
        }
        return Optional.of(fetcher);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Identifier> IdFetcher<T> getIdFetcherForIdentifier(Class<T> clazz) {
        if (clazz == DOI.class) {
            return (IdFetcher<T>) new CrossRef();
        } else {
            throw new IllegalArgumentException("No fetcher found for identifier" + clazz.getCanonicalName());
        }
    }

    public static Optional<IdFetcher<? extends Identifier>> getIdFetcherForField(Field field) {
        if (field == StandardField.DOI) {
            return Optional.of(new CrossRef());
        }
        return Optional.empty();
    }

    /**
     * @return sorted set containing search based fetchers
     */
    public static SortedSet<SearchBasedFetcher> getSearchBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        SortedSet<SearchBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new ArXiv(importFormatPreferences));
        set.add(new INSPIREFetcher(importFormatPreferences));
        set.add(new GvkFetcher());
        set.add(new MedlineFetcher());
        set.add(new AstrophysicsDataSystem(importFormatPreferences));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new ZbMATH(importFormatPreferences));
        set.add(new ACMPortalFetcher(importFormatPreferences));
        set.add(new GoogleScholar(importFormatPreferences));
        set.add(new DBLPFetcher(importFormatPreferences));
        set.add(new SpringerFetcher());
        set.add(new CrossRef());
        set.add(new CiteSeer());
        set.add(new DOAJFetcher(importFormatPreferences));
        set.add(new IEEE(importFormatPreferences));
        return set;
    }

    /**
     * @return sorted set containing id based fetchers
     */
    public static SortedSet<IdBasedFetcher> getIdBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        SortedSet<IdBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new ArXiv(importFormatPreferences));
        set.add(new AstrophysicsDataSystem(importFormatPreferences));
        set.add(new IsbnFetcher(importFormatPreferences));
        set.add(new DiVA(importFormatPreferences));
        set.add(new DoiFetcher(importFormatPreferences));
        set.add(new MedlineFetcher());
        set.add(new TitleFetcher(importFormatPreferences));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new CrossRef());
        set.add(new LibraryOfCongress(importFormatPreferences));
        set.add(new IacrEprintFetcher(importFormatPreferences));
        set.add(new RfcFetcher(importFormatPreferences));
        return set;
    }

    /**
     * @return an Hashmap containing predicate from id type and their id based fetchers
     */
    public static HashMap<Predicate<String>,IdBasedFetcher> getHashMapPredicateIdBasedFetchers(ImportFormatPreferences importFormatPreferences) {

        HashMap <Predicate<String>,IdBasedFetcher> fetcherMatchPattern = new HashMap<Predicate<String>,IdBasedFetcher>();

        //DOI validate
        Pattern pdoi = Pattern.compile("(https://doi\\.org/)?10\\.[0-9]{4}/([0-9]{13}|[a-zA-Z]+\\.[0-9]{4}\\.[0-9]{2}|[-_0-9]+)");
        fetcherMatchPattern.put(pdoi.asPredicate(), new DoiFetcher(importFormatPreferences));

        //ISBN validate
        Pattern pisbn = Pattern.compile("^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$");
        fetcherMatchPattern.put(pisbn.asPredicate(), new IsbnFetcher(importFormatPreferences));
        //Rajouter pour long isbn

        //DIVA Validate
        Pattern pdiva = Pattern.compile("diva2:[0-9]{6}");
        fetcherMatchPattern.put(pdiva.asPredicate(), new DiVA(importFormatPreferences));

        //MedlineFetcher Validate
        Pattern pmd = Pattern.compile("[0-9]{8}");
        fetcherMatchPattern.put(pmd.asPredicate(), new MedlineFetcher());

        //MathSciNetId validate
        Pattern pMathSciNetId = Pattern.compile("[0-9]{7}");
        fetcherMatchPattern.put(pMathSciNetId.asPredicate(), new MathSciNet(importFormatPreferences));

        //LibraryOfCongress validate
        Pattern pLibraryOfCongress = Pattern.compile("[0-9]{10}");
        fetcherMatchPattern.put(pLibraryOfCongress.asPredicate(), new LibraryOfCongress(importFormatPreferences));

        //RfcFetcher validate
        Pattern pRfcFetcher = Pattern.compile("([rR][fF][cC])?[0-9]{4}");
        fetcherMatchPattern.put(pRfcFetcher.asPredicate(), new RfcFetcher(importFormatPreferences));

        //IacrEprintFetcher validate
        Pattern pIacr = Pattern.compile("[a-zA-Z]*[0-9]{4}/[0-9]{4}");
        fetcherMatchPattern.put(pIacr.asPredicate(), new IacrEprintFetcher(importFormatPreferences));

        //Arxiv validate
        Pattern parxiv = Pattern.compile("(http[s]?://arxiv\\.org/(abs/)?)?(arXiv:)?[0-9][0-9](0[1-9]|1[0-2])\\.[0-9]{4,5}([v][0-9]+)?");
        fetcherMatchPattern.put(parxiv.asPredicate(), new ArXiv(importFormatPreferences));

        //AstrophysicsDataSystem
        Pattern pads = Pattern.compile("10\\.[0-9]{4,5}/[-)(.a-zA-Z_0-9]+");
        fetcherMatchPattern.put(pads.asPredicate(), new AstrophysicsDataSystem(importFormatPreferences));

        //TitleFetcher
        Pattern pTitleFetcher = Pattern.compile("[-)(.a-zA-Z_0-9]+");
        fetcherMatchPattern.put(pTitleFetcher.asPredicate(), new TitleFetcher(importFormatPreferences));

        //CrossRef -> DOI

        return fetcherMatchPattern;
    }

        /**
         * @return sorted set containing entry based fetchers
         */
    public static SortedSet<EntryBasedFetcher> getEntryBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        SortedSet<EntryBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new AstrophysicsDataSystem(importFormatPreferences));
        set.add(new DoiFetcher(importFormatPreferences));
        set.add(new IsbnFetcher(importFormatPreferences));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new CrossRef());
        return set;
    }

    /**
     * @return sorted set containing id fetchers
     */
    public static SortedSet<IdFetcher> getIdFetchers(ImportFormatPreferences importFormatPreferences) {
        SortedSet<IdFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new CrossRef());
        set.add(new ArXiv(importFormatPreferences));
        return set;
    }

    /**
     * @return set containing fulltext fetchers
     */
    public static Set<FulltextFetcher> getFullTextFetchers(ImportFormatPreferences importFormatPreferences) {
        Set<FulltextFetcher> fetchers = new HashSet<>();
        // Original
        fetchers.add(new DoiResolution());
        // Publishers
        fetchers.add(new ScienceDirect());
        fetchers.add(new SpringerLink());
        fetchers.add(new ACS());
        fetchers.add(new ArXiv(importFormatPreferences));
        fetchers.add(new IEEE(importFormatPreferences));
        // Meta search
        fetchers.add(new GoogleScholar(importFormatPreferences));
        fetchers.add(new OpenAccessDoi());

        return fetchers;
    }
}
