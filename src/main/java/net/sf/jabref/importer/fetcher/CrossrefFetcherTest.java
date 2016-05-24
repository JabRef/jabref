package net.sf.jabref.importer.fetcher;

import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class CrossrefFetcherTest {
    public static void main(String[] args) throws IOException {
        Globals.prefs = JabRefPreferences.getInstance();

        BibtexParser parser = new BibtexParser(new FileReader("C:\\Users\\Stefan\\Desktop\\Github\\references\\references.bib"));
        //BibtexParser parser = new BibtexParser(new FileReader("C:\\Users\\Stefan\\Desktop\\Vorlesungen\\Promotion\\Paper\\master.bib"));
        ParserResult result = parser.parse();
        BibDatabase db = result.getDatabase();

        int total = result.getDatabase().getEntryCount();

        int dois = 0;
        int doiFound = 0;
        int doiNew = 0;
        int doiIdentical = 0;

        for (BibEntry entry : db.getEntries()) {
            Optional<DOI> origDOI = DOI.build(entry.getField("doi"));
            if (origDOI.isPresent()) {
                dois++;
                Optional<DOI> crossrefDOI = CrossRef.findDOI(entry);
                if (crossrefDOI.isPresent()) {
                    doiFound++;
                    if (origDOI.get().getDOI().equalsIgnoreCase(crossrefDOI.get().getDOI())) {
                        doiIdentical++;
                    } else {
                        System.out.println("DOI not identical for : " + entry);
                    }
                } else {
                    System.out.println("DOI not found for: " + entry);
                }
            } else {
                Optional<DOI> crossrefDOI = CrossRef.findDOI(entry);
                if (crossrefDOI.isPresent()) {
                    //System.out.println("New DOI found for: " + entry);
                    doiNew++;
                }
            }
        }

        System.out.println("---------------------------------");
        System.out.println("Total DB size: " + total);
        System.out.println("Total DOIs: " + dois);
        System.out.println("DOIs found: " + doiFound);
        System.out.println("DOIs identical: " + doiIdentical);
        System.out.println("New DOIs found: " + doiNew);
    }
}
