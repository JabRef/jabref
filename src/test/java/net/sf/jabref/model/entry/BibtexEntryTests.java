package net.sf.jabref.model.entry;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.importer.fileformat.BibtexParser;

import java.util.ArrayList;
import java.util.List;

public class BibtexEntryTests {
    @Before
    public void setup() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testDefaultConstructor() {
        BibtexEntry entry = new BibtexEntry();
        // we have to use `getType("misc")` in the case of biblatex mode
        Assert.assertEquals(EntryTypes.getType("misc"), entry.getType());
        Assert.assertNotNull(entry.getId());
        Assert.assertNull(entry.getField("author"));
    }

    @Test
    public void testGetPublicationDate() {
        Assert.assertEquals("2003-02",
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = #FEB# }"))
                        .getPublicationDate());

        Assert.assertEquals("2003-03",
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = 3 }")).getPublicationDate());

        Assert.assertEquals("2003",
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}}")).getPublicationDate());

        Assert.assertEquals(null,
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, month = 3 }")).getPublicationDate());

        Assert.assertEquals(null,
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, author={bla}}")).getPublicationDate());

        Assert.assertEquals("2003-12",
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {03}, month = #DEC# }"))
                        .getPublicationDate());
    }

    @Test
    public void allFieldsPresentDefault() {
        BibtexEntry e = new BibtexEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        List<String> requiredFields = new ArrayList<>();

        requiredFields.add("author");
        requiredFields.add("title");
        Assert.assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add("year");
        Assert.assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void allFieldsPresentOr() {
        BibtexEntry e = new BibtexEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");
        List<String> requiredFields = new ArrayList<>();

        // XOR required
        requiredFields.add("journal/year");
        Assert.assertTrue(e.allFieldsPresent(requiredFields, null));

        requiredFields.add("year/address");
        Assert.assertFalse(e.allFieldsPresent(requiredFields, null));
    }

    @Test
    public void hasAllRequiredFields() {
        BibtexEntry e = new BibtexEntry("id", BibtexEntryTypes.ARTICLE);
        e.setField("author", "abc");
        e.setField("title", "abc");
        e.setField("journal", "abc");

        Assert.assertFalse(e.hasAllRequiredFields(null));

        e.setField("year", "2015");
        Assert.assertTrue(e.hasAllRequiredFields(null));
    }

    @Test
    public void isNullOrEmptyCiteKey() {
        BibtexEntry e = new BibtexEntry("id", BibtexEntryTypes.ARTICLE);
        Assert.assertFalse(e.hasCiteKey());
        e.setField(BibtexEntry.KEY_FIELD, "");
        Assert.assertFalse(e.hasCiteKey());
        e.setField(BibtexEntry.KEY_FIELD, null);
        Assert.assertFalse(e.hasCiteKey());
        e.setField(BibtexEntry.KEY_FIELD, "key");
        Assert.assertTrue(e.hasCiteKey());
        e.clearField(BibtexEntry.KEY_FIELD);
        Assert.assertFalse(e.hasCiteKey());
    }

}
