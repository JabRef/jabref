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

/**
 * The type mapping between BibTeXML and BibTeX is actually an identity mapping. The purpose of this class is to ensure
 * that all BibTeXML types are tested.
 */
public class BibTeXMLImporterTestTypes {

    public static Collection<String> types() {
        return Arrays.asList(new String[]{
                "article",
                "book",
                "booklet",
                "conference",
                "inbook",
                "incollection",
                "inproceedings",
                "manual",
                "mastersthesis",
                "misc",
                "phdthesis",
                "techreport",
                "unpublished"
        });
    }


    @ParameterizedTest
    @MethodSource("types")
    public void importConvertsToCorrectBibType(String type) throws IOException {
        String bibteXMLInput = "<?xml version=\"1.0\" ?>\n" + "<bibtex:file xmlns:bibtex=\"http://bibtexml.sf.net/\">\n"
                + "<bibtex:entry>\n" + "<bibtex:" + type + ">\n"
                + "<bibtex:author>Max Mustermann</bibtex:author>\n" + "<bibtex:keywords>java</bibtex:keywords>\n"
                + "<bibtex:title>Java tricks</bibtex:title>\n" + "<bibtex:year>2016</bibtex:year>\n" + "</bibtex:"
                + type + ">\n" + "</bibtex:entry>\n" + "</bibtex:file>";

        List<BibEntry> bibEntries = new BibTeXMLImporter().importDatabase(new BufferedReader(new StringReader(bibteXMLInput)))
                .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField("author", "Max Mustermann");
        entry.setField("keywords", "java");
        entry.setField("title", "Java tricks");
        entry.setField("year", "2016");
        entry.setType(type);

        Assertions.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
