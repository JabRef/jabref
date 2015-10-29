package net.sf.jabref.logic.cleanup;

import junit.framework.Assert;
import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AutoFormatterTest {
    private BibtexEntry entry;

    @Before
    public void setUp() {
        entry = new BibtexEntry();
    }

    @After
    public void teardown() {
        entry = null;
    }

    @Test
    public void replacesSuperscript() {
        entry.setField("field one", "1st");
        entry.setField("field two", "2nd");
        entry.setField("field three", "3rd");
        entry.setField("field four", "4th");
        entry.setField("field five", "21th");

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals("1\\textsuperscript{st}", entry.getField("field one"));
        Assert.assertEquals("2\\textsuperscript{nd}", entry.getField("field two"));
        Assert.assertEquals("3\\textsuperscript{rd}", entry.getField("field three"));
        Assert.assertEquals("4\\textsuperscript{th}", entry.getField("field four"));
        Assert.assertEquals("21\\textsuperscript{th}", entry.getField("field five"));
    }

    @Test
    public void replacesSuperscriptsInAllFields() {
        entry.setField("field_one", "1st");
        entry.setField("field_two", "1st");

        new AutoFormatter(entry).applySuperscripts();

        for(String name: entry.getAllFields()) {
            Assert.assertEquals("1\\textsuperscript{st}", entry.getField(name));
        }
    }

    @Test
    public void replaceSuperscriptsEmptyFields() {
        entry.setField("empty field", "");
        entry.setField("null field", null);

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals("", entry.getField("empty field"));
        Assert.assertEquals(null, entry.getField("null field"));
    }

    @Test
    public void replaceSuperscriptsIgnoresCase() {
        entry.setField("lowercase", "1st");
        entry.setField("uppercase", "1ST");
        entry.setField("mixedcase", "1sT");

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals("1\\textsuperscript{st}", entry.getField("lowercase"));
        Assert.assertEquals("1\\textsuperscript{ST}", entry.getField("uppercase"));
        Assert.assertEquals("1\\textsuperscript{sT}", entry.getField("mixedcase"));
    }

    @Test
    public void replaceSuperscriptsInMultilineStrings() {
        entry.setField("multiline", "replace on 1st line\nand on 2nd line.");

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals(
                "replace on 1\\textsuperscript{st} line\nand on 2\\textsuperscript{nd} line.",
                entry.getField("multiline")
        );
    }

    @Test
    public void replaceAllSuperscripts() {
        entry.setField("multiple", "1st 2nd 3rd 4th");

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals(
                "1\\textsuperscript{st} 2\\textsuperscript{nd} 3\\textsuperscript{rd} 4\\textsuperscript{th}",
                entry.getField("multiple")
        );
    }

    @Test
    public void ignoreSuperscriptsInsideWords() {
        entry.setField("word boundaries", "1st 1stword words1st inside1stwords");

        new AutoFormatter(entry).applySuperscripts();

        Assert.assertEquals(
                "1\\textsuperscript{st} 1stword words1st inside1stwords",
                entry.getField("word boundaries")
        );
    }
}