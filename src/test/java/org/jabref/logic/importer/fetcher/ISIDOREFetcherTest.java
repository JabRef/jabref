package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class ISIDOREFetcherTest {

    private ISIDOREFetcher fetcher;

    @BeforeEach
    public void setup() {
        this.fetcher = new ISIDOREFetcher();
    }

    @Test
    public void checkArticle() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Investigating day-to-day variability of transit usage on a multimonth scale with smart card data. A case study in Lyon")
                .withField(StandardField.AUTHOR, "Oscar Egu and Patrick Bonnel")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Travel Behaviour and Society")
                .withField(StandardField.PUBLISHER, "HAL CCSD, Elsevier")
                .withField(StandardField.ABSTRACT, "To examine the variability of travel behaviour over time, transportation researchers need to collect longitudinal data. The first studies around day-to-day variability of travel behaviour were based on surveys. Those studies have shown that there is considerable variation in individual travel behaviour. They have also discussed the implications of this variability in terms of modelling, policy evaluation or marketing. Recently, the multiplication of big data has led to an explosion in the number of studies about travel behaviour. This is because those new data sources collect lots of data, about lots of people over long periods. In the field of public transit, smart card data is one of those big data sources. They have been used by various authors to conduct longitudinal analyses of transit usage behaviour. However, researchers working with smart card data mostly rely on clustering techniques to measure variability, and they often use conceptual framework different from those of transportation researchers familiar with traditional data sources. In particular, there is no study based on smart card data that explicitly measure day-to-day intrapersonal variability of transit usage. Therefore, the purpose of this investigation is to address this gap. To do this, a clustering method and a similarity metric are combined to explore simultaneously interpersonal and intrapersonal variability of transit usage. The application is done with a rich dataset covering a 6 months period (181 days) and it contributes to the growing literature on smart card data. Results of this research confirm previous works based on survey data and show that there is no one size fits all approach to the problem of day-to-day variability of transit usage. They also prove that combining clustering algorithm with day-to-day intrapersonal similarity metric is a valuable tool to mine smart card data. The findings of this study can help in identifying new passenger segmentation and in tailoring information and services.")
                .withField(StandardField.DOI, "10.1016/j.tbs.2019.12.003")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.hrzlqd");

        Optional<BibEntry> optionalBibEntry = fetcher.performSearchById("https://isidore.science/document/10670/1.hrzlqd");

        BibEntry actual = optionalBibEntry.get();

        assertEquals(expected.getField(StandardField.TYPE), actual.getField(StandardField.TYPE));
        assertEquals(expected.getField(StandardField.TITLE), actual.getField(StandardField.TITLE));
        assertEquals(expected.getField(StandardField.AUTHOR), actual.getField(StandardField.AUTHOR));
        assertEquals(expected.getField(StandardField.YEAR), actual.getField(StandardField.YEAR));
        assertEquals(expected.getField(StandardField.JOURNAL), actual.getField(StandardField.JOURNAL));
        assertEquals(expected.getField(StandardField.PUBLISHER), actual.getField(StandardField.PUBLISHER));
        assertEquals(expected.getField(StandardField.ABSTRACT), actual.getField(StandardField.ABSTRACT));
        assertEquals(expected.getField(StandardField.DOI), actual.getField(StandardField.DOI));
        assertEquals(expected.getField(StandardField.URL), actual.getField(StandardField.URL));
    }

    @Test
    public void checkArticle2() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "“ Anthony B. Atkinson, Inequality – What Can Be Done ? Cambridge (Mass.) Harvard University Press, 2015, XI-384 p. ”")
                .withField(StandardField.AUTHOR, "Benoît Rapoport")
                .withField(StandardField.YEAR, "2016")
                .withField(StandardField.JOURNAL, "Population (édition française)")
                .withField(StandardField.PUBLISHER, "HAL CCSD, INED - Institut national d’études démographiques")
                .withField(StandardField.ABSTRACT, "The economist Anthony Atkinson has studied the issues of poverty and inequality for over four decades; an index of inequality is named after him. In his most recent work, Atkinson offers a fascinating, encouraging guide to what could be done to reduce monetary inequality. The importance of equal opportunity should not lead us to ignore inequality in outcomes, he explains, not only because the same efforts do not always produce the same results but also because inequality of outcome for one generation can become inequality of opportunity for the next: “today’s ex-post outcomes shape tomorrow’s ex-ante playing field” (p. 11). And though income is only one dimension, it is a major source of inequality. The book is divided into three parts: diagnosis; 15 proposals for action that, if implemented, would reduce inequality; and answers to the main objections to taking those actions.")
                .withField(StandardField.DOI, "10.3917/popu.1601.0153")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.d2vlam");

        Optional<BibEntry> optionalBibEntry = fetcher.performSearchById("d2vlam");

        BibEntry actual = optionalBibEntry.get();

        assertEquals(expected.getField(StandardField.TYPE), actual.getField(StandardField.TYPE));
        assertEquals(expected.getField(StandardField.TITLE), actual.getField(StandardField.TITLE));
        assertEquals(expected.getField(StandardField.AUTHOR), actual.getField(StandardField.AUTHOR));
        assertEquals(expected.getField(StandardField.YEAR), actual.getField(StandardField.YEAR));
        assertEquals(expected.getField(StandardField.JOURNAL), actual.getField(StandardField.JOURNAL));
        assertEquals(expected.getField(StandardField.PUBLISHER), actual.getField(StandardField.PUBLISHER));
        assertEquals(expected.getField(StandardField.ABSTRACT), actual.getField(StandardField.ABSTRACT));
        assertEquals(expected.getField(StandardField.DOI), actual.getField(StandardField.DOI));
        assertEquals(expected.getField(StandardField.URL), actual.getField(StandardField.URL));
    }

    @Test
    public void checkThesis() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Thesis)
                .withField(StandardField.TITLE, "Mapping English L2 errors : an integrated system and textual approach")
                .withField(StandardField.AUTHOR, "Clive Hamilton")
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.ABSTRACT, "The main objective of this study is to try and pinpoint the frontier between grammatical (or sentence-level) errors on the one hand and textual errors on the other in university student essays. Accordingly, a corpus of English L2 learner texts, written by French learners, was collected and annotated using several annotation schemes. The first annotation scheme used is based on a model from the UAM CorpusTool software package, which provided us with an integrated error taxonomy. The annotations obtained were then cross-analyzed using the semantic metafunctions identified in systemic functional linguistics.In addition to providing statistics in terms of specific error frequency, our cross analysis has identified some areas that appear to pose particularly difficult problems, i.e. phraseology, and certain semantic and textual constructions. A classification of what we have called textual acceptability errors has thus been established. In short, the thesis begins with an examination of conceptual issues and ends with the proposal for an explanatory model that can describe erroneous occurrences identified in a foreign language – whether they are grammatical (i.e., linked to the language system itself) or textual (i.e. linked to the text) in nature.")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.m05oth");

        Optional<BibEntry> optionalBibEntry = fetcher.performSearchById("1.m05oth");

        BibEntry actual = optionalBibEntry.get();

        assertEquals(expected.getField(StandardField.TYPE), actual.getField(StandardField.TYPE));
        assertEquals(expected.getField(StandardField.TITLE), actual.getField(StandardField.TITLE));
        assertEquals(expected.getField(StandardField.AUTHOR), actual.getField(StandardField.AUTHOR));
        assertEquals(expected.getField(StandardField.YEAR), actual.getField(StandardField.YEAR));
        assertEquals(expected.getField(StandardField.JOURNAL), actual.getField(StandardField.JOURNAL));
        assertEquals(expected.getField(StandardField.PUBLISHER), actual.getField(StandardField.PUBLISHER));
        assertEquals(expected.getField(StandardField.ABSTRACT), actual.getField(StandardField.ABSTRACT));
        assertEquals(expected.getField(StandardField.DOI), actual.getField(StandardField.DOI));
        assertEquals(expected.getField(StandardField.URL), actual.getField(StandardField.URL));
    }

    @Test
    public void checkArticle3() throws FetcherException {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Salvage Lymph Node Dissection for Nodal Recurrent Prostate Cancer: A Systematic Review.")
                .withField(StandardField.AUTHOR, "G. Ploussard and G. Gandaglia and H. Borgmann and P. de Visschere and I. Heidegger and A. Kretschmer and R. Mathieu and C. Surcel and D. Tilki and I. Tsaur and M. Valerio and R. van den Bergh and P. Ost and A. Briganti")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.JOURNAL, "European urology")
                .withField(StandardField.ABSTRACT, "Identification of early nodal recurrence after primary prostate cancer (PCa) treatment by functional imaging may guide metastasis-directed therapy such as salvage lymph node dissection (SLND). The aim of this systematic review was to assess the oncological role and the safety of SLND in the era of modern imaging in case of exclusive nodal recurrence after primary PCa treatment with curative intent. A systematic literature search in the PubMed and Cochrane databases was performed up to August 2018 according to Preferred Reporting Items for Systematic Reviews and Meta-analysis guidelines. Overall, 27 SLND series have been selected for synthesis. Prostate-specific membrane antigen or choline positron emission tomography/computed tomography was the reference detection technique. SLND was performed by open or laparoscopic approach with &lt;10% of grade 3 or more complication rate. Mean follow-up was 29.4 mo. Complete biochemical response after SLND was achieved in 13-79.5%of cases (mean 44.3%). The 2- and 5-yr biochemical progression-free survival rates ranged from 23% to 64% and from 6% to 31%, respectively. Fiver-year overall survival was approximately 84%. Main drawbacks limiting the interpretation of the effectiveness of SLND were the retrospective design of single-center series, heterogeneity between series in terms of adjuvant treatment, endpoints, definitions of progression and study population, as well as the absence of long-term follow-up. A growing body of accumulated data suggests that SLND is a safe metastasis-directed therapy option in nodal recurrence after primary treatment. However, to date, high level of evidence is still missing to draw any clinically meaningful conclusion about the oncological impact of SLND on long-term endpoints. When imaging identifies exclusive nodal recurrent prostate cancer, surgery directed to the positive lesions is safe and can offer at least a temporary biochemical response. The oncological role assessed by strong clinical endpoints remains uncertain.")
                .withField(StandardField.DOI, "10.1016/j.eururo.2018.10.041")
                .withField(StandardField.URL, "https://isidore.science/document/10670/1.zm7q2x");

        Optional<BibEntry> optionalBibEntry = fetcher.performSearchById("https://isidore.science/document/10670/1.zm7q2x");

        BibEntry actual = optionalBibEntry.get();

        assertEquals(expected.getField(StandardField.TYPE), actual.getField(StandardField.TYPE));
        assertEquals(expected.getField(StandardField.TITLE), actual.getField(StandardField.TITLE));
        assertEquals(expected.getField(StandardField.AUTHOR), actual.getField(StandardField.AUTHOR));
        assertEquals(expected.getField(StandardField.YEAR), actual.getField(StandardField.YEAR));
        assertEquals(expected.getField(StandardField.JOURNAL), actual.getField(StandardField.JOURNAL));
        assertEquals(expected.getField(StandardField.PUBLISHER), actual.getField(StandardField.PUBLISHER));
        assertEquals(expected.getField(StandardField.ABSTRACT), actual.getField(StandardField.ABSTRACT));
        assertEquals(expected.getField(StandardField.DOI), actual.getField(StandardField.DOI));
        assertEquals(expected.getField(StandardField.URL), actual.getField(StandardField.URL));
    }
}
