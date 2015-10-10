package net.sf.jabref.importer;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.database.BibtexDatabase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class OpenDatabaseActionTest {
    private final String defaultEncoding = "UTF-8";

    @BeforeClass
    public static void setUpGlobalsPrefs() {
        // otherwise FieldContentParser (called by BibtexParser) crashes
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void headerless() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(new File(OpenDatabaseActionTest.class.getResource("headerless.bib").getFile()), defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
    }

    @Test
    public void jabrefHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(new File(OpenDatabaseActionTest.class.getResource("jabref-header.bib").getFile()), defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
        // Version
        Assert.assertEquals(2, result.getJabrefMajorVersion());
        Assert.assertEquals(9, result.getJabrefMinorVersion());
        // Encoding
        Assert.assertEquals("UTF-8", result.getEncoding());
    }

    @Test
    public void testAbsentVersionHeader() throws IOException {
        // newer JabRef versions do not put a header
        ParserResult result = OpenDatabaseAction.loadDatabase(new File(OpenDatabaseActionTest.class.getResource("encoding-header.bib").getFile()), defaultEncoding);

        // Version
        Assert.assertEquals(0, result.getJabrefMajorVersion());
        Assert.assertEquals(0, result.getJabrefMinorVersion());
    }

    @Test
    public void encodingHeader() throws IOException {
        ParserResult result = OpenDatabaseAction.loadDatabase(new File(OpenDatabaseActionTest.class.getResource("encoding-header.bib").getFile()), defaultEncoding);
        BibtexDatabase db = result.getDatabase();

        // Entry
        Assert.assertEquals(1, db.getEntryCount());
        Assert.assertEquals("2014", db.getEntryByKey("1").getField("year"));
        // Encoding
        Assert.assertEquals("UTF-8", result.getEncoding());
    }
}