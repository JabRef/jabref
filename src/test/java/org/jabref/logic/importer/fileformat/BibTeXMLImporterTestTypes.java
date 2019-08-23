package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BibtexEntryType;
import org.jabref.model.entry.types.EntryType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The type mapping between BibTeXML and BibTeX is actually an identity mapping. The purpose of this class is to ensure
 * that all BibTeXML types are tested.
 */
public class BibTeXMLImporterTestTypes {

    public static Collection<EntryType> types() {
        return Arrays.asList(
                BibtexEntryType.Article,
                BibtexEntryType.Book,
                BibtexEntryType.Booklet,
                BibtexEntryType.Conference,
                BibtexEntryType.InBook,
                BibtexEntryType.InCollection,
                BibtexEntryType.InProceedings,
                BibtexEntryType.Manual,
                BibtexEntryType.MastersThesis,
                BibtexEntryType.Misc,
                BibtexEntryType.PhdThesis,
                BibtexEntryType.TechReport,
                BibtexEntryType.Unpublished);
    }

    @ParameterizedTest
    @MethodSource("types")
    public void importConvertsToCorrectBibType(EntryType type) throws IOException {
        String bibteXMLInput = "<?xml version=\"1.0\" ?>\n" + "<bibtex:file xmlns:bibtex=\"http://bibtexml.sf.net/\">\n"
                + "<bibtex:entry>\n" + "<bibtex:" + type.getName() + ">\n"
                + "<bibtex:author>Max Mustermann</bibtex:author>\n" + "<bibtex:keywords>java</bibtex:keywords>\n"
                + "<bibtex:title>Java tricks</bibtex:title>\n" + "<bibtex:year>2016</bibtex:year>\n" + "</bibtex:"
                + type.getName() + ">\n" + "</bibtex:entry>\n" + "</bibtex:file>";

        List<BibEntry> bibEntries = new BibTeXMLImporter().importDatabase(new BufferedReader(new StringReader(bibteXMLInput)))
                                                          .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Max Mustermann");
        entry.setField(StandardField.KEYWORDS, "java");
        entry.setField(StandardField.TITLE, "Java tricks");
        entry.setField(StandardField.YEAR, "2016");
        entry.setType(type);

        Assertions.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
