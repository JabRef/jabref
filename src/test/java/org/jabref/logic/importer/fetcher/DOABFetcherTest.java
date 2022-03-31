package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class DOABFetcherTest {
    private DOABFetcher fetcher;
    private BibEntry David_Opal;
    private BibEntry Ronald_Snijder;
    private BibEntry Andrew_Perrin;

    @BeforeEach
    public void setUp() throws Exception {
        fetcher = new DOABFetcher();

        David_Opal = new BibEntry();
        David_Opal.setField(StandardField.AUTHOR, "David Pol");
        David_Opal.setField(StandardField.TITLE, "I Open Fire");
        David_Opal.setField(StandardField.TYPE, "book");
        David_Opal.setField(StandardField.DOI, "10.21983/P3.0086.1.00");
        David_Opal.setField(StandardField.PAGES, "56");
        David_Opal.setField(StandardField.YEAR, "2014");
        David_Opal.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/34739");
        David_Opal.setField(StandardField.ABSTRACT, "David Pol presents an ontology of war in the form of " +
                "the lyric poem. “Do you hear what I’m shooting at you?” In I Open Fire, all relation is " +
                "warfare. Minefields compromise movement. Intention aims. Touch burns. Sex explodes bodies. " +
                "Time ticks in bomb countdowns. Sound is sirens. Plenitude is debris. All of it under " +
                "surveillance. “My world is critically injured. It was ambushed.” The poems in this book perform" +
                " the reductions and repetitions endemic to war itself, each one returning the reader to the same," +
                " unthinkable place in which the range of human experience has been so flattened that, despite all" +
                " the explosive action, “Almost nothing is happening.” Against this backdrop, we continue to fall" +
                " in love. But Pol’s poems remind us that this is no reason for optimism. Does love offer a" +
                " delusional escape from war, or are relationships the very definition of combat? These poems take" +
                " up the themes of love, sex, marriage, touch, hope — in short, the many dimensions of" +
                " interpersonal connection — in a world in unprecedentedly critical condition. “And when the night" +
                " goes off the shock wave throws us apart toward each other.”");
        David_Opal.setField(StandardField.LANGUAGE, "English");
        David_Opal.setField(StandardField.KEYWORDS, "poetry,love,warfare");
        David_Opal.setField(StandardField.PUBLISHER, "punctum books");

        Ronald_Snijder = new BibEntry();
        Ronald_Snijder.setField(StandardField.AUTHOR, "Ronald Snijder");
        Ronald_Snijder.setField(StandardField.TITLE, "The deliverance of open access books");
        Ronald_Snijder.setField(StandardField.TYPE, "book");
        Ronald_Snijder.setField(StandardField.DOI, "10.26530/OAPEN_1004809");
        Ronald_Snijder.setField(StandardField.PAGES, "234");
        Ronald_Snijder.setField(StandardField.YEAR, "2019");
        Ronald_Snijder.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/26303");
        Ronald_Snijder.setField(StandardField.ABSTRACT, "In many scholarly disciplines, books - not articles" +
                " - are the norm. As print runs become smaller, the question arises whether publishing monographs" +
                " in open access helps to make their contents globally accessible. To answer this question, the" +
                " results of multiple studies on the usage of open access books are presented. The research" +
                " focuses on three areas: economic viability; optimization of open access monographs" +
                " infrastructure and measuring the effects of open access in terms of scholarly impact and" +
                " societal influence. Each chapter reviews a different aspect: book sales, digital dissemination," +
                " open licenses, user communities, measuring usage, developing countries and the effects on" +
                " citations and social media.");
        Ronald_Snijder.setField(StandardField.LANGUAGE, "English");
        Ronald_Snijder.setField(StandardField.KEYWORDS, "Open Access,Monographs,OAPEN Library," +
                "Directory of Open Access Books");
        Ronald_Snijder.setField(StandardField.PUBLISHER, "Amsterdam University Press");

        Andrew_Perrin = new BibEntry();
        Andrew_Perrin.setField(StandardField.EDITOR, "Andrew Perrin and Loren Stuckenbruck");
        Andrew_Perrin.setField(StandardField.TITLE, "Four Kingdom Motifs before and beyond the Book of Daniel");
        Andrew_Perrin.setField(StandardField.TYPE, "book");
        Andrew_Perrin.setField(StandardField.DOI, "10.1163/9789004443280");
        Andrew_Perrin.setField(StandardField.PAGES, "354");
        Andrew_Perrin.setField(StandardField.YEAR, "2020");
        Andrew_Perrin.setField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/68086");
        Andrew_Perrin.setField(StandardField.ABSTRACT, "The four kingdoms motif enabled writers of various " +
                "cultures, times, and places, to periodize history as the staged succession of empires " +
                "barrelling towards an utopian age. The motif provided order to lived experiences under empire" +
                " (the present), in view of ancestral traditions and cultural heritage (the past), and inspired" +
                " outlooks assuring hope, deliverance, and restoration (the future). Four Kingdom Motifs before" +
                " and beyond the Book of Daniel includes thirteen essays that explore the reach and redeployment" +
                " of the motif in classical and ancient Near Eastern writings, Jewish and Christian scriptures," +
                " texts among the Dead Sea Scrolls, Apocrypha and pseudepigrapha, depictions in European" +
                " architecture and cartography, as well as patristic, rabbinic, Islamic, and African writings " +
                "from antiquity through the Mediaeval eras. Readership: Advanced students and scholars of the " +
                "textual formation, apocalyptic theology, and historiographies of the book of Daniel and its " +
                "diverse reception by writers and communities.");
        Andrew_Perrin.setField(StandardField.LANGUAGE, "English");
        Andrew_Perrin.setField(StandardField.KEYWORDS, "Religion");
        Andrew_Perrin.setField(StandardField.PUBLISHER, "Brill");

    }

    @Test
    public void TestGetName() {
        assertEquals("DOAB", fetcher.getName());
    }

    @Test
    public void TestPerformSearch() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("i open fire");
        assertFalse(entries.isEmpty());
        assertTrue(entries.contains(David_Opal));
    }

     @Test
    public void TestPerformSearch2() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("the deliverance of open access books");
        assertFalse(entries.isEmpty());
        assertTrue(entries.contains(Ronald_Snijder));
    }

    @Test
    public void TestPerformSearch3() throws FetcherException {
        List<BibEntry> entries;
        entries = fetcher.performSearch("Four Kingdom Motifs before and beyond the Book of Daniel");
        assertFalse(entries.isEmpty());
        assertTrue(entries.contains(Andrew_Perrin));
    }

}
