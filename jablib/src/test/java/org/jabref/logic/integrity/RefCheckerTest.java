package org.jabref.logic.integrity;

import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefCheckerTest {
    private static final ImportFormatPreferences IMPORT_FORMAT_PREFERENCES = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    public BibEntry realEntry = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("Decker_2007")
            .withField(StandardField.AUTHOR, "Decker, Gero and Kopp, Oliver and Leymann, Frank and Weske, Mathias")
            .withField(StandardField.BOOKTITLE, "IEEE International Conference on Web Services (ICWS 2007)")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.TITLE, "BPEL4Chor: Extending BPEL for Modeling Choreographies")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.PAGES, "296--303")
            .withField(StandardField.DOI, "10.1109/icws.2007.59");
    public BibEntry realEntryNoDoi = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("Decker_2007")
            .withField(StandardField.AUTHOR, "Decker, Gero and Kopp, Oliver and Leymann, Frank and Weske, Mathias")
            .withField(StandardField.BOOKTITLE, "IEEE International Conference on Web Services (ICWS 2007)")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.TITLE, "BPEL4Chor: Extending BPEL for Modeling Choreographies")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.PAGES, "296--303");
    public BibEntry realEntryArxiv = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.TITLE, "The Architecture of Mr. DLib's Scientific Recommender-System API")
            .withField(StandardField.DATE, "2018-11-26")
            .withField(StandardField.ABSTRACT, "Recommender systems in academia are not widely available. This may be in part due to the difficulty and cost of developing and maintaining recommender systems. Many operators of academic products such as digital libraries and reference managers avoid this effort, although a recommender system could provide significant benefits to their users. In this paper, we introduce Mr. DLib's \"Recommendations as-a-Service\" (RaaS) API that allows operators of academic products to easily integrate a scientific recommender system into their products. Mr. DLib generates recommendations for research articles but in the future, recommendations may include call for papers, grants, etc. Operators of academic products can request recommendations from Mr. DLib and display these recommendations to their users. Mr. DLib can be integrated in just a few hours or days; creating an equivalent recommender system from scratch would require several months for an academic operator. Mr. DLib has been used by GESIS Sowiport and by the reference manager JabRef. Mr. DLib is open source and its goal is to facilitate the application of, and research on, scientific recommender systems. In this paper, we present the motivation for Mr. DLib, the architecture and details about the effectiveness. Mr. DLib has delivered 94m recommendations over a span of two years with an average click-through rate of 0.12%.")
            .withField(StandardField.EPRINT, "1811.10364")
            .withField(StandardField.FILE, ":http\\://arxiv.org/pdf/1811.10364v1:PDF")
            .withField(StandardField.EPRINTTYPE, "arXiv")
            .withField(StandardField.EPRINTCLASS, "cs.IR")
            .withField(new UnknownField("copyright"), "arXiv.org perpetual, non-exclusive license")
            .withField(InternalField.KEY_FIELD, "https://doi.org/10.48550/arxiv.1811.10364")
            .withField(StandardField.YEAR, "2018")
            .withField(StandardField.KEYWORDS, "Information Retrieval (cs.IR), Artificial Intelligence (cs.AI), Digital Libraries (cs.DL), Machine Learning (cs.LG), FOS: Computer and information sciences")
            .withField(StandardField.AUTHOR, "Beel, Joeran and Collins, Andrew and Aizawa, Akiko")
            .withField(StandardField.PUBLISHER, "arXiv")
            .withField(StandardField.DOI, "10.48550/ARXIV.1811.10364");
    public BibEntry closeToRealEntry = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("Decker_2007")
            .withField(StandardField.AUTHOR, "Decker, Gero and Kopp, Oliver and Leymann, Frank and Weske, Mathias")
            .withField(StandardField.BOOKTITLE, "IEEE International Conference on Web Services (ICWS 2007)")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.TITLE, "BPEL4Chor: Extending BPEL for Modeling Choreographies")
            .withField(StandardField.YEAR, "2008") // Incorrect Field
            .withField(StandardField.PAGES, "296--303")
            .withField(StandardField.DOI, "10.1109/icws.2007.59");
    public BibEntry fakeEntry = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("Decker_2003")
            .withField(StandardField.AUTHOR, "Kopp, Oliver")
            .withField(StandardField.BOOKTITLE, "IEEE International Conference on Web Services (ICWS 2007)")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.TITLE, "Some Title")
            .withField(StandardField.YEAR, "2013")
            .withField(StandardField.PAGES, "296--303");

    public RefChecker refChecker;

    @BeforeAll
    public static void setUpAll() {
        when(IMPORT_FORMAT_PREFERENCES.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        // Used during DOI fetch process
        when(IMPORT_FORMAT_PREFERENCES.fieldPreferences().getNonWrappableFields()).thenReturn(
                FXCollections.observableArrayList(List.of(
                        StandardField.PDF,
                        StandardField.PS,
                        StandardField.URL,
                        StandardField.DOI,
                        StandardField.FILE,
                        StandardField.ISBN,
                        StandardField.ISSN)));
    }

    @BeforeEach
    public void setUp() {
        ArXivFetcher af = new ArXivFetcher(IMPORT_FORMAT_PREFERENCES);
        DoiFetcher df = new DoiFetcher(IMPORT_FORMAT_PREFERENCES);
        this.refChecker = new RefChecker(df, af);
    }

    @Test
    void findsRealEntry() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.referenceValidityOfEntry(realEntry);
        assertEquals(RefChecker.Real.class, rv.getClass());
    }

    @Test
    void findsRealEntryFromDoi() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.validityFromDoiFetcher(realEntry);
        assertEquals(RefChecker.Real.class, rv.getClass());
    }

    @Test
    void closeToRealEntry() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.referenceValidityOfEntry(closeToRealEntry);
        assertEquals(RefChecker.Unsure.class, rv.getClass());
    }

    @Test
    void findsRealEntryWithoutDoi() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.referenceValidityOfEntry(realEntryNoDoi);
        assertEquals(RefChecker.Real.class, rv.getClass());
    }

    @Test
    void noFakeEntry() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.referenceValidityOfEntry(fakeEntry);
        assertEquals(RefChecker.Fake.class, rv.getClass());
    }

    @Test
    void findsRealFromArxiv() throws FetcherException {
        RefChecker.ReferenceValidity rv = refChecker.referenceValidityOfEntry(realEntryArxiv);
        assertEquals(RefChecker.Real.class, rv.getClass());
        assertEquals(RefChecker.Real.class, refChecker.validityFromArxiv(realEntryArxiv).getClass());
    }

    @Test
    void validateListOfEntriesTest() throws FetcherException {
        List<BibEntry> entries = List.of(realEntry, realEntryNoDoi, fakeEntry);
        Map<BibEntry, RefChecker.ReferenceValidity> e = refChecker.validateListOfEntries(entries);

        assertEquals(3, e.size());
        assertEquals(RefChecker.Real.class, e.get(realEntry).getClass());
        assertEquals(RefChecker.Real.class, e.get(realEntryNoDoi).getClass());
        assertEquals(RefChecker.Fake.class, e.get(fakeEntry).getClass());
    }

    @Nested
    public class ReferenceValidityTest {
        @Test
        void realEquals() {
            RefChecker.ReferenceValidity t1 = new RefChecker.Real(realEntry);
            RefChecker.ReferenceValidity t2 = new RefChecker.Real(realEntry);
            assertEquals(t1, t2);
            assertNotEquals(t1, new RefChecker.Real(fakeEntry));
        }

        @Test
        void fakeEquals() {
            RefChecker.ReferenceValidity t1 = new RefChecker.Real(null);
            RefChecker.ReferenceValidity t2 = new RefChecker.Fake();

            assertNotEquals(t1, t2);

            assertEquals(t2, new RefChecker.Fake());
        }

        @Test
        void orTest() {
            RefChecker.ReferenceValidity t1 = new RefChecker.Real(realEntry);
            RefChecker.ReferenceValidity t2 = new RefChecker.Real(fakeEntry);
            RefChecker.ReferenceValidity t3 = new RefChecker.Fake();
            assertEquals(t1, t1.or(t2));
            assertEquals(t1, t1.or(t3));
            assertEquals(t2, t3.or(t2));
        }

        @Test
        void unsureTest() {
            RefChecker.ReferenceValidity t1 = new RefChecker.Unsure(realEntry);
            RefChecker.ReferenceValidity t2 = new RefChecker.Unsure(fakeEntry);
            assertNotEquals(t1, t2);
            RefChecker.ReferenceValidity result = t1.or(t2);
            RefChecker.ReferenceValidity otherResult = t2.or(t1);
            assertEquals(result, otherResult);
        }
    }
}
