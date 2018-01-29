package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BibTeXMLImporterTestTypes {

    public static Collection<String[]> types() {
        return Arrays.asList(new String[][] {
                {"journal", "article"},
                {"book section", "inbook"},
                {"book", "book"},
                {"conference", "inproceedings"},
                {"proceedings", "inproceedings"},
                {"report", "techreport"},
                {"master thesis", "mastersthesis"},
                {"thesis", "phdthesis"},
                {"master", "misc"}});
    }


    @ParameterizedTest
    @MethodSource("types")
    public void importConvertsToCorrectBibType(String actualType, String expectedType) throws IOException {
        String bibteXMLInput = "<?xml version=\"1.0\" ?>\n" + "<bibtex:file xmlns:bibtex=\"http://bibtexml.sf.net/\">\n"
                + "<bibtex:entry>\n" + "<bibtex:" + actualType + ">\n"
                + "<bibtex:author>Max Mustermann</bibtex:author>\n" + "<bibtex:keywords>java</bibtex:keywords>\n"
                + "<bibtex:title>Java tricks</bibtex:title>\n" + "<bibtex:year>2016</bibtex:year>\n" + "</bibtex:"
                + actualType + ">\n" + "</bibtex:entry>\n" + "</bibtex:file>";

        List<BibEntry> bibEntries = new BibTeXMLImporter().importDatabase(new BufferedReader(new StringReader(bibteXMLInput)))
                .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField("author", "Max Mustermann");
        entry.setField("keywords", "java");
        entry.setField("title", "Java tricks");
        entry.setField("year", "2016");
        entry.setType(expectedType);

        Assertions.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
