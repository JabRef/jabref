package net.sf.jabref.model.entry;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
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
        BibEntry entry = new BibEntry();
        // we have to use `getType("misc")` in the case of biblatex mode
        Assert.assertEquals("misc", entry.getType());
        Assert.assertNotNull(entry.getId());
        Assert.assertNull(entry.getField("author"));
    }

    @Test
    public void allFieldsPresentDefault() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
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
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
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
    public void isNullOrEmptyCiteKey() {
        BibEntry e = new BibEntry("id", BibtexEntryTypes.ARTICLE.getName());
        Assert.assertFalse(e.hasCiteKey());

        e.setField(BibEntry.KEY_FIELD, "");
        Assert.assertFalse(e.hasCiteKey());

        try {
            e.setField(BibEntry.KEY_FIELD, null);
            Assert.fail();
        } catch(NullPointerException asExpected) {

        }

        e.setField(BibEntry.KEY_FIELD, "key");
        Assert.assertTrue(e.hasCiteKey());

        e.clearField(BibEntry.KEY_FIELD);
        Assert.assertFalse(e.hasCiteKey());
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
    public void testKeywordMethods() {
        BibEntry be = BibtexParser.singleFromString("@ARTICLE{Key15, keywords = {Foo, Bar}}");

        String[] expected = {"Foo",  "Bar"};
        Assert.assertArrayEquals(expected, be.getSeparatedKeywords().toArray());

        List<String> kw = be.getSeparatedKeywords();

        be.addKeyword("FooBar");
        String[] expected2 = {"Foo", "Bar", "FooBar"};
        Assert.assertArrayEquals(expected2, be.getSeparatedKeywords().toArray());

        be.addKeyword("FooBar");
        Assert.assertArrayEquals(expected2, be.getSeparatedKeywords().toArray());

        be.addKeyword("FOO");
        Assert.assertArrayEquals(expected2, be.getSeparatedKeywords().toArray());

        be.addKeyword("");
        Assert.assertArrayEquals(expected2, be.getSeparatedKeywords().toArray());

        try {
            be.addKeyword(null);
            Assert.fail();
        } catch(NullPointerException asExpected){

        }

        BibEntry be2 = new BibEntry();
        Assert.assertTrue(be2.getSeparatedKeywords().isEmpty());
        be2.addKeyword("");
        Assert.assertTrue(be2.getSeparatedKeywords().isEmpty());
        be2.addKeywords(be.getSeparatedKeywords());
        Assert.assertArrayEquals(expected2, be2.getSeparatedKeywords().toArray());
        be2.putKeywords(kw);
        Assert.assertArrayEquals(expected, be2.getSeparatedKeywords().toArray());
    }

    @Test
    public void testGroupAndSearchHits() {
        BibEntry be = new BibEntry();
        be.setGroupHit(true);
        Assert.assertTrue(be.isGroupHit());
        be.setGroupHit(false);
        Assert.assertFalse(be.isGroupHit());
        be.setSearchHit(true);
        Assert.assertTrue(be.isSearchHit());
        be.setSearchHit(false);
        Assert.assertFalse(be.isSearchHit());

    }

    @Test
    public void testCiteKeyAndID() {
        BibEntry be = new BibEntry();
        Assert.assertFalse(be.hasCiteKey());
        be.setField("author", "Albert Einstein");
        be.setField(BibEntry.KEY_FIELD, "Einstein1931");
        Assert.assertTrue(be.hasCiteKey());
        Assert.assertEquals("Einstein1931", be.getCiteKey());
        Assert.assertEquals("Albert Einstein", be.getField("author"));
        be.clearField("author");
        Assert.assertNull(be.getField("author"));

        String id = IdGenerator.next();
        be.setId(id);
        Assert.assertEquals(id, be.getId());
    }
}
