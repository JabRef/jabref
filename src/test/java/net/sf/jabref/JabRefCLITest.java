package net.sf.jabref;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class JabRefCLITest {

    @Test
    public void testCLIParsingLongOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"--nogui", "--import=some/file", "--output=some/export/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getFileImport());
        Assert.assertTrue(cli.isDisableGui());
        Assert.assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testCLIParsingShortOptions() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-i=some/file", "-o=some/export/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getFileImport());
        Assert.assertTrue(cli.isDisableGui());
        Assert.assertEquals("some/export/file", cli.getFileExport());
    }

    @Test
    public void testPreferencesExport() {
        JabRefCLI cli = new JabRefCLI(new String[] {"-n", "-x=some/file"});

        Assert.assertEquals("[]", Arrays.toString(cli.getLeftOver()));
        Assert.assertEquals("some/file", cli.getPreferencesExport());
        Assert.assertTrue(cli.isDisableGui());
    }

}