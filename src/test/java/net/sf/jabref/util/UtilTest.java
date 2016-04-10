package net.sf.jabref.util;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.layout.format.NameFormatter;
import net.sf.jabref.importer.ParserResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class UtilTest {

    private BibDatabase database;
    private BibEntry entry;


    @Before
    public void setUp() throws IOException {
        // Required by BibtexParser -> FieldContentParser
        Globals.prefs = JabRefPreferences.getInstance();

        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03," + "\n" + "  author = {Eric von Hippel and Georg von Krogh}," + "\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},"
                        + "\n" + "  journal = {Organization Science}," + "\n" + "  year = {2003}," + "\n"
                        + "  volume = {14}," + "\n" + "  pages = {209--223}," + "\n" + "  number = {2}," + "\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},"
                        + "\n" + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
                        + "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}" + "\n" + "}");

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = null;

        result = parser.parse();

        database = result.getDatabase();
        entry = database.getEntryByKey("HipKro03");

        Assert.assertNotNull(database);
        Assert.assertNotNull(entry);
    }

    @Test
    @Ignore
    public void testFieldAndFormat() {
        Assert.assertEquals("Eric von Hippel and Georg von Krogh",
                net.sf.jabref.util.Util.getFieldAndFormat("[author]", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh",
                net.sf.jabref.util.Util.getFieldAndFormat("author", entry, database));

        Assert.assertEquals("", net.sf.jabref.util.Util.getFieldAndFormat("[unknownkey]", entry, database));

        Assert.assertEquals("", net.sf.jabref.util.Util.getFieldAndFormat("[:]", entry, database));

        Assert.assertEquals("", net.sf.jabref.util.Util.getFieldAndFormat("[:lower]", entry, database));

        Assert.assertEquals("eric von hippel and georg von krogh",
                net.sf.jabref.util.Util.getFieldAndFormat("[author:lower]", entry, database));

        Assert.assertEquals("HipKro03", net.sf.jabref.util.Util.getFieldAndFormat("[bibtexkey]", entry, database));

        Assert.assertEquals("HipKro03", net.sf.jabref.util.Util.getFieldAndFormat("[bibtexkey:]", entry, database));
    }

    @Test
    @Ignore
    public void testUserFieldAndFormat() {

        List<String> names = Globals.prefs.getStringList(NameFormatter.NAME_FORMATER_KEY);

        List<String> formats = Globals.prefs.getStringList(NameFormatter.NAME_FORMATTER_VALUE);

        try {

            List<String> f = new LinkedList<>(formats);
            List<String> n = new LinkedList<>(names);

            n.add("testMe123454321");
            f.add("*@*@test");

            Globals.prefs.putStringList(NameFormatter.NAME_FORMATER_KEY, n);
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATTER_VALUE, f);

            Assert.assertEquals("testtest",
                    net.sf.jabref.util.Util.getFieldAndFormat("[author:testMe123454321]", entry, database));

        } finally {
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATER_KEY, names);
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATTER_VALUE, formats);
        }
    }

    @Test
    public void testExpandBrackets() {

        Assert.assertEquals("", net.sf.jabref.util.Util.expandBrackets("", entry, database));

        Assert.assertEquals("dropped", net.sf.jabref.util.Util.expandBrackets("drop[unknownkey]ped", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh",
                net.sf.jabref.util.Util.expandBrackets("[author]", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                net.sf.jabref.util.Util.expandBrackets("[author] are two famous authors.", entry, database));

        Assert.assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                net.sf.jabref.util.Util.expandBrackets("[author] are two famous authors.", entry, database));

        Assert.assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                net.sf.jabref.util.Util.expandBrackets("[author] have published [title] in [journal].", entry,
                        database));
    }

}
