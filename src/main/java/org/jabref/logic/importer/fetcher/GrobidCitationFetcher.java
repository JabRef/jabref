package org.jabref.logic.importer.fetcher;

import org.jabref.Globals;
import org.jabref.logic.importer.*;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.importer.util.GrobidServiceException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class GrobidCitationFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcher.class);
    private ImportFormatPreferences importFormatPreferences;
    private FileUpdateMonitor fileUpdateMonitor;
    private GrobidService grobidService;
    private static ArrayList<String> failedEntries = new ArrayList<>();

    public GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileUpdateMonitor, JabRefPreferences jabRefPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        grobidService = new GrobidService(jabRefPreferences);
        failedEntries = new ArrayList<>();
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result.
     * Takes a while, since the server has to look up the entry.
     */
    private String parseUsingGrobid(String plainText) throws FetcherException {
        try {
            return grobidService.processCitation(plainText, 1);
        } catch (GrobidServiceException e) {
            throw new FetcherException("The Pipeline failed to get the results from the GROBID client", e);
        }
    }

    private Optional<BibEntry> parseBibToBibEntry(String bibtexString) throws FetcherException {
        try {
            return BibtexParser.singleFromString(bibtexString,
                    importFormatPreferences, fileUpdateMonitor);
        } catch (ParseException e) {
            throw new FetcherException("Jabref failed to extract a BibEntry form bibtexString.", e);
        }
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        TreeSet<String> plainReferences = new TreeSet<>();
        String[] plainReferencesArray = query.split(";;");
        for (int i = 0; i < plainReferencesArray.length; i++) {
            plainReferences.add(plainReferencesArray[i].trim());
        }
        plainReferences.remove("");
        if (plainReferences.size() == 0) {
            throw new FetcherException("Your entered References are empty.");
        } else {
            ArrayList<BibEntry> resultsList = new ArrayList<>();
            for (String reference: plainReferences) {
                parseBibToBibEntry(parseUsingGrobid(reference)).ifPresentOrElse(resultsList::add, () -> failedEntries.add(reference));
            }
            return resultsList;
        }
    }

    @Override
    public String getName() {
        return "GROBID";
    }

    public static ArrayList<String> getFailedEntries() {
        return failedEntries;
    }
}
