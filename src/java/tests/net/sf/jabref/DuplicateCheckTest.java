package tests.net.sf.jabref;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.DuplicateCheck;
import net.sf.jabref.Util;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Nov 9, 2007
 * Time: 7:04:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class DuplicateCheckTest extends TestCase {

    public void testDuplicateDetection() {

        BibtexEntry one = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.ARTICLE);

        BibtexEntry two = new BibtexEntry(Util.createNeutralId(), BibtexEntryType.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "James Joyce");
        assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryType.BOOK);
        assertFalse(DuplicateCheck.isDuplicate(one, two));

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
        assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("volume", "22");
        assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("title", "Another title");
        two.setField("journal", "B");
        assertFalse(DuplicateCheck.isDuplicate(one, two));
    }

}
