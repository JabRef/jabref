package org.jabref.logic.importer.fetcher;

import java.util.*;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.importer.util.GrobidServiceException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidCitationFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcher.class);
    private ImportFormatPreferences importFormatPreferences;
    private FileUpdateMonitor fileUpdateMonitor;
    private GrobidService grobidService;
    private List<String> failedEntries = new ArrayList<>();

    public GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileUpdateMonitor, GrobidService grobidService) {
      this.importFormatPreferences = importFormatPreferences;
      this.fileUpdateMonitor = fileUpdateMonitor;
      this.grobidService = grobidService;
    }

    public GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileUpdateMonitor, JabRefPreferences jabRefPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        grobidService = new GrobidService(jabRefPreferences);
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result.
     * Takes a while, since the server has to look up the entry.
     */
    private String parseUsingGrobid(String plainText) throws FetcherException {
        try {
            return grobidService.processCitation(plainText, GrobidService.ConsolidateCitations.WITH_METADATA);
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
        TreeSet<String> plainReferences = Arrays.stream( query.split( "[\\r\\n]+" ) )
              .map(String::trim)
              .collect(Collectors.toCollection(TreeSet::new));
        if (plainReferences.size() == 0) {
            throw new FetcherException("Your entered references are empty.");
        } else {
            //ArrayList<BibEntry> resultsList = new ArrayList<>();
          List<BibEntry> resultsList = new ArrayList<>();
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

    public List<String> getFailedEntries() {
        return failedEntries;
    }
}
