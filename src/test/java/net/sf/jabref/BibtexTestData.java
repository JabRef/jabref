package net.sf.jabref;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.Assert;

import java.io.StringReader;

public class BibtexTestData {

    public static BibtexEntry getBibtexEntry() {
        BibtexDatabase database = getBibtexDatabase();
        return database.getEntriesByKey("HipKro03")[0];
    }

    public static BibtexDatabase getBibtexDatabase() {
        // @formatter:off
        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03,\n"
                        + "  author = {Eric von Hippel and Georg von Krogh},\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
                        + "  journal = {Organization Science},\n"
                        + "  year = {2003},\n"
                        + "  volume = {14},\n"
                        + "  pages = {209--223},\n"
                        + "  number = {2},\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n"
                        + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
                        + "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}\n" + "}"
        );
        // @formatter:on

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = null;
        try {
            result = parser.parse();
        } catch (Exception e) {
            Assert.fail();
        }
        return result.getDatabase();
    }
}
