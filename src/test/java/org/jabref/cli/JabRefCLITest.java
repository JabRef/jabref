package org.jabref.cli;

import java.util.Collections;
import java.util.List;

import javafx.util.Pair;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JabRefCLITest {

    private final String bibtex = "@article{test, title=\"test title\"}";

    @Test
    void emptyCLILeftOversLongOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
    }

    @Test
    void guiIsDisabledLongOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertTrue(cli.isDisableGui());
    }

    @Test
    void successfulParsingOfFileImportCLILongOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("some/file", cli.getFileImport());
    }

    @Test
    void successfulParsingOfFileExportCLILongOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void emptyCLILeftOversShortOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
    }

    @Test
    void guiIsDisabledShortOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertTrue(cli.isDisableGui());
    }

    @Test
    void successfulParsingOfFileImportShortOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("some/file", cli.getFileImport());
    }

    @Test
    void successfulParsingOfFileExportShortOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void emptyPreferencesLeftOver() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-x=some/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
    }

    @Test
    void successfulExportOfPreferences() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-x=some/file"});

        assertEquals("some/file", cli.getPreferencesExport());
    }

    @Test
    void guiDisabledForPreferencesExport() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-x=some/file"});

        assertTrue(cli.isDisableGui());
    }

    @Test
    void emptyLeftOversCLIShortImportingBibtex() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-ib", bibtex});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
    }

    @Test
    void recognizesImportBibtexShort() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-ib", bibtex});

        assertTrue(cli.isBibtexImport());
    }

    @Test
    void successfulParsingOfBibtexImportShort() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-ib", bibtex});

        assertEquals(bibtex, cli.getBibtexImport());
    }

    @Test
    void emptyLeftOversCLILongImportingBibtex() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-importBibtex", bibtex});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
    }

    @Test
    void recognizesImportBibtexLong() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-importBibtex", bibtex});

        assertTrue(cli.isBibtexImport());
    }

    @Test
    void successfulParsingOfBibtexImportLong() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-importBibtex", bibtex});

        assertEquals(bibtex, cli.getBibtexImport());
    }

    @Test
    void wrapStringList() {
        List<String> given = List.of("html", "simplehtml", "docbook5", "docbook4", "din1505", "bibordf", "tablerefs", "listrefs",
                "tablerefsabsbib", "harvard", "iso690rtf", "iso690txt", "endnote", "oocsv", "ris", "misq", "yaml", "bibtexml", "oocalc", "ods",
                "MSBib", "mods", "xmp", "pdf", "bib");
        String expected = """
                Available export formats: html, simplehtml, docbook5, docbook4, din1505, bibordf, tablerefs,
                listrefs, tablerefsabsbib, harvard, iso690rtf, iso690txt, endnote, oocsv, ris, misq, yaml, bibtexml,
                oocalc, ods, MSBib, mods, xmp, pdf, bib""";

        assertEquals(expected, "Available export formats: " + JabRefCLI.wrapStringList(given, 26));
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
                """;

        assertEquals(expected, JabRefCLI.alignStringTable(given));
    }
}
