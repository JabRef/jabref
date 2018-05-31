package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.jabref.logic.importer.fileformat.BibTeXMLImporter;
import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.FreeCiteImporter;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.SilverPlatterImporter;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ImporterTest {

    @Parameter public Importer format;

    @Test(expected = NullPointerException.class)
    public void isRecognizedFormatWithNullForBufferedReaderThrowsException() throws IOException {
        format.isRecognizedFormat((BufferedReader) null);
    }

    @Test(expected = NullPointerException.class)
    public void isRecognizedFormatWithNullForStringThrowsException() throws IOException {
        format.isRecognizedFormat((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void importDatabaseWithNullForBufferedReaderThrowsException() throws IOException {
        format.importDatabase((BufferedReader) null);
    }

    @Test(expected = NullPointerException.class)
    public void importDatabaseWithNullForStringThrowsException() throws IOException {
        format.importDatabase((String) null);
    }

    @Test
    public void getFormatterNameDoesNotReturnNull() {
        Assert.assertNotNull(format.getName());
    }

    @Test
    public void getFileTypeDoesNotReturnNull() {
        Assert.assertNotNull(format.getFileType());
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
        Importer importer = mock(Importer.class, Mockito.CALLS_REAL_METHODS);
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

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        // @formatter:off
        return Arrays.asList(
                new Object[]{new BiblioscapeImporter()},
                new Object[]{new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor())},
                new Object[]{new BibTeXMLImporter()},
                new Object[]{new CopacImporter()},
                new Object[]{new EndnoteImporter(importFormatPreferences)},
                new Object[]{new FreeCiteImporter(importFormatPreferences)},
                new Object[]{new InspecImporter()},
                new Object[]{new IsiImporter()},
                new Object[]{new MedlineImporter()},
                new Object[]{new MedlinePlainImporter()},
                new Object[]{new ModsImporter(importFormatPreferences)},
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
