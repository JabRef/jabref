package net.sf.jabref;

import java.io.IOException;
import java.io.StringReader;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class BibtexTestData {

    public static BibEntry getBibtexEntry() throws IOException {
        BibDatabase database = getBibtexDatabase();
        return database.getEntryByKey("HipKro03");
    }

    public static BibDatabase getBibtexDatabase() throws IOException {
        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03,\n" + "  author = {Eric von Hippel and Georg von Krogh},\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
                        + "  journal = {Organization Science},\n" + "  year = {2003},\n" + "  volume = {14},\n"
                        + "  pages = {209--223},\n" + "  number = {2},\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n"
                        + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n" + "  issn = {1526-5455},"
                        + "\n" + "  publisher = {INFORMS}\n" + "}");

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = parser.parse();

        return result.getDatabase();
    }
}
