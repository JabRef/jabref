package org.jabref.cli;

import java.util.Collections;

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
}
