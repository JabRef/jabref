package net.sf.jabref;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ParserResult;

import net.sf.jabref.model.database.BibtexDatabase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BibtexDatabaseTest {

    @Before
    public void setup() {
        Globals.prefs = JabRefPreferences.getInstance(); // set preferences for this test
    }

    @After
    public void teardown() {
        Globals.prefs = null;
    }

    /**
     * Some basic test cases for resolving strings.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testResolveStrings() throws IOException {

        try (FileReader fr = new FileReader("src/test/resources/net/sf/jabref/util/twente.bib")) {
        ParserResult result = BibtexParser.parse(fr);

        BibtexDatabase db = result.getDatabase();

        Assert.assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
        Assert.assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
        Assert.assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

        // Strings that are not found return just the given string.
        Assert.assertEquals("#unknown#", db.resolveForStrings("#unknown#"));
        }
    }
}
