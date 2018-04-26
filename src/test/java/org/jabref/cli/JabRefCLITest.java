package org.jabref.cli;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JabRefCLITest {

    @Test
    public void testCLIParsingLongOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getFileImport());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testCLIParsingShortOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getFileImport());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testPreferencesExport() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-x=some/file"});

        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertEquals("some/file", cli.getPreferencesExport());
        assertTrue(cli.isDisableGui());
    }

    @Test
    public void recognizesImportBibtex() {
        String bibtex = "@article{test, title=\"test title\"}";
        JabRefCLI cli = new JabRefCLI(new String[]{"-ib", bibtex});
        assertEquals(Collections.emptyList(), cli.getLeftOver());
        assertTrue(cli.isBibtexImport());
        assertEquals(bibtex, cli.getBibtexImport());
    }
}
