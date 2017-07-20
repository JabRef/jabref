package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CsvImporterTestTypes {
    private CsvImporter csvImporter;

    @Parameter(value = 0)
    public String csvType;

    @Parameter(value = 1)
    public String expectedCsvType;


    @Parameters
    public static Collection<String[]> types() {
        return Arrays.asList(new String[][] {{"7", "article"}, {"1", "book"}, {"2", "booklet"},
                {"5", "inbook"}, {"6", "inproceedings"}, {"8", "manual"}, {"9", "mastersthesis"},
                {"10", "misc"}, {"3", "proceedings"}, {"13", "techreport"}, {"14", "unpublished"}});
    }

    @Before
    public void setUp() throws Exception {
        csvImporter = new CsvImporter();
    }

    @Test
    public void importConvertsToCorrectBibType() throws IOException {
        String bsInput = "BibliographyType,Author\n" + csvType + ",\"Corso\"";

        List<BibEntry> csvEntries = csvImporter.importDatabase(new BufferedReader(new StringReader(bsInput)))
                .getDatabase().getEntries();

        String returnedType = csvEntries.get(0).getType();

        Assert.assertEquals(expectedCsvType, returnedType);
    }
}
