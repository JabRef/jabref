package org.jabref.logic.refcheck;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
class RefCheckerTest {

    private RefChecker refChecker;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences =
                mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        refChecker = new RefChecker(
                new DoiFetcher(importFormatPreferences),
                new ArXivFetcher(importFormatPreferences)
        );
    }

    @Test
    void realPaperWithCorrectDoiIsClassifiedAsReal() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam and Parmar, Niki")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.DOI, "10.48550/arXiv.1706.03762");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void entryWithCorrectDoiButWrongMetadataIsNotClassifiedAsReal() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Not a Real Paper")
                .withField(StandardField.AUTHOR, "Random Author")
                .withField(StandardField.YEAR, "2099")
                .withField(StandardField.DOI, "10.48550/arXiv.1706.03762");

        RefCheckResult result = refChecker.check(entry);

        assertNotEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void entryThatDoesNotExistAnywhereIsClassifiedAsFake() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Nonexistent Paper with no Database")
                .withField(StandardField.AUTHOR, "No Author")
                .withField(StandardField.YEAR, "1800");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.FAKE, result.validity());
    }

    @Test
    void entryWithSlightlyWrongTitleIsClassifiedAsUnsureOrReal() {
        // Correct DOI and author but title has minor differences
        // Score should be high but result depends on similarity
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All that You Need!")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam and Parmar, Niki and others")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.DOI, "10.48550/arXiv.1706.03762");

        RefCheckResult result = refChecker.check(entry);

        assertNotEquals(RefValidity.FAKE, result.validity());
    }

    @Test
    void entryWithNoIdentifierIsValidatedViaCrossRef() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Attention Is All You Need")
                .withField(StandardField.AUTHOR, "Vaswani, Ashish and Shazeer, Noam and Parmar, Niki and Uszkoreit")
                .withField(StandardField.YEAR, "2017");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void entryWithOnlyArXivIdIsValidatedViaArXiv() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Language Models are Few-Shot Learners")
                .withField(StandardField.EPRINT, "2005.14165")
                .withField(StandardField.EPRINTTYPE, "arXiv")
                .withField(StandardField.ARCHIVEPREFIX, "arXiv");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void entryWithCompletelyWrongAuthorIsNotClassifiedAsReal() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Deep Residual Learning for Image Recognition")
                .withField(StandardField.AUTHOR, "Wrong Author")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.DOI, "10.1109/CVPR.2016.90");

        RefCheckResult result = refChecker.check(entry);

        assertNotEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void entryWhoseDOIResolvesToDifferentPaperIsNotClassifiedAsReal() {
        // Entry is different paper and the DOI points to "Attention Is All You Need"
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Efficient Neural Network Pruning Using Iterative Sparse Retraining")
                .withField(StandardField.AUTHOR, "Li, Shuang and Chen, Yifan")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.DOI, "10.48550/arXiv.1706.03762");

        RefCheckResult result = refChecker.check(entry);

        assertNotEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void bertPaperWithCorrectDoiIsClassifiedAsReal() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding")
                .withField(StandardField.AUTHOR, "Devlin, Jacob and Chang, Ming-Wei and Lee, Kenton and Toutanova, Kristina")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.DOI, "10.48550/arXiv.1810.04805");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.REAL, result.validity());
    }

    @Test
    void realMussgnugPaperIsClassifiedAsReal() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Interdisciplinary research: Friend or foe to ethical AI?")
                .withField(StandardField.AUTHOR, "Mussgnug, Alexander Martin")
                .withField(StandardField.YEAR, "2026")
                .withField(StandardField.DOI, "10.1017/cfc.2026.10015");

        RefCheckResult result = refChecker.check(entry);

        assertEquals(RefValidity.REAL, result.validity());
    }
}
