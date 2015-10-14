package net.sf.jabref.bibtex;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class BibtexEntryWriterTest {

    private BibtexEntryWriter writer;

    @BeforeClass
    public static void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        // make sure that we use the "new style" serialization
        Globals.prefs.putInt(JabRefPreferences.WRITEFIELD_SORTSTYLE, 0);

    }

    @Before
    public void setUpWriter() {
        writer = new BibtexEntryWriter(new LatexFieldFormatter(), true);
    }

    @Test
    public void testSerialization() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibtexEntry entry = new BibtexEntry("1234", BibtexEntryType.getType("Article"));
        //set a required field
        entry.setField("author", "Foo Bar");
        entry.setField("journal", "International Journal of Something");
        //set an optional field
        entry.setField("number", "1");
        entry.setField("note", "some note");

        writer.write(entry, stringWriter);

        String actual = stringWriter.toString();

        String expected = "@Article{," + Globals.NEWLINE +
                "  Author                   = {Foo Bar}," + Globals.NEWLINE +
                "  Journal                  = {International Journal of Something}," + Globals.NEWLINE +
                "  Note                     = {some note}," + Globals.NEWLINE +
                "  Number                   = {1}" + Globals.NEWLINE +
                "}" + Globals.NEWLINE;

        assertEquals(expected, actual);
    }
}
