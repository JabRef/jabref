package net.sf.jabref.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ImportFormatTest {

    @Parameter
    public ImportFormat format;

    @Test(expected = NullPointerException.class)
    public void isRecognizedFormatWithNullThrowsException() throws IOException {
        format.isRecognizedFormat((InputStream)null);
    }

    @Test(expected = NullPointerException.class)
    public void importDatabaseWithNullThrowsException() throws IOException {
        format.importDatabase((InputStream)null);
    }

    @Test
    public void importDatabaseWithUnrecognizedInputDoesNotReturnNull() throws IOException {
        InputStream stream = new ByteArrayInputStream("!#!bad string".getBytes(StandardCharsets.UTF_8));
        Assert.assertNotNull(format.importDatabase(stream));
    }

    @Test
    public void getFormatterNameDoesNotReturnNull() {
        Assert.assertNotNull(format.getFormatName());
    }

    @Test
    @Ignore
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
        ImportFormat importFormat = Mockito.mock(ImportFormat.class, Mockito.CALLS_REAL_METHODS);
        when(importFormat.getFormatName()).thenReturn("*Test-Importer");
        Assert.assertEquals("testimporter", importFormat.getId());
    }

    @Test
    @Ignore
    public void getDescriptionDoesNotReturnNull() {
        Assert.assertNotNull(format.getDescription());
    }

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() {
        // all classes implementing {@link ImportFormat}
        // sorted alphabetically

        // @formatter:off
        return Arrays.asList(
                new Object[]{new BiblioscapeImporter()},
                new Object[]{new BibtexImporter()},
                new Object[]{new BibteXMLImporter()},
                new Object[]{new CopacImporter()},
                new Object[]{new EndnoteImporter()},
                new Object[]{new FreeCiteImporter()},
                new Object[]{new InspecImporter()},
                new Object[]{new IsiImporter()},
                new Object[]{new MedlineImporter()},
                new Object[]{new MedlinePlainImporter()},
                new Object[]{new MsBibImporter()},
                new Object[]{new OvidImporter()},
                new Object[]{new PdfContentImporter()},
                new Object[]{new PdfXmpImporter()},
                new Object[]{new RepecNepImporter()},
                new Object[]{new RisImporter()},
                new Object[]{new SilverPlatterImporter()}
        );
        // @formatter:on
    }
}