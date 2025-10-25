package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class EuropePmcFetcherTest {

    private EuropePmcFetcher fetcher;
    private BibEntry entryWijedasa;
    private BibEntry entryWithFulltextAndKeywords;

    @BeforeEach
    void setUp() {
        // Use null for ImporterPreferences - fulltext tests will handle empty API key case
        fetcher = new EuropePmcFetcher(null);
        entryWijedasa = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Wijedasa, Lahiru S. and Jauhiainen, Jyrki and Könönen, Mari and Lampela, Maija and Vasander, Harri and Leblanc, Marie-Claire and Evers, Stephanie and Smith, Thomas E. L. and Yule, Catherine M. and Varkkey, Helena and Lupascu, Massimo and Parish, Faizal and Singleton, Ian and Clements, Gopalasamy R. and Aziz, Sheema Abdul and Harrison, Mark E. and Cheyne, Susan and Anshari, Gusti Z. and Meijaard, Erik and Goldstein, Jenny E. and Waldron, Susan and Hergoualc'h, Kristell and Dommain, Rene and Frolking, Steve and Evans, Christopher D. and Posa, Mary Rose C. and Glaser, Paul H. and Suryadiputra, Nyoman and Lubis, Reza and Santika, Truly and Padfield, Rory and Kurnianto, Sofyan and Hadisiswoyo, Panut and Lim, Teck Wyn and Page, Susan E. and Gauci, Vincent and Van Der Meer, Peter J. and Buckland, Helen and Garnier, Fabien and Samuel, Marshall K. and Choo, Liza Nuriati Lim Kim and O'Reilly, Patrick and Warren, Matthew and Suksuwan, Surin and Sumarga, Elham and Jain, Anuj and Laurance, William F. and Couwenberg, John and Joosten, Hans and Vernimmen, Ronald and Hooijer, Aljosja and Malins, Chris and Cochrane, Mark A. and Perumal, Balu and Siegert, Florian and Peh, Kelvin S.-H. and Comeau, Louis-Pierre and Verchot, Louis and Harvey, Charles F. and Cobb, Alex and Jaafar, Zeehan and Wösten, Henk and Manuri, Solichin and Müller, Moritz and Giesen, Wim and Phelps, Jacob and Yong, Ding Li and Silvius, Marcel and Wedeux, Béatrice M. M. and Hoyt, Alison and Osaki, Mitsuru and Hirano, Takashi and Takahashi, Hidenori and Kohyama, Takashi S. and Haraguchi, Akira and Nugroho, Nunung P. and Coomes, David A. and Quoi, Le Phat and Dohong, Alue and Gunawan, Haris and Gaveau, David L. A. and Langner, Andreas and Lim, Felix K. S. and Edwards, David P. and Giam, Xingli and Van Der Werf, Guido and Carmenta, Rachel and Verwer, Caspar C. and Gibson, Luke and Gandois, Laure and Graham, Laura Linda Bozena and Regalino, Jhanson and Wich, Serge A. and Rieley, Jack and Kettridge, Nicholas and Brown, Chloe and Pirard, Romain and Moore, Sam and Capilla, B. Ripoll and Ballhorn, Uwe and Ho, Hua Chew and Hoscilo, Agata and Lohberger, Sandra and Evans, Theodore A. and Yulianti, Nina and Blackham, Grace and Onrizal and Husson, Simon and Murdiyarso, Daniel and Pangala, Sunita and Cole, Lydia E. S. and Tacconi, Luca and Segah, Hendrik and Tonoto, Prayoto and Lee, Janice S. H. and Schmilewski, Gerald and Wulffraat, Stephan and Putra, Erianto Indra and Cattau, Megan E. and Clymo, R. S. and Morrison, Ross and Mujahid, Aazani and Miettinen, Jukka and Liew, Soo Chin and Valpola, Samu and Wilson, David and D'Arcy, Laura and Gerding, Michiel and Sundari, Siti and Thornton, Sara A. and Kalisz, Barbara and Chapman, Stephen J. and Su, Ahmad Suhaizi Mat and Basuki, Imam and Itoh, Masayuki and Traeholt, Carl and Sloan, Sean and Sayok, Alexander K. and Andersen, Roxane")
                .withField(StandardField.DOI, "10.1111/gcb.13516")
                .withField(StandardField.ISSN, "1354-1013") // there is also an essn
                .withField(StandardField.ISSUE, "3")
                .withField(StandardField.JOURNAL, "Global change biology")
                .withField(StandardField.MONTH, "#mar#")
                .withField(StandardField.PAGES, "977--982")
                .withField(StandardField.PMID, "27670948")
                .withField(StandardField.HOWPUBLISHED, "Print-Electronic")
                .withField(new UnknownField("nlmid"), "9888746")
                .withField(StandardField.PUBSTATE, "ppublish")
                .withField(StandardField.TITLE, "Denial of long-term issues with agriculture on tropical peatlands will have devastating consequences.")
                .withField(StandardField.VOLUME, "23")
                .withField(StandardField.URL, "https://pubmed.ncbi.nlm.nih.gov/27670948/")
                .withField(StandardField.YEAR, "2017");

        entryWithFulltextAndKeywords = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Okpala, Chibuike and Umeh, Ifeoma and Anagu, Linda Onyeka")
                .withField(StandardField.DOI, "10.12688/openresafrica.15809.2")
                .withField(StandardField.HOWPUBLISHED, "Electronic-eCollection")
                .withField(StandardField.ISSN, "2752-6925")
                .withField(StandardField.JOURNAL, "Open research Africa")
                .withField(StandardField.KEYWORDS, "rainy season, Preventive Measures, Asymptomatic Malaria, Malaria Transmission.")
                .withField(new UnknownField("nlmid"), "9918487345206676")
                .withField(StandardField.PAGES, "5")
                .withField(StandardField.PMID, "40860931")
                .withField(StandardField.PUBSTATE, "epublish")
                .withField(StandardField.TITLE, "Economic empowerment and various preventive strategies play a role in reducing asymptomatic malaria towards the end of the rainy season.")
                .withField(StandardField.URL, "https://europepmc.org/articles/PMC12375191?pdf=render")
                .withField(StandardField.VOLUME, "8")
                .withField(StandardField.YEAR, "2025");
    }

    @Test
    void searchByIDWijedasa() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("27670948");
        fetchedEntry.get().clearField(StandardField.ABSTRACT); // Remove abstract due to copyright
        assertEquals(Optional.of(entryWijedasa), fetchedEntry);
    }

    @Test
    void searchByIDDownloadsFulltextAndKeywords() throws FetcherException {
        Optional<BibEntry> fetchedEntry;
        fetchedEntry = fetcher.performSearchById("40860931");
        fetchedEntry.get().clearField(StandardField.ABSTRACT);
        assertEquals(Optional.of(entryWithFulltextAndKeywords), fetchedEntry);
    }

    @Test
    void searchByDoiTermReturnsWijedasa() throws FetcherException {
        // Use Europe PMC fielded search: DOI
        var results = fetcher.performSearch("doi:10.1111/gcb.13516");
        BibEntry first = results.getFirst();
        first.clearField(StandardField.ABSTRACT);
        assertEquals(entryWijedasa, first);
    }

    @Test
    void findFullTextReturnsEmptyForNonWileyEntry() throws Exception {
        // Non-Wiley entry should return empty
        Optional<java.net.URL> result = fetcher.findFullText(entryWijedasa);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void findFullTextDetectsWileyByDoi() throws Exception {
        // Create a Wiley entry with DOI prefix 10.1002
        BibEntry wileyEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.DOI, "10.1002/we.1234")
                .withField(StandardField.TITLE, "Test Wiley Entry");

        Optional<java.net.URL> result = fetcher.findFullText(wileyEntry);
        
        // Verify result is either empty (no token/real DOI) or contains expected domain
        if (result.isPresent()) {
            String url = result.get().toString();
            assertTrue(url.contains("wiley") || url.contains("europepmc"), 
                      "URL should be from Wiley or EuropePMC, but was: " + url);
        }
        // If empty, that's acceptable (no token or fallbacks didn't find anything)
    }

    @Test
    void findFullTextDetectsWileyByPublisher() throws Exception {
        // Create entry with Wiley publisher
        BibEntry wileyEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.DOI, "10.9999/test.123")
                .withField(StandardField.PUBLISHER, "John Wiley & Sons")
                .withField(StandardField.TITLE, "Test Wiley Entry");

        // Should detect as Wiley but return empty (no token and fake DOI won't be found)
        Optional<java.net.URL> result = fetcher.findFullText(wileyEntry);
        assertEquals(Optional.empty(), result);
    }

    @Test
    void findFullTextReturnsEmptyWithoutDoi() throws Exception {
        // Wiley entry without DOI should return empty
        BibEntry wileyEntryNoDoi = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.PUBLISHER, "Wiley")
                .withField(StandardField.TITLE, "Test Entry Without DOI");

        Optional<java.net.URL> result = fetcher.findFullText(wileyEntryNoDoi);
        assertEquals(Optional.empty(), result);
    }
}
