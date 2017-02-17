package org.jabref;

import java.io.IOException;
import java.io.StringReader;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class BibtexTestData {

    public static BibEntry getBibtexEntry(ImportFormatPreferences importFormatPreferences) throws IOException {
        BibDatabase database = getBibtexDatabase(importFormatPreferences);
        return database.getEntryByKey("HipKro03").get();
    }

    public static BibDatabase getBibtexDatabase(ImportFormatPreferences importFormatPreferences) throws IOException {
        String article = "@ARTICLE{HipKro03,\n" + "  author = {Eric von Hippel and Georg von Krogh},\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},\n"
                        + "  journal = {Organization Science},\n" + "  year = {2003},\n" + "  volume = {14},\n"
                        + "  pages = {209--223},\n" + "  number = {2},\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},\n"
                        + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n" + "  issn = {1526-5455},"
                        + "\n" + "  publisher = {INFORMS}\n" + "}";

        BibtexParser parser = new BibtexParser(importFormatPreferences);
        ParserResult result = parser.parse(new StringReader(article));

        return result.getDatabase();
    }
}
