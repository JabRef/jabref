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

public class BibtexEntryTypes_bookTest {

    private BibEntryWriter writer;

    @Before
    public void setUpWriter() {
        writer = new BibEntryWriter(
                new LatexFieldFormatter(JabRefPreferences.getInstance().getLatexFieldFormatterPreferences()), true);
    }

    /**
     * A book with an explicit publisher.
     * <p>
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */

    //apenas com os campos obrigatorios/requeridos
    @Test
    public void testBibtexEntryType_book_reqFields() throws Exception {

        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("book");
        entry.setField("author", "George R.R. Martin");
        entry.setField("title","A Tormenta De Espadas");
        entry.setField("publisher","LeYa");
        entry.setField("year","2011");
        entry.setCiteKey("testeKey");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Book{testeKey," + OS.NEWLINE +
                "  title     = {A Tormenta De Espadas}," + OS.NEWLINE +
                "  publisher = {LeYa}," + OS.NEWLINE +
                "  year      = {2011}," + OS.NEWLINE +
                "  author    = {George R.R. Martin}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
        //System.out.print(actual);
    }

    //com todos os campos preenchidos
    @Test
    public void testBibtexEntryType_book_allFields() throws Exception {

        StringWriter stringWriter = new StringWriter();

        BibEntry entry = new BibEntry("book");
        entry.setField("author", "George R.R. Martin");
        entry.setField("title", "A Tormenta De Espadas");
        entry.setField("publisher", "LeYa");
        entry.setField("year", "2011");
        entry.setCiteKey("testeKey");
        entry.setField("volume", "3");
        entry.setField("series", "As Cronicas De Gelo E Fogo");
        entry.setField("address", "Sao Paulo");
        entry.setField("month", "setembro");
        entry.setField("note", "livro grande");

        writer.write(entry, stringWriter, BibDatabaseMode.BIBTEX);
        String actual = stringWriter.toString();

        // @formatter:off
        String expected = OS.NEWLINE + "@Book{testeKey," + OS.NEWLINE +
                "  title     = {A Tormenta De Espadas}," + OS.NEWLINE +
                "  publisher = {LeYa}," + OS.NEWLINE +
                "  year      = {2011}," + OS.NEWLINE +
                "  author    = {George R.R. Martin}," + OS.NEWLINE +
                "  volume    = {3}," + OS.NEWLINE +
                "  series    = {As Cronicas De Gelo E Fogo}," + OS.NEWLINE +
                "  address   = {Sao Paulo}," + OS.NEWLINE +
                "  month     = {setembro}," + OS.NEWLINE +
                "  note      = {livro grande}," + OS.NEWLINE +
                "}" + OS.NEWLINE;
        // @formatter:on

        assertEquals(expected, actual);
    }
}
