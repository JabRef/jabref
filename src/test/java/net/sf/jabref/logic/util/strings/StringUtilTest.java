package net.sf.jabref.logic.util.strings;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

public class StringUtilTest {
    @BeforeClass
    public static void loadPreferences() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testUnifyLineBreaks() throws Exception {
        // Mac < v9
        String result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\r");
        assertEquals(Globals.NEWLINE, result);
        // Windows
        result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\r\n");
        assertEquals(Globals.NEWLINE, result);
        // Unix
        result = StringUtil.unifyLineBreaksToConfiguredLineBreaks("\n");
        assertEquals(Globals.NEWLINE, result);
    }
}