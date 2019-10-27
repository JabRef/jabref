package org.jabref.cli;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JabRefCLITest {

    @Test
    void parsingLongOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getFileImport());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void parsingShortOptions() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getFileImport());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    void preferencesExport() throws Exception {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-x=some/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getPreferencesExport());
        assertTrue(cli.isDisableGui());
    }

    @Test
    void recognizesImportBibtex() throws Exception {
        String bibtex = "@article{test, title=\"test title\"}";
        JabRefCLI cli = new JabRefCLI(new String[]{"-ib", bibtex});
        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertTrue(cli.isBibtexImport());
        assertEquals(bibtex, cli.getBibtexImport());
    }

    @Test
    void recognizesImportBibtexLong() throws Exception {
        String bibtex = "@article{test, title=\"test title\"}";
        JabRefCLI cli = new JabRefCLI(new String[]{"-importBibtex", bibtex});
        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertTrue(cli.isBibtexImport());
        assertEquals(bibtex, cli.getBibtexImport());
    }
}
