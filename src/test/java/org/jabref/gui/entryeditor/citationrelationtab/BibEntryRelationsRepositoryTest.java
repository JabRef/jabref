package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;

import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.SemanticScholarFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibEntryRelationsRepositoryTest {

    private List<BibEntry> getCitedBy(BibEntry entry) {
        return List.of(createCitingBibEntry(entry));
    }

    private BibEntry createBibEntry(int i) {
        return new BibEntry()
                .withCitationKey("entry" + i)
                .withField(StandardField.DOI, "10.1234/5678" + i);
    }

    private BibEntry createCitingBibEntry(Integer i) {
        return new BibEntry()
                .withCitationKey("citing_entry" + i)
                .withField(StandardField.DOI, "10.2345/6789" + i);
    }

    private BibEntry createCitingBibEntry(BibEntry citedEntry) {
        return createCitingBibEntry(Integer.valueOf(citedEntry.getCitationKey().get().substring(5)));
    }

    @Test
    void getCitations() throws Exception {
        SemanticScholarFetcher semanticScholarFetcher = mock(SemanticScholarFetcher.class);
        when(semanticScholarFetcher.searchCitedBy(any(BibEntry.class))).thenAnswer(invocation -> {
            BibEntry entry = invocation.getArgument(0);
            return getCitedBy(entry);
        });
        BibEntryRelationsCache bibEntryRelationsCache = new BibEntryRelationsCache();

        BibEntryRelationsRepository bibEntryRelationsRepository = new BibEntryRelationsRepository(semanticScholarFetcher, bibEntryRelationsCache);

        for (int i = 0; i < 150; i++) {
            BibEntry entry = createBibEntry(i);
            List<BibEntry> citations = bibEntryRelationsRepository.getCitations(entry);
            assertEquals(getCitedBy(entry), citations);
        }

        for (int i = 0; i < 150; i++) {
            BibEntry entry = createBibEntry(i);
            List<BibEntry> citations = bibEntryRelationsRepository.getCitations(entry);
            assertEquals(getCitedBy(entry), citations);
        }
    }
}
