package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        entryWijedasa = new BibEntry();
        entryWijedasa.setType(StandardEntryType.Article);
        entryWijedasa.setField(StandardField.AUTHOR, "Wijedasa, Lahiru S. and Jauhiainen, Jyrki and Könönen, Mari and Lampela, Maija and Vasander, Harri and Leblanc, Marie-Claire and Evers, Stephanie and Smith, Thomas E. L. and Yule, Catherine M. and Varkkey, Helena and Lupascu, Massimo and Parish, Faizal and Singleton, Ian and Clements, Gopalasamy R. and Aziz, Sheema Abdul and Harrison, Mark E. and Cheyne, Susan and Anshari, Gusti Z. and Meijaard, Erik and Goldstein, Jenny E. and Waldron, Susan and Hergoualc'h, Kristell and Dommain, Rene and Frolking, Steve and Evans, Christopher D. and Posa, Mary Rose C. and Glaser, Paul H. and Suryadiputra, Nyoman and Lubis, Reza and Santika, Truly and Padfield, Rory and Kurnianto, Sofyan and Hadisiswoyo, Panut and Lim, Teck Wyn and Page, Susan E. and Gauci, Vincent and Van Der Meer, Peter J. and Buckland, Helen and Garnier, Fabien and Samuel, Marshall K. and Choo, Liza Nuriati Lim Kim and O'Reilly, Patrick and Warren, Matthew and Suksuwan, Surin and Sumarga, Elham and Jain, Anuj and Laurance, William F. and Couwenberg, John and Joosten, Hans and Vernimmen, Ronald and Hooijer, Aljosja and Malins, Chris and Cochrane, Mark A. and Perumal, Balu and Siegert, Florian and Peh, Kelvin S.-H. and Comeau, Louis-Pierre and Verchot, Louis and Harvey, Charles F. and Cobb, Alex and Jaafar, Zeehan and Wösten, Henk and Manuri, Solichin and Müller, Moritz and Giesen, Wim and Phelps, Jacob and Yong, Ding Li and Silvius, Marcel and Wedeux, Béatrice M. M. and Hoyt, Alison and Osaki, Mitsuru and Hirano, Takashi and Takahashi, Hidenori and Kohyama, Takashi S. and Haraguchi, Akira and Nugroho, Nunung P. and Coomes, David A. and Quoi, Le Phat and Dohong, Alue and Gunawan, Haris and Gaveau, David L. A. and Langner, Andreas and Lim, Felix K. S. and Edwards, David P. and Giam, Xingli and Van Der Werf, Guido and Carmenta, Rachel and Verwer, Caspar C. and Gibson, Luke and Gandois, Laure and Graham, Laura Linda Bozena and Regalino, Jhanson and Wich, Serge A. and Rieley, Jack and Kettridge, Nicholas and Brown, Chloe and Pirard, Romain and Moore, Sam and Capilla, B. Ripoll and Ballhorn, Uwe and Ho, Hua Chew and Hoscilo, Agata and Lohberger, Sandra and Evans, Theodore A. and Yulianti, Nina and Blackham, Grace and Onrizal and Husson, Simon and Murdiyarso, Daniel and Pangala, Sunita and Cole, Lydia E. S. and Tacconi, Luca and Segah, Hendrik and Tonoto, Prayoto and Lee, Janice S. H. and Schmilewski, Gerald and Wulffraat, Stephan and Putra, Erianto Indra and Cattau, Megan E. and Clymo, R. S. and Morrison, Ross and Mujahid, Aazani and Miettinen, Jukka and Liew, Soo Chin and Valpola, Samu and Wilson, David and D'Arcy, Laura and Gerding, Michiel and Sundari, Siti and Thornton, Sara A. and Kalisz, Barbara and Chapman, Stephen J. and Su, Ahmad Suhaizi Mat and Basuki, Imam and Itoh, Masayuki and Traeholt, Carl and Sloan, Sean and Sayok, Alexander K. and Andersen, Roxane");
        entryWijedasa.setField(new UnknownField("country"), "England");
        entryWijedasa.setField(StandardField.DOI, "10.1111/gcb.13516");
        entryWijedasa.setField(StandardField.ISSN, "1365-2486");
        entryWijedasa.setField(new UnknownField("issn-linking"), "1354-1013");
        entryWijedasa.setField(StandardField.ISSUE, "3");
        entryWijedasa.setField(StandardField.JOURNAL, "Global change biology");
        entryWijedasa.setField(StandardField.MONTH, "#mar#");
        entryWijedasa.setField(new UnknownField("nlm-id"), "9888746");
        entryWijedasa.setField(StandardField.OWNER, "NLM");
        entryWijedasa.setField(StandardField.PAGES, "977--982");
        entryWijedasa.setField(StandardField.PMID, "27670948");
        entryWijedasa.setField(new UnknownField("pubmodel"), "Print-Electronic");
        entryWijedasa.setField(StandardField.PUBSTATE, "ppublish");
        entryWijedasa.setField(new UnknownField("revised"), "2019-11-20");
        entryWijedasa.setField(StandardField.TITLE, "Denial of long-term issues with agriculture on tropical peatlands will have devastating consequences.");
        entryWijedasa.setField(StandardField.VOLUME, "23");
        entryWijedasa.setField(StandardField.YEAR, "2017");

        entryEndharti = new BibEntry();
        entryEndharti.setType(StandardEntryType.Article);
        entryEndharti.setField(StandardField.TITLE, "Dendrophthoe pentandra (L.) Miq extract effectively inhibits inflammation, proliferation and induces p53 expression on colitis-associated colon cancer.");
        entryEndharti.setField(StandardField.AUTHOR, "Endharti, Agustina Tri and Wulandari, Adisti and Listyana, Anik and Norahmawati, Eviana and Permana, Sofy");
        entryEndharti.setField(new UnknownField("country"), "England");
        entryEndharti.setField(StandardField.DOI, "10.1186/s12906-016-1345-0");
        entryEndharti.setField(new UnknownField("pii"), "10.1186/s12906-016-1345-0");
        entryEndharti.setField(new UnknownField("pmc"), "PMC5037598");
        entryEndharti.setField(StandardField.ISSN, "1472-6882");
        entryEndharti.setField(new UnknownField("issn-linking"), "1472-6882");
        entryEndharti.setField(StandardField.ISSUE, "1");
        entryEndharti.setField(StandardField.JOURNAL, "BMC complementary and alternative medicine");
        entryEndharti.setField(StandardField.KEYWORDS, "CAC; Dendrophtoe pentandra; IL-22; MPO; Proliferation; p53");
        entryEndharti.setField(new UnknownField("nlm-id"), "101088661");
        entryEndharti.setField(StandardField.OWNER, "NLM");
        entryEndharti.setField(StandardField.PAGES, "374");
        entryEndharti.setField(StandardField.MONTH, "#sep#");
        entryEndharti.setField(StandardField.PMID, "27670445");
        entryEndharti.setField(new UnknownField("pubmodel"), "Electronic");
        entryEndharti.setField(StandardField.PUBSTATE, "epublish");
        entryEndharti.setField(new UnknownField("revised"), "2019-11-20");
        entryEndharti.setField(StandardField.VOLUME, "16");
        entryEndharti.setField(StandardField.YEAR, "2016");

        bibEntryIchikawa = new BibEntry();
        bibEntryIchikawa.setType(StandardEntryType.Article);
        bibEntryIchikawa.setField(StandardField.AUTHOR, "Ichikawa-Seki, Madoka and Guswanto, Azirwan and Allamanda, Puttik and Mariamah, Euis Siti and Wibowo, Putut Eko and Igarashi, Ikuo and Nishikawa, Yoshifumi");
        bibEntryIchikawa.setField(new UnknownField("chemicals"), "Antibodies, Protozoan, Antigens, Protozoan, GRA7 protein, Toxoplasma gondii, Protozoan Proteins");
        bibEntryIchikawa.setField(new UnknownField("citation-subset"), "IM");
        bibEntryIchikawa.setField(new UnknownField("completed"), "2016-07-26");
        bibEntryIchikawa.setField(new UnknownField("country"), "Netherlands");
        bibEntryIchikawa.setField(StandardField.DOI, "10.1016/j.parint.2015.07.004");
        bibEntryIchikawa.setField(StandardField.ISSN, "1873-0329");
        bibEntryIchikawa.setField(StandardField.PUBSTATE, "ppublish");
        bibEntryIchikawa.setField(new UnknownField("revised"), "2015-09-26");
        bibEntryIchikawa.setField(new UnknownField("issn-linking"), "1383-5769");
        bibEntryIchikawa.setField(StandardField.ISSUE, "6");
        bibEntryIchikawa.setField(StandardField.JOURNAL, "Parasitology international");
        bibEntryIchikawa.setField(StandardField.KEYWORDS, "Animals; Antibodies, Protozoan, blood; Antigens, Protozoan, immunology; Cattle, parasitology; Cattle Diseases, epidemiology, parasitology; Enzyme-Linked Immunosorbent Assay, veterinary; Geography; Humans; Indonesia, epidemiology; Livestock, immunology, parasitology; Meat, parasitology; Protozoan Proteins, immunology; Seroepidemiologic Studies; Swine, parasitology; Swine Diseases, epidemiology, parasitology; Toxoplasma, immunology; Toxoplasmosis, Animal, epidemiology, immunology, parasitology; Cattle; ELISA; Indonesia; Pig; TgGRA7; Toxoplasma gondii");
        bibEntryIchikawa.setField(StandardField.MONTH, "#dec#");
        bibEntryIchikawa.setField(new UnknownField("nlm-id"), "9708549");
        bibEntryIchikawa.setField(StandardField.OWNER, "NLM");
        bibEntryIchikawa.setField(StandardField.PAGES, "484--486");
        bibEntryIchikawa.setField(new UnknownField("pii"), "S1383-5769(15)00124-5");
        bibEntryIchikawa.setField(StandardField.PMID, "26197440");
        bibEntryIchikawa.setField(new UnknownField("pubmodel"), "Print-Electronic");
        bibEntryIchikawa.setField(StandardField.TITLE, "Seroprevalence of antibody to TgGRA7 antigen of Toxoplasma gondii in livestock animals from Western Java, Indonesia.");
        bibEntryIchikawa.setField(StandardField.VOLUME, "64");
        bibEntryIchikawa.setField(StandardField.YEAR, "2015");

        bibEntrySari = new BibEntry();
        bibEntrySari.setType(StandardEntryType.Article);
        bibEntrySari.setField(StandardField.AUTHOR, "Sari, Yulia and Haryati, Sri and Raharjo, Irvan and Prasetyo, Afiono Agung");
        bibEntrySari.setField(new UnknownField("chemicals"), "Antibodies, Protozoan, Antibodies, Viral, HTLV-I Antibodies, HTLV-II Antibodies, Hepatitis Antibodies, Hepatitis B Antibodies, Hepatitis C Antibodies, Immunoglobulin G, Immunoglobulin M");
        bibEntrySari.setField(new UnknownField("citation-subset"), "IM");
        bibEntrySari.setField(new UnknownField("completed"), "2016-04-21");
        bibEntrySari.setField(new UnknownField("country"), "Thailand");
        bibEntrySari.setField(StandardField.ISSN, "0125-1562");
        bibEntrySari.setField(new UnknownField("issn-linking"), "0125-1562");
        bibEntrySari.setField(StandardField.ISSUE, "6");
        bibEntrySari.setField(StandardField.JOURNAL, "The Southeast Asian journal of tropical medicine and public health");
        bibEntrySari.setField(StandardField.KEYWORDS, "Antibodies, Protozoan; Antibodies, Viral, immunology; Coinfection, epidemiology, immunology; Female; HIV Infections, epidemiology; HTLV-I Antibodies, immunology; HTLV-I Infections, epidemiology, immunology; HTLV-II Antibodies, immunology; HTLV-II Infections, epidemiology, immunology; Hepatitis Antibodies, immunology; Hepatitis B Antibodies, immunology; Hepatitis C Antibodies, immunology; Hepatitis Delta Virus, immunology; Hepatitis, Viral, Human, epidemiology, immunology; Humans; Immunoglobulin G, immunology; Immunoglobulin M, immunology; Indonesia, epidemiology; Male; Prisoners; Seroepidemiologic Studies; Toxoplasma, immunology; Toxoplasmosis, epidemiology, immunology");
        bibEntrySari.setField(StandardField.MONTH, "#nov#");
        bibEntrySari.setField(StandardField.PUBSTATE, "ppublish");
        bibEntrySari.setField(new UnknownField("revised"), "2018-12-02");
        bibEntrySari.setField(new UnknownField("nlm-id"), "0266303");
        bibEntrySari.setField(StandardField.OWNER, "NLM");
        bibEntrySari.setField(StandardField.PAGES, "977--985");
        bibEntrySari.setField(StandardField.PMID, "26867355");
        bibEntrySari.setField(new UnknownField("pubmodel"), "Print");
        bibEntrySari.setField(StandardField.TITLE, "TOXOPLASMA AND VIRAL ANTIBODIES AMONG HIV PATIENTS AND INMATES IN CENTRAL JAVA, INDONESIA.");
        bibEntrySari.setField(StandardField.VOLUME, "46");
        bibEntrySari.setField(StandardField.YEAR, "2015");
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
        System.out.println(entryList);
        assertEquals(50, entryList.size());
    }

    @Test
    public void testInvalidSearchTerm() throws Exception {
        assertEquals(Optional.empty(), fetcher.performSearchById("this.is.a.invalid.search.term.for.the.medline.fetcher"));
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
