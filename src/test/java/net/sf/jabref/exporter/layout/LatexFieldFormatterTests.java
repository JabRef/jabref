package net.sf.jabref.exporter.layout;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by joerg on 05.10.2015.
 */
public class LatexFieldFormatterTests {

    private LatexFieldFormatter formatter;

    @BeforeClass
    public static void setUpBeforeClass(){
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        this.formatter = new LatexFieldFormatter();
    }

    @Test
    public void preserveNewlineInAbstractField() {
        String fieldName = "abstract";
        String text = "lorem ipsum lorem ipsum" + Globals.NEWLINE + "lorem ipsum lorem ipsum" + Globals.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{"+text+"}";

        assertEquals(expected, result);
    }

    @Test
    public void preserveNewlineInReviewField() {
        String fieldName = "review";
        String text = "lorem ipsum lorem ipsum" + Globals.NEWLINE + "lorem ipsum lorem ipsum" + Globals.NEWLINE;

        String result = formatter.format(text, fieldName);
        String expected = "{"+text+"}";

        assertEquals(expected, result);
    }
}
