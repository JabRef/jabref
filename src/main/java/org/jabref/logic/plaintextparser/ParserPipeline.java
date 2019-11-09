package org.jabref.logic.plaintextparser;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.HttpPostService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is used to help making new entries faster by parsing a String.
 * An external parser called grobid is being used for that.
 */
public class ParserPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserPipeline.class);
    private static final String GROBID_PROCESS_CITATIONS_URL = "http://localhost:8070/api/processCitation";
    private static HttpPostService grobidPostService;
    private static BibEntry bibEntry;

    /**
     * Takes a whole String and filters the specific fields for the entry which is done
     * by an external parser.
     * @param plainText Plain reference citation to be parsed.
     * @return The BibEntry, if creation was possible.
     */
    public static Optional<BibEntry> parsePlainRefCit(String plainText) {
        try {
            return Optional.of(parseBibToBibEntry(parseTeiToBib(parseUsingGrobid(plainText))));
        } catch (ParserPipelineException e) {
        LOGGER.error("ParserPipeline Failed. Reason: "+e.getMessage());
        return Optional.empty();
        }
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result.
     */
    private static String parseUsingGrobid(String plainText) throws ParserPipelineException {
        if (grobidPostService == null) {
            try {
                grobidPostService = new HttpPostService(GROBID_PROCESS_CITATIONS_URL);
            } catch (URISyntaxException e) {
                throw new ParserPipelineException("HttpPostService could not be created");
            }
        }
        try {
            HttpEntity serverResponse = grobidPostService.sendPostAndWait(
                    Map.of("citations", plainText, "consolidateCitations", "1")).getEntity();
            if (serverResponse == null) {
                throw new ParserPipelineException("The server response does not contain anything.");
            }
            InputStream serverResponseAsStream = serverResponse.getContent();
            return IOUtils.toString(serverResponseAsStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ParserPipelineException("Something went wrong requesting parsing results from grobid server.");
        }
    }

    private static String parseTeiToBib(String tei) {
        //TODO: THIS IS A DUMMY METHOD RIGHT NOW, SHOULD BE IMPLEMENTED
        return "@BOOK{DUMMY:1,\n" +
                "AUTHOR=\"John Doe\",\n" +
                "TITLE=\"The Book without Title\",\n" +
                "PUBLISHER=\"Dummy Publisher\",\n" +
                "YEAR=\"2100\",\n" +
                "}";
    }

    private static BibEntry parseBibToBibEntry(String bibtexString) {
        //TODO: THIS IS A DUMMY METHOD RIGHT NOW, SHOULD BE IMPLEMENTED
        //return BibtexParser.singleFromString(bibtexString, importFormatPreferences, fileMonitor);
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.AUTHOR, "WHATEVER AUTHOR");
        return bibEntry;
    }

}
