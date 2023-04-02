package org.jabref.logic.importer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.ACS;
import org.jabref.logic.importer.fetcher.ApsFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.BiodiversityLibrary;
import org.jabref.logic.importer.fetcher.BvbFetcher;
import org.jabref.logic.importer.fetcher.CollectionOfComputerScienceBibliographiesFetcher;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.DOABFetcher;
import org.jabref.logic.importer.fetcher.DOAJFetcher;
import org.jabref.logic.importer.fetcher.DiVA;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.DoiResolution;
import org.jabref.logic.importer.fetcher.GvkFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.INSPIREFetcher;
import org.jabref.logic.importer.fetcher.IacrEprintFetcher;
import org.jabref.logic.importer.fetcher.LibraryOfCongress;
import org.jabref.logic.importer.fetcher.MathSciNet;
import org.jabref.logic.importer.fetcher.MedlineFetcher;
import org.jabref.logic.importer.fetcher.Medra;
import org.jabref.logic.importer.fetcher.OpenAccessDoi;
import org.jabref.logic.importer.fetcher.ResearchGate;
import org.jabref.logic.importer.fetcher.RfcFetcher;
import org.jabref.logic.importer.fetcher.ScienceDirect;
import org.jabref.logic.importer.fetcher.SemanticScholar;
import org.jabref.logic.importer.fetcher.SpringerFetcher;
import org.jabref.logic.importer.fetcher.SpringerLink;
import org.jabref.logic.importer.fetcher.TitleFetcher;
import org.jabref.logic.importer.fetcher.ZbMATH;
import org.jabref.logic.importer.fetcher.isbntobibtex.EbookDeIsbnFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.preferences.FilePreferences;

import static org.jabref.model.entry.field.StandardField.EPRINT;
import static org.jabref.model.entry.field.StandardField.ISBN;

public class WebFetchers {

    private WebFetchers() {
    }

    public static Optional<IdBasedFetcher> getIdBasedFetcherForField(Field field, ImportFormatPreferences importFormatPreferences) {
        IdBasedFetcher fetcher;

        if (field == StandardField.DOI) {
            fetcher = new DoiFetcher(importFormatPreferences);
        } else if (field == ISBN) {
            fetcher = new IsbnFetcher(importFormatPreferences)
                    .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences));
                    // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences));
        } else if (field == EPRINT) {
            fetcher = new ArXivFetcher(importFormatPreferences);
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
    public static SortedSet<SearchBasedFetcher> getSearchBasedFetchers(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        SortedSet<SearchBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new ArXivFetcher(importFormatPreferences));
        set.add(new INSPIREFetcher(importFormatPreferences));
        set.add(new GvkFetcher());
        set.add(new BvbFetcher());
        set.add(new MedlineFetcher());
        set.add(new AstrophysicsDataSystem(importFormatPreferences, importerPreferences));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new ZbMATH(importFormatPreferences));
        set.add(new ACMPortalFetcher());
        // set.add(new GoogleScholar(importFormatPreferences));
        set.add(new DBLPFetcher(importFormatPreferences));
        set.add(new SpringerFetcher(importerPreferences));
        set.add(new CrossRef());
        // set.add(new CiteSeer());
        set.add(new DOAJFetcher(importFormatPreferences));
        set.add(new IEEE(importFormatPreferences, importerPreferences));
        set.add(new CompositeSearchBasedFetcher(set, 30));
        set.add(new CollectionOfComputerScienceBibliographiesFetcher(importFormatPreferences));
        set.add(new DOABFetcher());
        // set.add(new JstorFetcher(importFormatPreferences));
        set.add(new SemanticScholar());
        set.add(new ResearchGate(importFormatPreferences));
        set.add(new BiodiversityLibrary(importerPreferences));
        return set;
    }

    /**
     * @return sorted set containing id based fetchers
     */
    public static SortedSet<IdBasedFetcher> getIdBasedFetchers(ImportFormatPreferences importFormatPreferences,
                                                               ImporterPreferences importerPreferences) {
        SortedSet<IdBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new ArXivFetcher(importFormatPreferences));
        set.add(new AstrophysicsDataSystem(importFormatPreferences, importerPreferences));
        set.add(new IsbnFetcher(importFormatPreferences)
                .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences)));
                // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences)));
        set.add(new DiVA(importFormatPreferences));
        set.add(new DoiFetcher(importFormatPreferences));
        set.add(new MedlineFetcher());
        set.add(new TitleFetcher(importFormatPreferences));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new ZbMATH(importFormatPreferences));
        set.add(new CrossRef());
        set.add(new LibraryOfCongress(importFormatPreferences));
        set.add(new IacrEprintFetcher(importFormatPreferences));
        set.add(new RfcFetcher(importFormatPreferences));
        set.add(new Medra());
        // set.add(new JstorFetcher(importFormatPreferences));
        return set;
    }

    /**
     * @return sorted set containing entry based fetchers
     */
    public static SortedSet<EntryBasedFetcher> getEntryBasedFetchers(ImporterPreferences importerPreferences,
                                                                     ImportFormatPreferences importFormatPreferences,
                                                                     FilePreferences filePreferences,
                                                                     BibDatabaseContext databaseContext) {
        SortedSet<EntryBasedFetcher> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new INSPIREFetcher(importFormatPreferences));
        set.add(new AstrophysicsDataSystem(importFormatPreferences, importerPreferences));
        set.add(new DoiFetcher(importFormatPreferences));
        set.add(new IsbnFetcher(importFormatPreferences)
                .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences)));
                // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences)));
        set.add(new MathSciNet(importFormatPreferences));
        set.add(new CrossRef());
        set.add(new ZbMATH(importFormatPreferences));
        set.add(new PdfMergeMetadataImporter.EntryBasedFetcherWrapper(importFormatPreferences, filePreferences, databaseContext));
        set.add(new SemanticScholar());
        set.add(new ResearchGate(importFormatPreferences));
        return set;
    }

    /**
     * @return sorted set containing id fetchers
     */
    public static SortedSet<IdFetcher<? extends Identifier>> getIdFetchers(ImportFormatPreferences importFormatPreferences) {
        SortedSet<IdFetcher<?>> set = new TreeSet<>(Comparator.comparing(WebFetcher::getName));
        set.add(new CrossRef());
        set.add(new ArXivFetcher(importFormatPreferences));
        return set;
    }

    /**
     * @return set containing fulltext fetchers
     */
    public static Set<FulltextFetcher> getFullTextFetchers(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        Set<FulltextFetcher> fetchers = new HashSet<>();

        // Original
        fetchers.add(new DoiResolution(importFormatPreferences.doiPreferences()));

        // Publishers
        fetchers.add(new ScienceDirect(importerPreferences));
        fetchers.add(new SpringerLink(importerPreferences));
        fetchers.add(new ACS());
        fetchers.add(new ArXivFetcher(importFormatPreferences));
        fetchers.add(new IEEE(importFormatPreferences, importerPreferences));
        fetchers.add(new ApsFetcher());
        fetchers.add(new IacrEprintFetcher(importFormatPreferences));

        // Meta search
        // fetchers.add(new JstorFetcher(importFormatPreferences));
        // fetchers.add(new GoogleScholar(importFormatPreferences));
        fetchers.add(new OpenAccessDoi());
        fetchers.add(new SemanticScholar());
        fetchers.add(new ResearchGate(importFormatPreferences));
        return fetchers;
    }

    /**
     * @return set containing customizable api key fetchers
     */
    public static Set<CustomizableKeyFetcher> getCustomizableKeyFetchers(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        Set<CustomizableKeyFetcher> fetchers = new HashSet<>();
        fetchers.add(new IEEE(importFormatPreferences, importerPreferences));
        fetchers.add(new SpringerFetcher(importerPreferences));
        fetchers.add(new ScienceDirect(importerPreferences));
        fetchers.add(new AstrophysicsDataSystem(importFormatPreferences, importerPreferences));
        fetchers.add(new BiodiversityLibrary(importerPreferences));
        return fetchers;
    }
}
