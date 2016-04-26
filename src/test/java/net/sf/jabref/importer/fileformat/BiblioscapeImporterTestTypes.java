package net.sf.jabref.importer.fileformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BiblioscapeImporterTestTypes {

    private BiblioscapeImporter bsImporter;

    @Parameter(value = 0)
    public String biblioscapeType;

    @Parameter(value = 1)
    public String expectedBibType;


    @Parameters
    public static Collection<String[]> types() {
        return Arrays.asList(new String[][] {{"journal", "article"}, {"book section", "inbook"}, {"book", "book"},
                {"conference", "inproceedings"}, {"proceedings", "inproceedings"}, {"report", "techreport"},
                {"master thesis", "mastersthesis"}, {"thesis", "phdthesis"}, {"master", "misc"}});
    }

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        bsImporter = new BiblioscapeImporter();
    }

    @Test
    public void importConvertsToCorrectBibType() throws IOException {
        String bsInput = "--AU-- Baklouti, F.\n" + "--YP-- 1999\n" + "--KW-- Cells; Rna; Isoforms\n" + "--TI-- Blood\n"
                + "--RT-- " + biblioscapeType + "\n" + "------";

        List<BibEntry> bibEntries = bsImporter.importEntries(
                new ByteArrayInputStream(bsInput.getBytes(StandardCharsets.UTF_8)), new OutputPrinterToNull());

        BibEntry entry = new BibEntry();
        entry.setField("author", "Baklouti, F.");
        entry.setField("keywords", "Cells; Rna; Isoforms");
        entry.setField("title", "Blood");
        entry.setField("year", "1999");
        entry.setType(expectedBibType);

        Assert.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
