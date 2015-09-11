package net.sf.jabref.importer;

import junit.framework.Assert;
import net.sf.jabref.model.database.BibtexDatabase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class OpenDatabaseActionTest {
    private final String defaultEncoding = "UTF-8";

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