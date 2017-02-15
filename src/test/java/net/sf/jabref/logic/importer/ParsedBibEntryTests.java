package net.sf.jabref.logic.importer;

import java.util.Optional;

import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ParsedBibEntryTests {


    private ImportFormatPreferences importFormatPreferences;


    @Before
    public void setUp() {
        importFormatPreferences = JabRefPreferences.getInstance().getImportFormatPreferences();
    }

    @Test
    public void testGetPublicationDate() throws ParseException {

        Assert.assertEquals(Optional.of("2003-02"),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = #FEB# }",
                        importFormatPreferences)).get().getPublicationDate());

        Assert.assertEquals(Optional.of("2003-03"),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = 3 }",
                        importFormatPreferences)).get().getPublicationDate());

        Assert.assertEquals(Optional.of("2003"),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}}", importFormatPreferences))
                        .get().getPublicationDate());

        Assert.assertEquals(Optional.empty(),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, month = 3 }", importFormatPreferences)).get()
                        .getPublicationDate());

        Assert.assertEquals(Optional.empty(),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, author={bla}}", importFormatPreferences))
                        .get().getPublicationDate());

        Assert.assertEquals(Optional.of("2003-12"),
                (BibtexParser.singleFromString("@ARTICLE{HipKro03, year = {2003}, month = #DEC# }",
                        importFormatPreferences)).get().getPublicationDate());

    }

}
