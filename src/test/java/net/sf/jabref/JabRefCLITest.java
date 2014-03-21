package net.sf.jabref;

import junit.framework.TestCase;

import java.util.Arrays;

public class JabRefCLITest extends TestCase {

    public void testCLIParsingLongOptions() {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        assertEquals("some/file", cli.importFile.getStringValue());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.exportFile.getStringValue());
    }

    public void testCLIParsingShortOptions() {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        assertEquals("some/file", cli.importFile.getStringValue());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.exportFile.getStringValue());
    }

}
