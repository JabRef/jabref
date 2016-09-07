package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
public class BibTeXMLImporterTestTypes {

    private BibTeXMLImporter bibteXMLImporter;

    @Parameter(value = 0)
    public String bibteXMLType;

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
        bibteXMLImporter = new BibTeXMLImporter();
    }

    @Test
    public void importConvertsToCorrectBibType() throws IOException {
        String bibteXMLInput = "<?xml version=\"1.0\" ?>\n" + "<bibtex:file xmlns:bibtex=\"http://bibtexml.sf.net/\">\n"
                + "<bibtex:entry>\n" + "<bibtex:" + expectedBibType + ">\n"
                + "<bibtex:author>Max Mustermann</bibtex:author>\n" + "<bibtex:keywords>java</bibtex:keywords>\n"
                + "<bibtex:title>Java tricks</bibtex:title>\n" + "<bibtex:year>2016</bibtex:year>\n" + "</bibtex:"
                + expectedBibType + ">\n" + "</bibtex:entry>\n" + "</bibtex:file>";

        List<BibEntry> bibEntries = bibteXMLImporter.importDatabase(new BufferedReader(new StringReader(bibteXMLInput)))
                .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField("author", "Max Mustermann");
        entry.setField("keywords", "java");
        entry.setField("title", "Java tricks");
        entry.setField("year", "2016");
        entry.setType(expectedBibType);

        Assert.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
