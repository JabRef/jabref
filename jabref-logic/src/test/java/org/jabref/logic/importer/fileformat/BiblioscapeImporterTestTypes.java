package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BiblioscapeImporterTestTypes {

    private static Stream<String[]> types() {
        return Arrays.stream(new String[][] {
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
    public void importConvertsToCorrectBibType(String biblioscapeType, String bibtexType) throws IOException {
        String bsInput = "--AU-- Baklouti, F.\n" + "--YP-- 1999\n" + "--KW-- Cells; Rna; Isoforms\n" + "--TI-- Blood\n"
                + "--RT-- " + biblioscapeType + "\n" + "------";

        List<BibEntry> bibEntries = new BiblioscapeImporter().importDatabase(new BufferedReader(new StringReader(bsInput)))
                .getDatabase().getEntries();

        BibEntry entry = new BibEntry();
        entry.setField("author", "Baklouti, F.");
        entry.setField("keywords", "Cells; Rna; Isoforms");
        entry.setField("title", "Blood");
        entry.setField("year", "1999");
        entry.setType(bibtexType);

        Assertions.assertEquals(Collections.singletonList(entry), bibEntries);
    }
}
