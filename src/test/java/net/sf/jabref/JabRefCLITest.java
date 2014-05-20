package net.sf.jabref;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JabRefCLITest {

    @Test
    public void testCLIParsingLongOptions() {
        JabRefCLI cli = new JabRefCLI(new String[]{"--nogui", "--import=some/file", "--output=some/export/file"});

        assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        assertEquals("some/file", cli.importFile.getStringValue());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.exportFile.getStringValue());
    }

    @Test
    public void testCLIParsingShortOptions() {
        JabRefCLI cli = new JabRefCLI(new String[]{"-n", "-i=some/file", "-o=some/export/file"});

        assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        assertEquals("some/file", cli.importFile.getStringValue());
        assertTrue(cli.isDisableGui());
        assertEquals("some/export/file", cli.exportFile.getStringValue());
    }

}
