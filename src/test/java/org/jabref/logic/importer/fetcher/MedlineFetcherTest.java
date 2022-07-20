package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class MedlineFetcherTest {

    private MedlineFetcher fetcher;
    private BibEntry entryWijedasa;
    private BibEntry entryEndharti;
    private BibEntry bibEntryIchikawa;
    private BibEntry bibEntrySari;

    @BeforeEach
    public void setUp() throws InterruptedException {
        // pause between runs to avoid 403 and 429 at Medline
        Thread.sleep(1000);

        fetcher = new MedlineFetcher();

        entryWijedasa = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Wijedasa, Lahiru S. and Jauhiainen, Jyrki and Könönen, Mari and Lampela, Maija and Vasander, Harri and Leblanc, Marie-Claire and Evers, Stephanie and Smith, Thomas E. L. and Yule, Catherine M. and Varkkey, Helena and Lupascu, Massimo and Parish, Faizal and Singleton, Ian and Clements, Gopalasamy R. and Aziz, Sheema Abdul and Harrison, Mark E. and Cheyne, Susan and Anshari, Gusti Z. and Meijaard, Erik and Goldstein, Jenny E. and Waldron, Susan and Hergoualc'h, Kristell and Dommain, Rene and Frolking, Steve and Evans, Christopher D. and Posa, Mary Rose C. and Glaser, Paul H. and Suryadiputra, Nyoman and Lubis, Reza and Santika, Truly and Padfield, Rory and Kurnianto, Sofyan and Hadisiswoyo, Panut and Lim, Teck Wyn and Page, Susan E. and Gauci, Vincent and Van Der Meer, Peter J. and Buckland, Helen and Garnier, Fabien and Samuel, Marshall K. and Choo, Liza Nuriati Lim Kim and O'Reilly, Patrick and Warren, Matthew and Suksuwan, Surin and Sumarga, Elham and Jain, Anuj and Laurance, William F. and Couwenberg, John and Joosten, Hans and Vernimmen, Ronald and Hooijer, Aljosja and Malins, Chris and Cochrane, Mark A. and Perumal, Balu and Siegert, Florian and Peh, Kelvin S.-H. and Comeau, Louis-Pierre and Verchot, Louis and Harvey, Charles F. and Cobb, Alex and Jaafar, Zeehan and Wösten, Henk and Manuri, Solichin and Müller, Moritz and Giesen, Wim and Phelps, Jacob and Yong, Ding Li and Silvius, Marcel and Wedeux, Béatrice M. M. and Hoyt, Alison and Osaki, Mitsuru and Hirano, Takashi and Takahashi, Hidenori and Kohyama, Takashi S. and Haraguchi, Akira and Nugroho, Nunung P. and Coomes, David A. and Quoi, Le Phat and Dohong, Alue and Gunawan, Haris and Gaveau, David L. A. and Langner, Andreas and Lim, Felix K. S. and Edwards, David P. and Giam, Xingli and Van Der Werf, Guido and Carmenta, Rachel and Verwer, Caspar C. and Gibson, Luke and Gandois, Laure and Graham, Laura Linda Bozena and Regalino, Jhanson and Wich, Serge A. and Rieley, Jack and Kettridge, Nicholas and Brown, Chloe and Pirard, Romain and Moore, Sam and Capilla, B. Ripoll and Ballhorn, Uwe and Ho, Hua Chew and Hoscilo, Agata and Lohberger, Sandra and Evans, Theodore A. and Yulianti, Nina and Blackham, Grace and Onrizal and Husson, Simon and Murdiyarso, Daniel and Pangala, Sunita and Cole, Lydia E. S. and Tacconi, Luca and Segah, Hendrik and Tonoto, Prayoto and Lee, Janice S. H. and Schmilewski, Gerald and Wulffraat, Stephan and Putra, Erianto Indra and Cattau, Megan E. and Clymo, R. S. and Morrison, Ross and Mujahid, Aazani and Miettinen, Jukka and Liew, Soo Chin and Valpola, Samu and Wilson, David and D'Arcy, Laura and Gerding, Michiel and Sundari, Siti and Thornton, Sara A. and Kalisz, Barbara and Chapman, Stephen J. and Su, Ahmad Suhaizi Mat and Basuki, Imam and Itoh, Masayuki and Traeholt, Carl and Sloan, Sean and Sayok, Alexander K. and Andersen, Roxane")
                .withField(new UnknownField("country"), "England")
                .withField(StandardField.DOI, "10.1111/gcb.13516")
                .withField(StandardField.ISSN, "1365-2486")
                .withField(new UnknownField("issn-linking"), "1354-1013")
                .withField(StandardField.ISSUE, "3")
                .withField(StandardField.JOURNAL, "Global change biology")
                .withField(StandardField.MONTH, "#mar#")
                .withField(new UnknownField("nlm-id"), "9888746")
                .withField(StandardField.OWNER, "NLM")
                .withField(StandardField.PAGES, "977--982")
                .withField(StandardField.PMID, "27670948")
                .withField(new UnknownField("pubmodel"), "Print-Electronic")
                .withField(StandardField.PUBSTATE, "ppublish")
                .withField(new UnknownField("revised"), "2019-11-20")
                .withField(StandardField.TITLE, "Denial of long-term issues with agriculture on tropical peatlands will have devastating consequences.")
                .withField(StandardField.VOLUME, "23")
                .withField(StandardField.YEAR, "2017");

        entryEndharti = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Dendrophthoe pentandra (L.) Miq extract effectively inhibits inflammation, proliferation and induces p53 expression on colitis-associated colon cancer.")
                .withField(StandardField.AUTHOR, "Endharti, Agustina Tri and Wulandari, Adisti and Listyana, Anik and Norahmawati, Eviana and Permana, Sofy")
                .withField(new UnknownField("country"), "England")
                .withField(StandardField.DOI, "10.1186/s12906-016-1345-0")
                .withField(new UnknownField("pii"), "10.1186/s12906-016-1345-0")
                .withField(new UnknownField("pmc"), "PMC5037598")
                .withField(StandardField.ISSN, "1472-6882")
                .withField(new UnknownField("issn-linking"), "1472-6882")
                .withField(StandardField.ISSUE, "1")
                .withField(StandardField.JOURNAL, "BMC complementary and alternative medicine")
                .withField(StandardField.KEYWORDS, "CAC; Dendrophtoe pentandra; IL-22; MPO; Proliferation; p53")
                .withField(new UnknownField("nlm-id"), "101088661")
                .withField(StandardField.OWNER, "NLM")
                .withField(StandardField.PAGES, "374")
                .withField(StandardField.MONTH, "#sep#")
                .withField(StandardField.PMID, "27670445")
                .withField(new UnknownField("pubmodel"), "Electronic")
                .withField(StandardField.PUBSTATE, "epublish")
                .withField(new UnknownField("revised"), "2022-04-08")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.YEAR, "2016");

        bibEntryIchikawa = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Ichikawa-Seki, Madoka and Guswanto, Azirwan and Allamanda, Puttik and Mariamah, Euis Siti and Wibowo, Putut Eko and Igarashi, Ikuo and Nishikawa, Yoshifumi")
                .withField(new UnknownField("chemicals"), "Antibodies, Protozoan, Antigens, Protozoan, GRA7 protein, Toxoplasma gondii, Protozoan Proteins")
                .withField(new UnknownField("citation-subset"), "IM")
                .withField(new UnknownField("completed"), "2016-07-26")
                .withField(new UnknownField("country"), "Netherlands")
                .withField(StandardField.DOI, "10.1016/j.parint.2015.07.004")
                .withField(StandardField.ISSN, "1873-0329")
                .withField(StandardField.PUBSTATE, "ppublish")
                .withField(new UnknownField("revised"), "2015-09-26")
                .withField(new UnknownField("issn-linking"), "1383-5769")
                .withField(StandardField.ISSUE, "6")
                .withField(StandardField.JOURNAL, "Parasitology international")
                .withField(StandardField.KEYWORDS, "Animals; Antibodies, Protozoan, blood; Antigens, Protozoan, immunology; Cattle, parasitology; Cattle Diseases, epidemiology, parasitology; Enzyme-Linked Immunosorbent Assay, veterinary; Geography; Humans; Indonesia, epidemiology; Livestock, immunology, parasitology; Meat, parasitology; Protozoan Proteins, immunology; Seroepidemiologic Studies; Swine, parasitology; Swine Diseases, epidemiology, parasitology; Toxoplasma, immunology; Toxoplasmosis, Animal, epidemiology, immunology, parasitology; Cattle; ELISA; Indonesia; Pig; TgGRA7; Toxoplasma gondii")
                .withField(StandardField.MONTH, "#dec#")
                .withField(new UnknownField("nlm-id"), "9708549")
                .withField(StandardField.OWNER, "NLM")
                .withField(StandardField.PAGES, "484--486")
                .withField(new UnknownField("pii"), "S1383-5769(15)00124-5")
                .withField(StandardField.PMID, "26197440")
                .withField(new UnknownField("pubmodel"), "Print-Electronic")
                .withField(StandardField.TITLE, "Seroprevalence of antibody to TgGRA7 antigen of Toxoplasma gondii in livestock animals from Western Java, Indonesia.")
                .withField(StandardField.VOLUME, "64")
                .withField(StandardField.YEAR, "2015");

        bibEntrySari = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Sari, Yulia and Haryati, Sri and Raharjo, Irvan and Prasetyo, Afiono Agung")
                .withField(new UnknownField("chemicals"), "Antibodies, Protozoan, Antibodies, Viral, HTLV-I Antibodies, HTLV-II Antibodies, Hepatitis Antibodies, Hepatitis B Antibodies, Hepatitis C Antibodies, Immunoglobulin G, Immunoglobulin M")
                .withField(new UnknownField("citation-subset"), "IM")
                .withField(new UnknownField("completed"), "2016-04-21")
                .withField(new UnknownField("country"), "Thailand")
                .withField(StandardField.ISSN, "0125-1562")
                .withField(new UnknownField("issn-linking"), "0125-1562")
                .withField(StandardField.ISSUE, "6")
                .withField(StandardField.JOURNAL, "The Southeast Asian journal of tropical medicine and public health")
                .withField(StandardField.KEYWORDS, "Antibodies, Protozoan; Antibodies, Viral, immunology; Coinfection, epidemiology, immunology; Female; HIV Infections, epidemiology; HTLV-I Antibodies, immunology; HTLV-I Infections, epidemiology, immunology; HTLV-II Antibodies, immunology; HTLV-II Infections, epidemiology, immunology; Hepatitis Antibodies, immunology; Hepatitis B Antibodies, immunology; Hepatitis C Antibodies, immunology; Hepatitis Delta Virus, immunology; Hepatitis, Viral, Human, epidemiology, immunology; Humans; Immunoglobulin G, immunology; Immunoglobulin M, immunology; Indonesia, epidemiology; Male; Prisoners; Seroepidemiologic Studies; Toxoplasma, immunology; Toxoplasmosis, epidemiology, immunology")
                .withField(StandardField.MONTH, "#nov#")
                .withField(StandardField.PUBSTATE, "ppublish")
                .withField(new UnknownField("revised"), "2018-12-02")
                .withField(new UnknownField("nlm-id"), "0266303")
                .withField(StandardField.OWNER, "NLM")
                .withField(StandardField.PAGES, "977--985")
                .withField(StandardField.PMID, "26867355")
                .withField(new UnknownField("pubmodel"), "Print")
                .withField(StandardField.TITLE, "TOXOPLASMA AND VIRAL ANTIBODIES AMONG HIV PATIENTS AND INMATES IN CENTRAL JAVA, INDONESIA.")
                .withField(StandardField.VOLUME, "46")
                .withField(StandardField.YEAR, "2015");
    }

    @Test
    public void testGetName() {
        assertEquals("Medline/PubMed", fetcher.getName());
    }

    @Test
    public void testSearchByIDWijedasa() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("27670948");
        assertTrue(fetchedEntry.isPresent());

        fetchedEntry.get().clearField(StandardField.ABSTRACT); // Remove abstract due to copyright
        assertEquals(Optional.of(entryWijedasa), fetchedEntry);
    }

    @Test
    public void testSearchByIDEndharti() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("27670445");
        assertTrue(fetchedEntry.isPresent());

        fetchedEntry.get().clearField(StandardField.ABSTRACT); // Remove abstract due to copyright
        assertEquals(Optional.of(entryEndharti), fetchedEntry);
    }

    @Test
    public void testSearchByIDIchikawa() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("26197440");
        assertTrue(fetchedEntry.isPresent());

        fetchedEntry.get().clearField(StandardField.ABSTRACT); // Remove abstract due to copyright
        assertEquals(Optional.of(bibEntryIchikawa), fetchedEntry);
    }

    @Test
    public void testSearchByIDSari() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("26867355");
        assertTrue(fetchedEntry.isPresent());

        fetchedEntry.get().clearField(StandardField.ABSTRACT); // Remove abstract due to copyright
        assertEquals(Optional.of(bibEntrySari), fetchedEntry);
    }

    @Test
    public void testMultipleEntries() throws Exception {
        List<BibEntry> entryList = fetcher.performSearch("java");
        entryList.forEach(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright);
        assertEquals(50, entryList.size());
    }

    @Test
    public void testWithLuceneQueryAuthorDate() throws Exception {
        List<BibEntry> entryList = fetcher.performSearch("author:vigmond AND year:2021");
        entryList.forEach(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright);
        assertEquals(18, entryList.size());
    }

    @Test
    public void testWithLuceneQueryAuthorDateRange() throws Exception {
        List<BibEntry> entryList = fetcher.performSearch("author:vigmond AND year-range:2020-2021");
        entryList.forEach(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright);
        assertEquals(28, entryList.size());
    }

    @Test
    public void testInvalidSearchTerm() throws Exception {
        assertThrows(FetcherClientException.class, () ->fetcher.performSearchById("this.is.a.invalid.search.term.for.the.medline.fetcher"));
    }

    @Test
    public void testEmptyEntryList() throws Exception {
        List<BibEntry> entryList = fetcher.performSearch("java is fantastic and awesome ");
        assertEquals(Collections.emptyList(), entryList);
    }

    @Test
    public void testEmptyInput() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }
}
