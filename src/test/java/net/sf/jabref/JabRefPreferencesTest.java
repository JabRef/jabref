package net.sf.jabref;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JabRefPreferencesTest {

    private JabRefPreferences prefs;

    private JabRefPreferences backup;


    @Before
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        backup = prefs;
    }

    @Test
    public void testPreferencesImport() throws JabRefException {
        // the primary sort field has been changed to "editor" in this case
        File importFile = new File("src/test/resources/net/sf/jabref/customPreferences.xml");

        prefs.importPreferences(importFile.getAbsolutePath());

        String expected = "my proxy host";
        String actual = prefs.get(JabRefPreferences.PROXY_HOSTNAME);

        assertEquals(expected, actual);
    }

    @Test
    public void getDefaultEncodingReturnsPreviouslyStoredEncoding() {
        prefs.setDefaultEncoding(StandardCharsets.UTF_16BE);
        assertEquals(StandardCharsets.UTF_16BE, prefs.getDefaultEncoding());
    }

    @After
    public void tearDown() {
        //clean up preferences to default state
        prefs.overwritePreferences(backup);
    }

}
