package org.jabref.cli;

import java.util.List;

import javafx.util.Pair;

import org.jabref.logic.os.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JabKitCliOptionsTest {

    private final String bibtex = "@article{test, title=\"test title\"}";

    @Test
    void emptyCLILeftOversLongOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals(List.of(), cli.getLeftOver());
    }

    @Test
    void successfulParsingOfFileImportCLILongOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("some/file", cli.getFileImport());
    }

    @Test
    void successfulParsingOfFileExportCLILongOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void emptyCLILeftOversShortOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals(List.of(), cli.getLeftOver());
    }

    @Test
    void successfulParsingOfFileImportShortOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("some/file", cli.getFileImport());
    }

    @Test
    void successfulParsingOfFileExportShortOptions() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void emptyPreferencesLeftOver() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-n", "-x=some/file"});

        assertEquals(List.of(), cli.getLeftOver());
    }

    @Test
    void successfulExportOfPreferences() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-n", "-x=some/file"});

        assertEquals("some/file", cli.getPreferencesExport());
    }

    @Test
    void emptyLeftOversCLIShortImportingBibtex() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-ib", bibtex});

        assertEquals(List.of(), cli.getLeftOver());
    }

    @Test
    void recognizesImportBibtexShort() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-ib", bibtex});

        assertTrue(cli.isBibtexImport());
    }

    @Test
    void successfulParsingOfBibtexImportShort() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-ib", bibtex});

        assertEquals(bibtex, cli.getBibtexImport());
    }

    @Test
    void emptyLeftOversCLILongImportingBibtex() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-importBibtex", bibtex});

        assertEquals(List.of(), cli.getLeftOver());
    }

    @Test
    void recognizesImportBibtexLong() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-importBibtex", bibtex});

        assertTrue(cli.isBibtexImport());
    }

    @Test
    void successfulParsingOfBibtexImportLong() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"-importBibtex", bibtex});

        assertEquals(bibtex, cli.getBibtexImport());
    }

    @Test
    void alignStringTable() {
        List<Pair<String, String>> given = List.of(
                new Pair<>("Apple", "Slice"),
                new Pair<>("Bread", "Loaf"),
                new Pair<>("Paper", "Sheet"),
                new Pair<>("Country", "County"));

        String expected = """
                Apple   : Slice
                Bread   : Loaf
                Paper   : Sheet
                Country : County
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected, JabKitCliOptions.alignStringTable(given));
    }

    @Test
    void checkConsistencyOption() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"--check-consistency", "jabref-authors.bib"});
        assertTrue(cli.isCheckConsistency());
        assertEquals("jabref-authors.bib", cli.getCheckConsistency());
    }

    @Test
    void checkConsistencyOutputFormatOption() throws Exception {
        JabKitCliOptions cli = new JabKitCliOptions(new String[] {"--check-consistency", "jabref-authors.bib", "--output-format", "csv"});
        assertEquals("csv", cli.getCheckConsistencyOutputFormat());
    }
}
