package net.sf.jabref.model;

import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DuplicateCheckTest {
    @Test
    public void noDuplicateForDifferentTypes() {
        BibEntry e1 = new BibEntry("1", "article");
        BibEntry e2 = new BibEntry("2", "journal");
        assertFalse(DuplicateCheck.isDuplicate(e1, e2, BibDatabaseMode.BIBTEX));
        assertFalse(DuplicateCheck.isDuplicate(e1, e2, BibDatabaseMode.BIBLATEX));
    }

    @Test
    public void noStrictDuplicateForDifferentTypes() {
        BibEntry e1 = new BibEntry("1", "article");
        BibEntry e2 = new BibEntry("2", "journal");
        assertEquals(0, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void strictDuplicateForEqualFields() {
        BibEntry e1 = new BibEntry();
        e1.setField("key1", "value1");
        e1.setField("key2", "value2");
        BibEntry e2 = new BibEntry();
        e2.setField("key1", "value1");
        e2.setField("key2", "value2");
        assertEquals(1, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void noStrictDuplicateForDifferentKeys() {
        BibEntry e1 = new BibEntry();
        e1.setField("key", "value1");
        BibEntry e2 = new BibEntry();
        e2.setField("key1", "value1");;
        assertEquals(0, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void noStrictDuplicateForDifferentValues() {
        BibEntry e1 = new BibEntry();
        e1.setField("key1", "value");
        BibEntry e2 = new BibEntry();
        e2.setField("key1", "value1");
        assertEquals(0, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void noStrictDuplicateIsCaseInsensitiveForKey() {
        BibEntry e1 = new BibEntry();
        e1.setField("KEY1", "value");
        BibEntry e2 = new BibEntry();
        e2.setField("key1", "value");
        assertEquals(1, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void noStrictDuplicateIsCaseSensitiveForValue() {
        BibEntry e1 = new BibEntry();
        e1.setField("key1", "Value");
        BibEntry e2 = new BibEntry();
        e2.setField("key1", "value");
        assertEquals(0, DuplicateCheck.compareEntriesStrictly(e1, e2), 0.01);
    }

    @Test
    public void testDuplicateDetection() {
        BibEntry one = new BibEntry(BibtexEntryTypes.ARTICLE.getName());

        BibEntry two = new BibEntry(BibtexEntryTypes.ARTICLE.getName());

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("author", "James Joyce");
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryTypes.BOOK);
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setType(BibtexEntryTypes.ARTICLE);
        one.setField("year", "2005");
        two.setField("year", "2005");
        one.setField("title", "A title");
        two.setField("title", "A title");
        one.setField("journal", "A");
        two.setField("journal", "A");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("journal", "B");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("journal", "A");
        one.setField("number", "1");
        two.setField("volume", "21");
        one.setField("pages", "334--337");
        two.setField("pages", "334--337");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("number", "1");
        one.setField("volume", "21");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("volume", "22");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("journal", "B");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        one.setField("journal", "");
        two.setField("journal", "");
        assertTrue(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));

        two.setField("title", "Another title");
        assertFalse(DuplicateCheck.isDuplicate(one, two, BibDatabaseMode.BIBTEX));
    }

    @Test
    public void wordCorrelationIsOneForEmptyStrings() {
        assertEquals(1.0, DuplicateCheck.correlateByWords("", ""), 0.01);
    }

    @Test
    public void wordCorrelationForSmallerFirstString() {
        String d1 = "a test";
        String d2 = "this a test";

        assertEquals(0.0, DuplicateCheck.correlateByWords(d1, d2), 0.01);
    }

    @Test
    public void wordCorrelationForBiggerFirstString() {
        String d1 = "Characterization of me";
        String d2 = "Characterization";

        assertEquals(1.0, DuplicateCheck.correlateByWords(d1, d2), 0.01);
    }

    @Test
    public void wordCorrelationForEqualStrings() {
        String d1 = "Characterization";
        String d2 = "Characterization";

        assertEquals(1.0, DuplicateCheck.correlateByWords(d1, d2), 0.01);
    }
}
