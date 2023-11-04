package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * Fetcher to generate the BibTex entry from an ISSN.
 * As an ISSN ist just a journal identifier, so we only return journal title and publisher
 * The idea is to use the {@link JournalInformationFetcher} to do a request for a given ISSN.
 */

public class IssnFetcher implements EntryBasedFetcher, IdBasedFetcher {

    private final JournalInformationFetcher journalInformationFetcher;

    public IssnFetcher() {
        this.journalInformationFetcher = new JournalInformationFetcher();
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> issn = entry.getField(StandardField.ISSN);
        if (issn.isPresent()) {
            Optional<JournalInformation> journalInformation = journalInformationFetcher.getJournalInformation(issn.get(), "");
            return journalInformation.map(journalInfo -> journalInformationToBibEntry(journalInfo, issn.get())).stream().toList();
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "ISSN";
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<JournalInformation> journalInformation = journalInformationFetcher.getJournalInformation(identifier, "");
        return journalInformation.map(journalInfo -> journalInformationToBibEntry(journalInfo, identifier));
    }

    private BibEntry journalInformationToBibEntry(JournalInformation journalInfo, String issn) {
        return new BibEntry().withField(StandardField.JOURNALTITLE, journalInfo.title()).withField(StandardField.PUBLISHER, journalInfo.publisher()).withField(StandardField.ISSN, issn);
    }
}
