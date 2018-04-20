package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class MedlineFetcherTest {

    private MedlineFetcher fetcher;
    private BibEntry entryWijedasa, entryEndharti, bibEntryIchikawa, bibEntrySari;

    @BeforeEach
    public void setUp() {
        fetcher = new MedlineFetcher();

        entryWijedasa = new BibEntry();
        entryWijedasa.setType(BiblatexEntryTypes.ARTICLE);
        entryWijedasa.setField("author", "Wijedasa, Lahiru S and Jauhiainen, Jyrki and Könönen, Mari and Lampela, Maija and Vasander, Harri and Leblanc, Marie-Claire and Evers, Stephanie and Smith, Thomas E L and Yule, Catherine M and Varkkey, Helena and Lupascu, Massimo and Parish, Faizal and Singleton, Ian and Clements, Gopalasamy R and Aziz, Sheema Abdul and Harrison, Mark E and Cheyne, Susan and Anshari, Gusti Z and Meijaard, Erik and Goldstein, Jenny E and Waldron, Susan and Hergoualc'h, Kristell and Dommain, Rene and Frolking, Steve and Evans, Christopher D and Posa, Mary Rose C and Glaser, Paul H and Suryadiputra, Nyoman and Lubis, Reza and Santika, Truly and Padfield, Rory and Kurnianto, Sofyan and Hadisiswoyo, Panut and Lim, Teck Wyn and Page, Susan E and Gauci, Vincent and Van Der Meer, Peter J and Buckland, Helen and Garnier, Fabien and Samuel, Marshall K and Choo, Liza Nuriati Lim Kim and O'Reilly, Patrick and Warren, Matthew and Suksuwan, Surin and Sumarga, Elham and Jain, Anuj and Laurance, William F and Couwenberg, John and Joosten, Hans and Vernimmen, Ronald and Hooijer, Aljosja and Malins, Chris and Cochrane, Mark A and Perumal, Balu and Siegert, Florian and Peh, Kelvin S-H and Comeau, Louis-Pierre and Verchot, Louis and Harvey, Charles F and Cobb, Alex and Jaafar, Zeehan and Wösten, Henk and Manuri, Solichin and Müller, Moritz and Giesen, Wim and Phelps, Jacob and Yong, Ding Li and Silvius, Marcel and Wedeux, Béatrice M M and Hoyt, Alison and Osaki, Mitsuru and Hirano, Takashi and Takahashi, Hidenori and Kohyama, Takashi S and Haraguchi, Akira and Nugroho, Nunung P and Coomes, David A and Quoi, Le Phat and Dohong, Alue and Gunawan, Haris and Gaveau, David L A and Langner, Andreas and Lim, Felix K S and Edwards, David P and Giam, Xingli and Van Der Werf, Guido and Carmenta, Rachel and Verwer, Caspar C and Gibson, Luke and Gandois, Laure and Graham, Laura Linda Bozena and Regalino, Jhanson and Wich, Serge A and Rieley, Jack and Kettridge, Nicholas and Brown, Chloe and Pirard, Romain and Moore, Sam and Capilla, B Ripoll and Ballhorn, Uwe and Ho, Hua Chew and Hoscilo, Agata and Lohberger, Sandra and Evans, Theodore A and Yulianti, Nina and Blackham, Grace and Onrizal and Husson, Simon and Murdiyarso, Daniel and Pangala, Sunita and Cole, Lydia E S and Tacconi, Luca and Segah, Hendrik and Tonoto, Prayoto and Lee, Janice S H and Schmilewski, Gerald and Wulffraat, Stephan and Putra, Erianto Indra and Cattau, Megan E and Clymo, R S and Morrison, Ross and Mujahid, Aazani and Miettinen, Jukka and Liew, Soo Chin and Valpola, Samu and Wilson, David and D'Arcy, Laura and Gerding, Michiel and Sundari, Siti and Thornton, Sara A and Kalisz, Barbara and Chapman, Stephen J and Su, Ahmad Suhaizi Mat and Basuki, Imam and Itoh, Masayuki and Traeholt, Carl and Sloan, Sean and Sayok, Alexander K and Andersen, Roxane");
        entryWijedasa.setField("country", "England");
        entryWijedasa.setField("doi", "10.1111/gcb.13516");
        entryWijedasa.setField("issn", "1365-2486");
        entryWijedasa.setField("issn-linking", "1354-1013");
        entryWijedasa.setField("issue", "3");
        entryWijedasa.setField("journal", "Global change biology");
        entryWijedasa.setField("month", "#mar#");
        entryWijedasa.setField("nlm-id", "9888746");
        entryWijedasa.setField("owner", "NLM");
        entryWijedasa.setField("pages", "977--982");
        entryWijedasa.setField("pmid", "27670948");
        entryWijedasa.setField("pubmodel", "Print-Electronic");
        entryWijedasa.setField("pubstatus", "ppublish");
        entryWijedasa.setField("revised", "2018-01-23");
        entryWijedasa.setField("title", "Denial of long-term issues with agriculture on tropical peatlands will have devastating consequences.");
        entryWijedasa.setField("volume", "23");
        entryWijedasa.setField("year", "2017");

        entryEndharti = new BibEntry();
        entryEndharti.setType(BiblatexEntryTypes.ARTICLE);
        entryEndharti.setField("title", "Dendrophthoe pentandra (L.) Miq extract effectively inhibits inflammation, proliferation and induces p53 expression on colitis-associated colon cancer.");
        entryEndharti.setField("author", "Endharti, Agustina Tri and Wulandari, Adisti and Listyana, Anik and Norahmawati, Eviana and Permana, Sofy");
        entryEndharti.setField("country", "England");
        entryEndharti.setField("doi", "10.1186/s12906-016-1345-0");
        entryEndharti.setField("pii", "10.1186/s12906-016-1345-0");
        entryEndharti.setField("pmc", "PMC5037598");
        entryEndharti.setField("issn", "1472-6882");
        entryEndharti.setField("issn-linking", "1472-6882");
        entryEndharti.setField("issue", "1");
        entryEndharti.setField("journal", "BMC complementary and alternative medicine");
        entryEndharti.setField("keywords", "CAC; Dendrophtoe pentandra; IL-22; MPO; Proliferation; p53");
        entryEndharti.setField("nlm-id", "101088661");
        entryEndharti.setField("owner", "NLM");
        entryEndharti.setField("pages", "374");
        entryEndharti.setField("month", "#sep#");
        entryEndharti.setField("pmid", "27670445");
        entryEndharti.setField("pubmodel", "Electronic");
        entryEndharti.setField("pubstatus", "epublish");
        entryEndharti.setField("revised", "2017-02-20");
        entryEndharti.setField("volume", "16");
        entryEndharti.setField("year", "2016");

        bibEntryIchikawa = new BibEntry();
        bibEntryIchikawa.setType(BiblatexEntryTypes.ARTICLE);
        bibEntryIchikawa.setField("author", "Ichikawa-Seki, Madoka and Guswanto, Azirwan and Allamanda, Puttik and Mariamah, Euis Siti and Wibowo, Putut Eko and Igarashi, Ikuo and Nishikawa, Yoshifumi");
        bibEntryIchikawa.setField("chemicals", "Antibodies, Protozoan, Antigens, Protozoan, GRA7 protein, Toxoplasma gondii, Protozoan Proteins");
        bibEntryIchikawa.setField("citation-subset", "IM");
        bibEntryIchikawa.setField("completed", "2016-07-26");
        bibEntryIchikawa.setField("country", "Netherlands");
        bibEntryIchikawa.setField("doi", "10.1016/j.parint.2015.07.004");
        bibEntryIchikawa.setField("issn", "1873-0329");
        bibEntryIchikawa.setField("pubstatus", "ppublish");
        bibEntryIchikawa.setField("revised", "2015-09-26");
        bibEntryIchikawa.setField("issn-linking", "1383-5769");
        bibEntryIchikawa.setField("issue", "6");
        bibEntryIchikawa.setField("journal", "Parasitology international");
        bibEntryIchikawa.setField("keywords", "Animals; Antibodies, Protozoan, blood; Antigens, Protozoan, immunology; Cattle, parasitology; Cattle Diseases, epidemiology, parasitology; Enzyme-Linked Immunosorbent Assay, veterinary; Geography; Humans; Indonesia, epidemiology; Livestock, immunology, parasitology; Meat, parasitology; Protozoan Proteins, immunology; Seroepidemiologic Studies; Swine, parasitology; Swine Diseases, epidemiology, parasitology; Toxoplasma, immunology; Toxoplasmosis, Animal, epidemiology, immunology, parasitology; Cattle; ELISA; Indonesia; Pig; TgGRA7; Toxoplasma gondii");
        bibEntryIchikawa.setField("month", "#dec#");
        bibEntryIchikawa.setField("nlm-id", "9708549");
        bibEntryIchikawa.setField("owner", "NLM");
        bibEntryIchikawa.setField("pages", "484--486");
        bibEntryIchikawa.setField("pii", "S1383-5769(15)00124-5");
        bibEntryIchikawa.setField("pmid", "26197440");
        bibEntryIchikawa.setField("pubmodel", "Print-Electronic");
        bibEntryIchikawa.setField("title", "Seroprevalence of antibody to TgGRA7 antigen of Toxoplasma gondii in livestock animals from Western Java, Indonesia.");
        bibEntryIchikawa.setField("volume", "64");
        bibEntryIchikawa.setField("year", "2015");

        bibEntrySari = new BibEntry();
        bibEntrySari.setType(BiblatexEntryTypes.ARTICLE);
        bibEntrySari.setField("author", "Sari, Yulia and Haryati, Sri and Raharjo, Irvan and Prasetyo, Afiono Agung");
        bibEntrySari.setField("chemicals", "Antibodies, Protozoan, Antibodies, Viral, HTLV-I Antibodies, HTLV-II Antibodies, Hepatitis Antibodies, Hepatitis B Antibodies, Hepatitis C Antibodies, Immunoglobulin G, Immunoglobulin M");
        bibEntrySari.setField("citation-subset", "IM");
        bibEntrySari.setField("completed", "2016-04-21");
        bibEntrySari.setField("country", "Thailand");
        bibEntrySari.setField("issn", "0125-1562");
        bibEntrySari.setField("issn-linking", "0125-1562");
        bibEntrySari.setField("issue", "6");
        bibEntrySari.setField("journal", "The Southeast Asian journal of tropical medicine and public health");
        bibEntrySari.setField("keywords", "Antibodies, Protozoan; Antibodies, Viral, immunology; Coinfection, epidemiology, immunology; Female; HIV Infections, epidemiology; HTLV-I Antibodies, immunology; HTLV-I Infections, epidemiology, immunology; HTLV-II Antibodies, immunology; HTLV-II Infections, epidemiology, immunology; Hepatitis Antibodies, immunology; Hepatitis B Antibodies, immunology; Hepatitis C Antibodies, immunology; Hepatitis Delta Virus, immunology; Hepatitis, Viral, Human, epidemiology, immunology; Humans; Immunoglobulin G, immunology; Immunoglobulin M, immunology; Indonesia, epidemiology; Male; Prisoners; Seroepidemiologic Studies; Toxoplasma, immunology; Toxoplasmosis, epidemiology, immunology");
        bibEntrySari.setField("month", "#nov#");
        bibEntrySari.setField("pubstatus", "ppublish");
        bibEntrySari.setField("revised", "2016-02-12");
        bibEntrySari.setField("nlm-id", "0266303");
        bibEntrySari.setField("owner", "NLM");
        bibEntrySari.setField("pages", "977--985");
        bibEntrySari.setField("pmid", "26867355");
        bibEntrySari.setField("pubmodel", "Print");
        bibEntrySari.setField("title", "TOXOPLASMA AND VIRAL ANTIBODIES AMONG HIV PATIENTS AND INMATES IN CENTRAL JAVA, INDONESIA.");
        bibEntrySari.setField("volume", "46");
        bibEntrySari.setField("year", "2015");
    }

    @Test
    public void testGetName() {
        assertEquals("Medline/PubMed", fetcher.getName());
    }

    @Test
    public void testGetHelpPage() {
        assertEquals("Medline", fetcher.getHelpPage().getPageName());
    }

    @Test
    public void testSearchByIDWijedasa() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("27670948");
        fetchedEntry.get().clearField(FieldName.ABSTRACT); //Remove abstract due to copyright
        assertEquals(Optional.of(entryWijedasa), fetchedEntry);
    }

    @Test
    public void testSearchByIDEndharti() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("27670445");
        fetchedEntry.get().clearField(FieldName.ABSTRACT); //Remove abstract due to copyright
        assertEquals(Optional.of(entryEndharti), fetchedEntry);
    }

    @Test
    public void testSearchByIDIchikawa() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("26197440");
        fetchedEntry.get().clearField(FieldName.ABSTRACT); //Remove abstract due to copyright
        assertEquals(Optional.of(bibEntryIchikawa), fetchedEntry);
    }

    @Test
    public void testSearchByIDSari() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("26867355");
        fetchedEntry.get().clearField(FieldName.ABSTRACT); //Remove abstract due to copyright
        assertEquals(Optional.of(bibEntrySari), fetchedEntry);
    }

    @Test
    public void testMultipleEntries() throws Exception {
        List<BibEntry> entryList = fetcher.performSearch("java");
        entryList.forEach(entry -> entry.clearField(FieldName.ABSTRACT)); //Remove abstract due to copyright);
        assertEquals(50, entryList.size());
        assertTrue(entryList.contains(bibEntryIchikawa));
        assertTrue(entryList.contains(bibEntrySari));
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
