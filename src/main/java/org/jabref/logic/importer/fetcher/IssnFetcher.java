package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.integrity.ISSNChecker;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher to generate the BibTex entry from an ISSN.
 * The idea is to use the {@link DOAJFetcher} to do a request for a given ISSN number.
 */

public class IssnFetcher implements EntryBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssnFetcher.class);
    private final JournalInformationFetcher journalInformationFetcher;
    // private final String SEARCH_URL = "https://doaj.org/api/search/journals/";
    private final ISSNChecker issnChecker;

    public IssnFetcher() {
        this.journalInformationFetcher = new JournalInformationFetcher();
        this.issnChecker = new ISSNChecker();
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> checkedId = entry.getField(StandardField.ISSN).flatMap(issnChecker::checkValue);
        if (checkedId.isPresent()) {
            Optional<JournalInformation> journalInformation = journalInformationFetcher.getJournalInformation(checkedId.get(), "");
            return journalInformation.map(journalInfo -> new BibEntry().withField(StandardField.JOURNALTITLE, journalInfo.publisher())).stream().toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "ISSN";
    }
}
