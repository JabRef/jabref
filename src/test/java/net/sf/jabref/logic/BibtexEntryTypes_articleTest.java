package net.sf.jabref.logic;

import java.io.StringWriter;

import net.sf.jabref.logic.bibtex.BibEntryWriter;
import net.sf.jabref.logic.bibtex.LatexFieldFormatter;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class BibtexEntryTypes_articleTest {

    private BibEntryWriter writer;

    @Before
    public void setUpWriter() {
        writer = new BibEntryWriter(
                new LatexFieldFormatter(JabRefPreferences.getInstance().getLatexFieldFormatterPreferences()), true);
    }

    /**
     * An article from a journal or magazine.
     * <p>
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */

    //apenas com os campos obrigatorios/requeridos
    @Test
    public void testBibtexEntryType_article_reqFields() throws Exception {

        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        entry.setField("author", "Auri V");
        entry.setField("title", "Aulas ES2");
        entry.setField("journal", "DC");
        entry.setField("year", "2017");
        entry.setCiteKey("testeKey");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{testeKey," + OS.NEWLINE +
                "  author  = {Auri V}," + OS.NEWLINE +
                "  title   = {Aulas ES2}," + OS.NEWLINE +
                "  journal = {DC}," + OS.NEWLINE +
                "  year    = {2017}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
        //System.out.print(actual);
    }

    //com todos os campos preenchidos
    @Test
    public void testBibtexEntryType_article_allFields() throws Exception {

        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("article");
        entry.setField("author", "Auri V");
        entry.setField("title", "Aulas ES2");
        entry.setField("journal", "DC");
        entry.setField("year", "2017");
        entry.setCiteKey("testeKey");
        entry.setField("volume", "2");
        entry.setField("number", "17");
        entry.setField("pages", "10");
        entry.setField("month", "julho");
        entry.setField("note", "artiguinho");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Article{testeKey," + OS.NEWLINE +
                "  author  = {Auri V}," + OS.NEWLINE +
                "  title   = {Aulas ES2}," + OS.NEWLINE +
                "  journal = {DC}," + OS.NEWLINE +
                "  year    = {2017}," + OS.NEWLINE +
                "  volume  = {2}," + OS.NEWLINE +
                "  number  = {17}," + OS.NEWLINE +
                "  pages   = {10}," + OS.NEWLINE +
                "  month   = {julho}," + OS.NEWLINE +
                "  note    = {artiguinho}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }
}

