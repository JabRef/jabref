package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.CrossRef;
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
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

public class WebFetchers {

    public static Optional<IdBasedFetcher> getIdBasedFetcherForField(String field, ImportFormatPreferences preferences) {
        IdBasedFetcher fetcher;
        switch (field) {
            case FieldName.DOI:
                fetcher = new DoiFetcher(preferences);
                break;
            case FieldName.ISBN:
                fetcher = new IsbnFetcher(preferences);
                break;
            case FieldName.EPRINT:
                fetcher = new ArXiv(preferences);
                break;
            default:
                return Optional.empty();
        }
        return Optional.of(fetcher);
    }

    @SuppressWarnings("unchecked")
    public static <T> IdFetcher<T> getIdFetcherForIdentifier(Class<T> clazz) {
        if (clazz == DOI.class) {
            return (IdFetcher<T>) new CrossRef();
        } else {
            throw new IllegalArgumentException("No fetcher found for identifier" + clazz.getCanonicalName());
        }
    }

    public static List<SearchBasedFetcher> getSearchBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        ArrayList<SearchBasedFetcher> list = new ArrayList<>();
        list.add(new ArXiv(importFormatPreferences));
        list.add(new GvkFetcher());
        list.add(new MedlineFetcher());
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.add(new zbMATH(importFormatPreferences));
        list.add(new GoogleScholar(importFormatPreferences));
        list.add(new DBLPFetcher(importFormatPreferences));
        list.add(new CrossRef());
        list.sort(Comparator.comparing(WebFetcher::getName));
        return list;
    }

    public static List<IdBasedFetcher> getIdBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        ArrayList<IdBasedFetcher> list = new ArrayList<>();
        list.add(new ArXiv(importFormatPreferences));
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new IsbnFetcher(importFormatPreferences));
        list.add(new DiVA(importFormatPreferences));
        list.add(new DoiFetcher(importFormatPreferences));
        list.add(new MedlineFetcher());
        list.add(new TitleFetcher(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.add(new CrossRef());
        list.sort(Comparator.comparing(WebFetcher::getName));
        return list;
    }

    public static List<EntryBasedFetcher> getEntryBasedFetchers(ImportFormatPreferences importFormatPreferences) {
        ArrayList<EntryBasedFetcher> list = new ArrayList<>();
        list.add(new AstrophysicsDataSystem(importFormatPreferences));
        list.add(new DoiFetcher(importFormatPreferences));
        list.add(new MathSciNet(importFormatPreferences));
        list.add(new CrossRef());
        list.sort(Comparator.comparing(WebFetcher::getName));
        return list;
    }
}
