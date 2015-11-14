package net.sf.jabref.model.entry;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibtexParser;

public class BibtexEntryTests {

    private BibtexEntry entry;


    @Before
    public void setup() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testDefaultConstructor() {
        entry = new BibtexEntry();
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
}
