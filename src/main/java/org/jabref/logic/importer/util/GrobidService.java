package org.jabref.logic.importer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 * <p>
 * Note: Currently a custom GROBID server is used...
 * https://github.com/NikodemKch/grobid
 * <p>
 * The methods are structured to match the GROBID server api.
 * Each method corresponds to a GROBID service request. Only the ones already used are already implemented.
 */
public class GrobidService {

    public enum ConsolidateCitations {
        NO(0), WITH_METADATA(1), WITH_DOI_ONLY(2);
        private int code;

        ConsolidateCitations(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    private final ImporterPreferences importerPreferences;

    public GrobidService(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
        if (!importerPreferences.isGrobidEnabled()) {
            throw new UnsupportedOperationException("Grobid was used but not enabled.");
        }
    }

    /**
     * Calls the Grobid server for converting the citation into a BibEntry
     *
     * @return A BibEntry for the String
     * @throws IOException if an I/O exception during the call occurred or no BibTeX entry could be determined
     */
    public Optional<BibEntry> processCitation(String rawCitation, ImportFormatPreferences importFormatPreferences, ConsolidateCitations consolidateCitations) throws IOException, ParseException {
        Connection.Response response = Jsoup.connect(importerPreferences.getGrobidURL() + "/api/processCitation")
                .header("Accept", MediaTypes.APPLICATION_BIBTEX)
                .data("citations", rawCitation)
                .data("consolidateCitations", String.valueOf(consolidateCitations.getCode()))
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .timeout(20000)
                .execute();
        String httpResponse = response.body();

        if (httpResponse == null || httpResponse.equals("@misc{-1,\n  author = {}\n}\n") || httpResponse.equals("@misc{-1,\n  author = {" + rawCitation + "}\n}\n")) { // This filters empty BibTeX entries
            throw new IOException("The GROBID server response does not contain anything.");
        }

        return BibtexParser.singleFromString(httpResponse, importFormatPreferences, new DummyFileUpdateMonitor());
    }

    public List<BibEntry> processPDF(Path filePath, ImportFormatPreferences importFormatPreferences) throws IOException, ParseException {
        Connection.Response response = Jsoup.connect(importerPreferences.getGrobidURL() + "/api/processHeaderDocument")
                .header("Accept", MediaTypes.APPLICATION_BIBTEX)
                .data("input", filePath.toString(), Files.newInputStream(filePath))
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .timeout(20000)
                .execute();

        String httpResponse = response.body();

        if (httpResponse == null || httpResponse.equals("@misc{-1,\n  author = {}\n}\n")) { // This filters empty BibTeX entries
            throw new IOException("The GROBID server response does not contain anything.");
        }

        BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
        List<BibEntry> result = parser.parseEntries(httpResponse);
        result.forEach((entry) -> entry.setCitationKey(""));
        return result;
    }
}
