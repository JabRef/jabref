package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Nov 9, 2007
 * Time: 7:04:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class DuplicateCheckTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testDuplicateDetection() {
        BibEntry one = new BibEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        BibEntry two = new BibEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "James Joyce");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryTypes.BOOK);
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setType(BibtexEntryTypes.ARTICLE);
        one.setField("year", "2005");
        two.setField("year", "2005");
        one.setField("title", "A title");
        two.setField("title", "A title");
        one.setField("journal", "A");
        two.setField("journal", "A");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));
        Assert.assertEquals(1.01, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);

        two.setField("journal", "B");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));
        Assert.assertEquals(0.75, DuplicateCheck.compareEntriesStrictly(one, two), 0.01);

        two.setField("journal", "A");
        one.setField("number", "1");
        two.setField("volume", "21");
        one.setField("pages", "334--337");
        two.setField("pages", "334--337");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("number", "1");
        one.setField("volume", "21");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("volume", "22");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("journal", "B");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        one.setField("journal", "");
        two.setField("journal", "");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("title", "Another title");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));
    }

    @Test
    public void testWordCorrelation() {
        String d1 = "Characterization of Calanus finmarchicus habitat in the North Sea";
        String d2 = "Characterization of Calunus finmarchicus habitat in the North Sea";
        String d3 = "Characterization of Calanus glacialissss habitat in the South Sea";

        assertEquals(1.0, (DuplicateCheck.correlateByWords(d1, d2)), 0.01);
        assertEquals(0.78, (DuplicateCheck.correlateByWords(d1, d3)), 0.01);
        assertEquals(0.78, (DuplicateCheck.correlateByWords(d2, d3)), 0.01);
    }

}
