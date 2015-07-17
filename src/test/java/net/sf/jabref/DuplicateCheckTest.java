package net.sf.jabref;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        BibtexEntry one = new BibtexEntry(IdGenerator.next(), BibtexEntryType.ARTICLE);

        BibtexEntry two = new BibtexEntry(IdGenerator.next(), BibtexEntryType.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        //TODO algorithm things bob and joyce is the same with high accuracy
        two.setField("author", "James Joyce");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryType.BOOK);
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setType(BibtexEntryType.ARTICLE);
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

}
