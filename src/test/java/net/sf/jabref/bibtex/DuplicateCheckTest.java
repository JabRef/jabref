package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    @Ignore
    public void testDuplicateDetection() {
        BibtexEntry one = new BibtexEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        BibtexEntry two = new BibtexEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        //TODO algorithm thinks bob and joyce is the same with high accuracy
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
        one.setField("number", "1");
        two.setField("number", "1");
        one.setField("volume", "21");
        two.setField("volume", "21");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("volume", "22");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("title", "Another title");
        two.setField("journal", "B");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));
    }

    @Test
    public void testWordCorrelation() {
        String d1 = "Characterization of Calanus finmarchicus habitat in the North Sea";
        String d2 = "Characterization of Calunus finmarchicus habitat in the North Sea";
        String d3 = "Characterization of Calanus glacialissss habitat in the South Sea";

        assertEquals(1.0, (DuplicateCheck.correlateByWords(d1, d2, false)), 0.01);
        assertEquals(0.88, (DuplicateCheck.correlateByWords(d1, d3, false)), 0.01);
        assertEquals(0.88, (DuplicateCheck.correlateByWords(d2, d3, false)), 0.01);
    }

}
