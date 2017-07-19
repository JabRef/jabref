package net.sf.jabref.logic.importer;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Testes para complementar as classes de equivalência para importar uma database
 *
 * Formatos já testados por {@link ImportFormatReaderIntegrationTest}:
 * Ris
 * Isi
 * SilverPlatter
 * RepecNep
 * Ovid
 * Endnote
 * MsBib
 * Bibtex
 *
 * Formatos testados por essa classe:
 * Biblioscape
 * BibTeXML
 * Copac
 * Inspec
 * Medline
 * MedlinePlain
 * Mods
 *
 * Não foi possível testar:
 * FreeCite
 *
 * Não foi achado nenhum exemplo:
 * PdfContent
 * PdfXmp
 */

@RunWith(Parameterized.class)
public class ImportFormatComplementaryIntegrationTest {
    private ImportFormatReader reader;

    private final Path file;
    public final String format;
    private final int count;

    public ImportFormatComplementaryIntegrationTest(String resource, String format, int count) throws URISyntaxException {
        this.file = Paths.get(ImportFormatComplementaryIntegrationTest.class.getResource(resource).toURI());
        this.format = format;
        this.count = count;
    }

    @Before
    public void setUp() {
        reader = new ImportFormatReader();
        reader.resetImportFormats(JabRefPreferences.getInstance().getImportFormatPreferences(),
                JabRefPreferences.getInstance().getXMPPreferences());
    }

    @Test
    public void testImportFormatFromFile() throws Exception {
        assertEquals(count, reader.importFromFile(format, file).getDatabase().getEntries().size());
    }

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> importFormats() {
        Collection<Object[]> result = new ArrayList<>();
        result.add(new Object[] {"ezboys/BiblioscapeImportTest.btf", "biblioscape", 1});
        result.add(new Object[] {"ezboys/BibtexmlImportTest.xml", "bibtexml", 1});
        result.add(new Object[] {"ezboys/CopacImportTest.txt", "cpc", 1});
        result.add(new Object[] {"ezboys/InspecImportTest.txt", "inspec", 1});
        result.add(new Object[] {"ezboys/MedlineImportTest.xml", "medline", 1});
        result.add(new Object[] {"ezboys/MedlinePlainImportTest.txt", "medlineplain", 1});
        result.add(new Object[] {"ezboys/ModsImportTest.xml", "mods", 1});
        return result;
    }
}
