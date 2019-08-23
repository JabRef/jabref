package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BibtexEntryType;
import org.jabref.model.entry.types.EntryType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BiblioscapeImporterTestTypes {

    private static Stream<Arguments> types() {
        return Stream.of(
                Arguments.of("journal", BibtexEntryType.Article),
                Arguments.of("book section", BibtexEntryType.InBook),
                Arguments.of("book", BibtexEntryType.Book),
                Arguments.of("conference", BibtexEntryType.InProceedings),
                Arguments.of("proceedings", BibtexEntryType.InProceedings),
                Arguments.of("report", BibtexEntryType.TechReport),
                Arguments.of("master thesis", BibtexEntryType.MastersThesis),
                Arguments.of("thesis", BibtexEntryType.PhdThesis),
                Arguments.of("master", BibtexEntryType.Misc)
        );
    }

    @ParameterizedTest
    @MethodSource("types")
    void importConvertsToCorrectBibType(String biblioscapeType, EntryType bibtexType) throws IOException {
        String bsInput = "--AU-- Baklouti, F.\n" + "--YP-- 1999\n" + "--KW-- Cells; Rna; Isoforms\n" + "--TI-- Blood\n"
                + "--RT-- " + biblioscapeType + "\n" + "------";

        List<BibEntry> bibEntries = new BiblioscapeImporter().importDatabase(new BufferedReader(new StringReader(bsInput)))
                                                             .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Baklouti, F.");
        entry.setField(StandardField.KEYWORDS, "Cells; Rna; Isoforms");
        entry.setField(StandardField.TITLE, "Blood");
        entry.setField(StandardField.YEAR, "1999");
        entry.setType(bibtexType);

        Assertions.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
