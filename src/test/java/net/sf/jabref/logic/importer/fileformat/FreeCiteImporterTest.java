package net.sf.jabref.logic.importer.fileformat;

import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FreeCiteImporterTest {

    private FreeCiteImporter importer;


    @Before
    public void setUp() {
        importer = new FreeCiteImporter(JabRefPreferences.getInstance().getImportFormatPreferences());
    }

    @Test
    public void testGetFormatName() {
        assertEquals("text citations", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        assertEquals(FileExtensions.FREECITE, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        assertEquals("This importer parses text format citations using the online API of FreeCite.",
                importer.getDescription());
    }
}
