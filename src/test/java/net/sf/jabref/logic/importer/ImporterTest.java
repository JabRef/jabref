package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import net.sf.jabref.logic.importer.fileformat.BibTeXMLImporter;
import net.sf.jabref.logic.importer.fileformat.BiblioscapeImporter;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.logic.importer.fileformat.CopacImporter;
import net.sf.jabref.logic.importer.fileformat.EndnoteImporter;
import net.sf.jabref.logic.importer.fileformat.FreeCiteImporter;
import net.sf.jabref.logic.importer.fileformat.InspecImporter;
import net.sf.jabref.logic.importer.fileformat.IsiImporter;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.logic.importer.fileformat.MedlinePlainImporter;
import net.sf.jabref.logic.importer.fileformat.ModsImporter;
import net.sf.jabref.logic.importer.fileformat.MsBibImporter;
import net.sf.jabref.logic.importer.fileformat.OvidImporter;
import net.sf.jabref.logic.importer.fileformat.PdfContentImporter;
import net.sf.jabref.logic.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.logic.importer.fileformat.RepecNepImporter;
import net.sf.jabref.logic.importer.fileformat.RisImporter;
import net.sf.jabref.logic.importer.fileformat.SilverPlatterImporter;
import net.sf.jabref.logic.xmp.XMPPreferences;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ImporterTest {

    @Parameter
    public Importer format;

    @Test(expected = NullPointerException.class)
    public void isRecognizedFormatWithNullThrowsException() throws IOException {
        format.isRecognizedFormat(null);
    }

    @Test(expected = NullPointerException.class)
    public void importDatabaseWithNullThrowsException() throws IOException {
        format.importDatabase(null);
    }

    @Test
    public void getFormatterNameDoesNotReturnNull() {
        Assert.assertNotNull(format.getName());
    }

    @Test
    public void getExtensionsDoesNotReturnNull() {
        Assert.assertNotNull(format.getExtensions());
    }

    @Test
    public void getIdDoesNotReturnNull() {
        Assert.assertNotNull(format.getId());
    }

    @Test
    public void getIdDoesNotContainWhitespace() {
        Pattern whitespacePattern = Pattern.compile("\\s");
        Assert.assertFalse(whitespacePattern.matcher(format.getId()).find());
    }

    @Test
    public void getIdStripsSpecialCharactersAndConvertsToLowercase() {
        Importer importer = Mockito.mock(Importer.class, Mockito.CALLS_REAL_METHODS);
        when(importer.getName()).thenReturn("*Test-Importer");
        Assert.assertEquals("testimporter", importer.getId());
    }

    @Test
    public void getDescriptionDoesNotReturnNull() {
        Assert.assertNotNull(format.getDescription());
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() {
        // all classes implementing {@link Importer}
        // sorted alphabetically

        ImportFormatPreferences importFormatPreferences = JabRefPreferences.getInstance().getImportFormatPreferences();
        XMPPreferences xmpPreferences = JabRefPreferences.getInstance().getXMPPreferences();
        // @formatter:off
        return Arrays.asList(
                new Object[]{new BiblioscapeImporter()},
                new Object[]{new BibtexImporter(importFormatPreferences)},
                new Object[]{new BibTeXMLImporter()},
                new Object[]{new CopacImporter()},
                new Object[]{new EndnoteImporter(importFormatPreferences)},
                new Object[]{new FreeCiteImporter(importFormatPreferences)},
                new Object[]{new InspecImporter()},
                new Object[]{new IsiImporter()},
                new Object[]{new MedlineImporter()},
                new Object[]{new MedlinePlainImporter()},
                new Object[]{new ModsImporter()},
                new Object[]{new MsBibImporter()},
                new Object[]{new OvidImporter()},
                new Object[]{new PdfContentImporter(importFormatPreferences)},
                new Object[]{new PdfXmpImporter(xmpPreferences)},
                new Object[]{new RepecNepImporter(importFormatPreferences)},
                new Object[]{new RisImporter()},
                new Object[]{new SilverPlatterImporter()}
        );
        // @formatter:on
    }
}
