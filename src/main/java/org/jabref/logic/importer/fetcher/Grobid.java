package org.jabref.logic.importer.fetcher;

import org.jabref.Globals;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.importer.util.GrobidServiceException;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * This class is used to help making new entries faster by parsing a String.
 * An external parser called grobid is being used for that.
 */
public class Grobid {

    private static final Logger LOGGER = LoggerFactory.getLogger(Grobid.class);

    /**
     * Takes a whole String and filters the specific fields for the entry which is done
     * by an external parser.
     * @param plainText Plain reference citation to be parsed.
     * @return The BibEntry, if creation was possible.
     */
    public static List<BibEntry> parsePlainRefCit(String plainText) {
        try {
            TreeSet<String> plainReferences = new TreeSet<>();
            String[] plainReferencesArray = plainText.split(";;");
            for (int i = 0; i < plainReferencesArray.length; i++) {
                plainReferences.add(plainReferencesArray[i].trim());
            }
            plainReferences.remove("");
            if (plainReferences.size() == 0) {
                throw new ParserPipelineException("Your entered References are empty.");
            } else {
                ArrayList<BibEntry> resultsList = new ArrayList<>();
                for (String reference: plainReferences) {
                    parseBibToBibEntry(parseUsingGrobid(reference)).ifPresent(resultsList::add);
                }
                return resultsList;
            }
        } catch (ParserPipelineException e) {
        LOGGER.error("ParserPipeline Failed. Reason: "+e.getMessage());
        return new ArrayList<>();
        }
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result.
     * Takes a while, since the server has to look up the entry.
     */
    private static String parseUsingGrobid(String plainText) throws ParserPipelineException {
        try {
            return GrobidService.processCitation(plainText, 1);
        } catch (GrobidServiceException e) {
            throw new ParserPipelineException("The Pipeline failed to get the results from the GROBID client");
        }
    }

    /*
    private static String parseTeiToBib(String tei) {
        System.out.println(tei);
        //TODO: THIS IS A DUMMY METHOD RIGHT NOW, SHOULD BE IMPLEMENTED
        //OR NOT, WHEN CUSTOM GROBID SERVER WORKS
        return "@BOOK{DUMMY:1,\n" +
                "AUTHOR=\"John Doe\",\n" +
                "TITLE=\"The Book without Title\",\n" +
                "PUBLISHER=\"Dummy Publisher\",\n" +
                "YEAR=\"2100\",\n" +
                "}";
    }*/

    private static Optional<BibEntry> parseBibToBibEntry(String bibtexString) throws ParserPipelineException {
        try {
            return BibtexParser.singleFromString(bibtexString,
                    JabRefPreferences.getInstance().getImportFormatPreferences(), Globals.getFileUpdateMonitor());
        } catch (ParseException e) {
            throw new ParserPipelineException("Jabref failed to extract a BibEntry form bibtexString.");
        }
    }

}
