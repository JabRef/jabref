package net.sf.jabref.importer.fileformat;

import java.util.Arrays;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FreeCiteImporterTest {

    private FreeCiteImporter importer;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new FreeCiteImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("text citations", importer.getFormatName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(Arrays.asList(".txt",".xml"), importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This importer parses text format citations using the online API of FreeCite.", importer.getDescription());
    }
}
